package uk.co.asepstrath.bank.controllers;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.co.asepstrath.bank.Constants.*;

@Path(ROUTE_ACCOUNT)
public class AccountController {

    private final DataSource dataSource;
    private final Logger logger;
    private static final String DB_NAME = "name";
    private static final String DB_BALANCE = "balance";
    private static final String DB_ID = "accountId";


    public AccountController(DataSource ds, Logger log) {
        this.dataSource = ds;
        this.logger = log;
    }

    private String getAccountID(Context ctx){
        var session = ctx.sessionOrNull();
        if (session == null || !session.get(SESSION_ACCOUNT_ID).isPresent()) {
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_LOGIN);
            return null;
        }
        return session.get(SESSION_ACCOUNT_ID).value();
    }

    @GET
    public ModelAndView<Map<String, Object>> viewAccount(Context ctx) {
        String accountID = getAccountID(ctx);
        Map<String, Object> model = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT Name, Balance FROM Accounts WHERE AccountID = ?")) {
            stmt.setString(1, accountID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    model.put(DB_NAME, rs.getString("Name"));
                    model.put(DB_BALANCE, rs.getBigDecimal(DB_BALANCE).toPlainString());
                    model.put(DB_ID, accountID);
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading account", e);
            model.put(SESSION_ERROR_MESSAGE, "Could not load account data");
        }
        transferFlashMessages(ctx, model);
        return new ModelAndView<>("account.hbs", model);
    }

    // Show deposit form with current balance
    @GET(ROUTE_DEPOSIT)
    public ModelAndView<Map<String, Object>> showDepositForm(Context ctx) {
        String accountID = getAccountID(ctx);
        Map<String, Object> model = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT Balance FROM Accounts WHERE AccountID = ?")) {
            stmt.setString(1, accountID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    model.put(DB_BALANCE, rs.getBigDecimal(DB_BALANCE).toPlainString());
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading balance for deposit form", e);
        }
        transferFlashMessages(ctx, model);
        return new ModelAndView<>(TEMPLATE_DEPOSIT, model);
    }

    // Show withdrawal form with current balance
    @GET(ROUTE_WITHDRAW)
    public ModelAndView<Map<String,Object>> showWithdrawalForm(Context ctx) {
        String accountID = getAccountID(ctx);
        Map<String,Object> model = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT Balance FROM Accounts WHERE AccountID = ?")) {
            stmt.setString(1, accountID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    model.put(DB_BALANCE, rs.getBigDecimal(DB_BALANCE).toPlainString());
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading balance for withdrawal form", e);
        }
        transferFlashMessages(ctx, model);
        return new ModelAndView<>(TEMPLATE_WITHDRAW, model);
    }

    @POST(ROUTE_WITHDRAW + ROUTE_PROCESS)
    public void processWithdrawal(Context ctx){
        String accountID = getAccountID(ctx);
        String amountstr = ctx.form("withdrawamount").valueOrNull();
        logger.info("Withdrawal requested - raw input: '{}'", amountstr);

        try {
            BigDecimal amount = parseAndValidateAmount(amountstr);
            performWithdrawal(accountID, amount);
            ctx.session().put(SESSION_SUCCESS_MESSAGE, 
                "Successfully withdrawn £" + amount.toPlainString()
            );

            logger.info("Withdrawal of £{} successful for {}", amount, accountID);
            ctx.sendRedirect(ROUTE_ACCOUNT);

        }catch (ArithmeticException e){
            logger.info("Withdrawal failed: {}", e.getMessage());
            ctx.session().put(SESSION_ERROR_MESSAGE, e.getMessage());
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_WITHDRAW);
        }catch (SQLException e) {
            logger.error("Database error during withdrawal", e);
            ctx.session().put(SESSION_ERROR_MESSAGE, "A system error occurred. Please try again.");
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_WITHDRAW);
        }
    }

    // Handle deposit submission
    @POST(ROUTE_DEPOSIT + ROUTE_PROCESS)
    public void processDeposit(Context ctx) {
        String accountID = getAccountID(ctx);
        String amountStr = ctx.form("depositamount").valueOrNull();
        logger.info("Deposit requested — raw input: '{}'", amountStr);

        try {
            BigDecimal amount = parseAndValidateAmount(amountStr);
            performDeposit(accountID, amount);
            ctx.session().put(SESSION_SUCCESS_MESSAGE,
                    "Successfully deposited £" + amount.toPlainString());
            logger.info("Deposit of £{} successful for {}", amount, accountID);
            ctx.sendRedirect(ROUTE_ACCOUNT);

        } catch (ArithmeticException e) {
            logger.info("Deposit failed: {}", e.getMessage());
            ctx.session().put(SESSION_ERROR_MESSAGE, e.getMessage());
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_DEPOSIT);
        } catch (SQLException e) {
            logger.error("Database error during deposit", e);
            ctx.session().put(SESSION_ERROR_MESSAGE, "A system error occurred. Please try again.");
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_DEPOSIT);
        }
    }

    // Perform deposit transaction in database
    private void performDeposit(String accountID, BigDecimal amount) throws SQLException {
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = loadAccount(conn, accountID);
                account.deposit(amount);
                updateBalance(conn, account);
                recordTransaction(conn, accountID, "DEPOSIT", amount);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void performWithdrawal(String accountID, BigDecimal amount) throws SQLException{
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = loadAccount(conn, accountID);
                account.withdraw(amount);
                updateBalance(conn, account);
                recordTransaction(conn, accountID, "WITHDRAWAL", amount);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Validate and parse transaction amount
    private BigDecimal parseAndValidateAmount(String raw) throws ArithmeticException {
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

    // Load account from DB
    private Account loadAccount(Connection conn, String accountId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT Name, Balance FROM Accounts WHERE AccountID = ?")) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Account not found: " + accountId);
                }
                return new Account(accountId, rs.getString("Name"), rs.getBigDecimal(DB_BALANCE));
            }
        }
    }

    // Update account balance in DB
    private void updateBalance(Connection conn, Account account) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Accounts SET Balance = ? WHERE AccountID = ?")) {
            stmt.setBigDecimal(1, account.getBalance());
            stmt.setString(2, account.getAccountId());
            stmt.executeUpdate();
        }
    }

    // Record a transaction in DB
    private void recordTransaction(Connection conn, String investorId,
                                   String type, BigDecimal amount) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Transactions " +
                        "(TransactionID, InvestorID, TransactionType, Ticker, TotalCashAmount, TransactionDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, investorId);
            stmt.setString(3, type);
            stmt.setNull(4, Types.VARCHAR);
            stmt.setBigDecimal(5, amount);
            stmt.setDate(6, Date.valueOf(LocalDate.now()));
            stmt.executeUpdate();
        }
    }

    // Move session flash messages into the model
    private void transferFlashMessages(Context ctx, Map<String, Object> model) {
        var session = ctx.sessionOrNull();
        if (session == null) return;
        for (String key : new String[]{SESSION_SUCCESS_MESSAGE, SESSION_ERROR_MESSAGE}) {
            if (session.get(key).isPresent()) {
                model.put(key, session.get(key).value());
                session.remove(key);
            }
        }
    }
}