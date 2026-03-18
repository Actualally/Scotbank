package uk.co.asepstrath.bank.services;

import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.repositories.AccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * AccountService - BUSINESS LOGIC LAYER
 * This class sits between the controller and the repository. It handles validation,
 * business rules (e.g. checking balances, parsing amounts), and coordinates database
 * transactions. The controller calls this class, and it never touches
 * the database directly. If you need to add new business rules or processing steps
 * (e.g. for the upcoming stock trading features), add them here.
 */

public class AccountService {

    private final AccountRepository accountRepository;
    private final Logger logger;

    public AccountService(AccountRepository accountRepository, Logger logger) {
        this.accountRepository = accountRepository;
        this.logger = logger;
    }

    public Account getAccountDetails(String accountId) throws SQLException {
        try (Connection conn = accountRepository.getConnection()) {
            return accountRepository.findById(conn, accountId);
        }
    }

    public BigDecimal getBalance(String accountId) throws SQLException {
        return accountRepository.getBalance(accountId);
    }

    public List<Map<String, String>> getTransactionHistory(String accountId) throws SQLException {
        return accountRepository.getTransactionHistory(accountId);
    }

    public List<Map<String, Object>> getEnrichedHoldings(String accountId) throws SQLException {
        List<Map<String, Object>> holdings = accountRepository.getHoldings(accountId);
        ApiService apiService = new ApiService(logger);

        for (Map<String, Object> holding : holdings) {
            String ticker = (String) holding.get("ticker");
            int shares = (int) holding.get("shares");
            double totalCost = ((java.math.BigDecimal) holding.get("totalCost")).doubleValue();

            double currentPrice = apiService.fetchLatestPrice(ticker);
            double currentValue = shares * currentPrice;
            double gainLoss = currentValue - totalCost;
            double gainLossPct = totalCost > 0 ? (gainLoss / totalCost) * 100 : 0.0;

            holding.put("currentPrice", String.format("%.2f", currentPrice));
            holding.put("currentValue", String.format("%.2f", currentValue));
            holding.put("gainLoss", String.format("%.2f", gainLoss));
            holding.put("gainLossPct", String.format("%.2f", gainLossPct));
            holding.put("gainLossPositive", gainLoss >= 0);
        }
        return holdings;
    }

    public Map<String, String> getPortfolioSummary(List<Map<String, Object>> enrichedHoldings) {
        double totalCurrentValue = 0.0;
        double totalCost = 0.0;

        for (Map<String, Object> h : enrichedHoldings) {
            totalCurrentValue += Double.parseDouble((String) h.get("currentValue"));
            totalCost += ((java.math.BigDecimal) h.get("totalCost")).doubleValue();
        }

        double totalGainLoss = totalCurrentValue - totalCost;
        double totalGainLossPct = totalCost > 0 ? (totalGainLoss / totalCost) * 100 : 0.0;

        Map<String, String> summary = new HashMap<>();
        summary.put("totalCurrentValue", String.format("%,.2f", totalCurrentValue));
        summary.put("totalGainLoss", String.format("%,.2f", totalGainLoss));
        summary.put("totalGainLossPct", String.format("%.2f", totalGainLossPct));
        summary.put("totalGainLossPositive", String.valueOf(totalGainLoss >= 0));
        return summary;
    }

    public void deposit(String accountId, BigDecimal amount) throws SQLException {
        try (Connection conn = accountRepository.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = accountRepository.findById(conn, accountId);
                account.deposit(amount);
                accountRepository.updateBalance(conn, account);
                accountRepository.recordTransaction(conn, accountId, "DEPOSIT", amount);
                conn.commit();
                logger.info("Deposit of £{} successful for {}", amount, accountId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void withdraw(String accountId, BigDecimal amount) throws SQLException {
        try (Connection conn = accountRepository.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = accountRepository.findById(conn, accountId);
                account.withdraw(amount);
                accountRepository.updateBalance(conn, account);
                accountRepository.recordTransaction(conn, accountId, "WITHDRAWAL", amount);
                conn.commit();
                logger.info("Withdrawal of £{} successful for {}", amount, accountId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public BigDecimal parseAndValidateAmount(String raw) throws ArithmeticException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ArithmeticException("Please enter an amount");
        }
        try {
            BigDecimal amount = new BigDecimal(raw.trim());
            return amount.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new ArithmeticException("Invalid amount — please enter a valid number");
        }
    }

    public Map<String, Object> getInvestorDetails(String accountId) throws SQLException {
        return accountRepository.getInvestorById(accountId);
    }

    public List<Map<String, Object>> getHoldings(String accountId) throws SQLException {
        return accountRepository.getHoldings(accountId);
    }

    public double getTotalPortfolioValue(String accountId) throws SQLException {
        return accountRepository.getTotalPortfolioValue(accountId);
    }
}
