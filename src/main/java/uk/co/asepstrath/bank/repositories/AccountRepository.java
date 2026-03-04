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

public class AccountRepository {

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
                return new Account(accountId, rs.getString("Name"), rs.getBigDecimal("Balance"));
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
                    return rs.getBigDecimal("Balance");
                }
                return null;
            }
        }
    }

    public void updateBalance(Connection conn, Account account) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Accounts SET Balance = ? WHERE AccountID = ?")) {
            stmt.setBigDecimal(1, account.getBalance());
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
}