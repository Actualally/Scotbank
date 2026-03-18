package uk.co.asepstrath.bank.services;

import org.slf4j.Logger;
import uk.co.asepstrath.bank.models.Transaction;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class TransactionProcessor {

    private final Logger log;
    private final ApiService apiService;

    public TransactionProcessor(Logger log, ApiService apiService) {
        this.log = log;
        this.apiService = apiService;
    }

    public void applyTransaction(Transaction t,
                                 PreparedStatement holdingGet,
                                 PreparedStatement holdingMerge) throws SQLException {
        String investorId = t.getInvestorId().toString();
        double amount = t.getTotalCashAmount();

        switch (t.getType()) {
            case "BUY":
                updateHolding(holdingGet, holdingMerge, investorId,
                        t.getTicker(), amount, true, t.getDate());
                log.info("Transaction Processed");
                break;
            case "SELL":
                updateHolding(holdingGet, holdingMerge, investorId,
                        t.getTicker(), amount, false, t.getDate());
                log.info("Transaction Processed");
                break;
            case "DEPOSIT", "WITHDRAW":
            default:
                break;
        }
    }

    private void updateHolding(PreparedStatement getStmt, PreparedStatement mergeStmt,
                               String investorId, String ticker, double amount,
                               boolean isBuy, LocalDate date) throws SQLException {
        double priceOnDate = apiService.fetchPriceOnDate(ticker, date);
        int sharesDelta = priceOnDate > 0 ? (int) Math.round(amount / priceOnDate) : 0;

        getStmt.setString(1, investorId);
        getStmt.setString(2, ticker);

        int currentShares = 0;
        double currentCost = 0.0;

        try (ResultSet rs = getStmt.executeQuery()) {
            if (rs.next()) {
                currentShares = rs.getInt("Shares");
                currentCost = rs.getDouble("TotalCost");
            }
        }

        if (isBuy) {
            currentShares += sharesDelta;
            currentCost += amount;
        } else {
            // use avg book price to calculate shares sold, not current market price
            double avgBookPrice = currentShares > 0 ? currentCost / currentShares : 0.0;
            int sharesToRemove = avgBookPrice > 0 ? (int) Math.round(amount / avgBookPrice) : sharesDelta;
            currentShares = Math.max(0, currentShares - sharesToRemove);
            currentCost = Math.max(0.0, currentCost - amount);
        }

        mergeStmt.setString(1, investorId);
        mergeStmt.setString(2, ticker);
        mergeStmt.setInt(3, currentShares);
        mergeStmt.setBigDecimal(4, BigDecimal.valueOf(currentCost));
        mergeStmt.executeUpdate();
    }
}
