package uk.co.asepstrath.bank.repositories;

import uk.co.asepstrath.bank.Account;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

/**
 * AccountRepository - DATA ACCESS LAYER
 * This class is the only place that talks directly to the database for account-related operations.
 * All SQL queries (selects, updates, inserts) for accounts and transactions live here.
 * If you need to change how data is stored or retrieved, this is where to do it.
 */

public class AccountRepository {

    private static final String DB_BALANCE = "balance";
    private final DataSource dataSource;

    public AccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public Account findById(Connection conn, String accountId) throws SQLException {
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

    public BigDecimal getBalance(String accountId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT Balance FROM Accounts WHERE AccountID = ?")) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(DB_BALANCE);
                }
                return null;
            }
        }
    }

    public void updateBalance(Connection conn, Account account) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Accounts SET Balance = ? WHERE AccountID = ?")) {
            stmt.setBigDecimal(1, account.getBalanceAsBigDecimal());
            stmt.setString(2, account.getAccountId());
            stmt.executeUpdate();
        }
    }

    public void recordTransaction(Connection conn, String investorId,
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

    public List<Map<String, String>> getTransactionHistory(String accountId) throws SQLException {
        List<Map<String, String>> transactions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT TransactionType, TotalCashAmount, TransactionDate " +
                             "FROM Transactions WHERE InvestorID = ? " +
                             "ORDER BY TransactionDate DESC")) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("type", rs.getString("TransactionType"));
                    row.put("amount", String.format("%,.2f", rs.getBigDecimal("TotalCashAmount")));
                    row.put("date", rs.getDate("TransactionDate").toString());
                    transactions.add(row);
                }
            }
        }
        return transactions;
    }

    public Map<String, Object> getInvestorById(String accountId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT AccountID, Name, Balance FROM Accounts WHERE AccountID = ?")) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> investor = new HashMap<>();
                    investor.put("accountId", rs.getString("AccountID"));
                    investor.put("name", rs.getString("Name"));
                    investor.put(DB_BALANCE, rs.getBigDecimal(DB_BALANCE));
                    return investor;
                }
                return new HashMap<>();
            }
        }
    }


    public List<Map<String, Object>> getHoldings(String accountId) throws SQLException {
        List<Map<String, Object>> holdings = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT h.Ticker, h.Shares, h.TotalCost, " +
                             "COALESCE(e.Name, etf.Name, 'Unknown') AS Name, " +
                             "COALESCE(e.Sector, 'ETF') AS Sector " +
                             "FROM Holdings h " +
                             "LEFT JOIN Equities e ON h.Ticker = e.Ticker " +
                             "LEFT JOIN ETFs etf ON h.Ticker = etf.Ticker " +
                             "WHERE h.InvestorID = ? AND h.Shares > 0 " +
                             "ORDER BY h.TotalCost DESC")) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> holding = new HashMap<>();
                    holding.put("ticker", rs.getString("Ticker"));
                    holding.put("shares", rs.getInt("Shares"));
                    holding.put("totalCost", rs.getBigDecimal("TotalCost"));
                    holding.put("name", rs.getString("Name") != null ? rs.getString("Name") : "Unknown");
                    holding.put("sector", rs.getString("Sector") != null ? rs.getString("Sector") : "Unknown");

                    int shares = rs.getInt("Shares");
                    double totalCost = rs.getDouble("TotalCost");
                    holding.put("avgPrice", shares > 0 ? String.format("%.2f", totalCost / shares) : "0.00");

                    holdings.add(holding);
                }
            }
        }
        return holdings;
    }


    public double getTotalPortfolioValue(String accountId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COALESCE(SUM(TotalCost), 0) AS TotalValue FROM Holdings WHERE InvestorID = ?")) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("TotalValue");
                }
                return 0.0;
            }
        }
    }
}