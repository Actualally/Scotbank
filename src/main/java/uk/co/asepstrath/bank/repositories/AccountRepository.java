package uk.co.asepstrath.bank.repositories;

import uk.co.asepstrath.bank.Account;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.sql.Date;

public class AccountRepository {

    private static final String DB_BALANCE = "balance";
    private static final String COL_TOTAL_CASH_AMOUNT = "TotalCashAmount";
    private static final String COL_TRANSACTION_DATE = "TransactionDate";
    private static final String COL_TICKER = "Ticker";
    private static final String COL_SHARES = "Shares";
    private static final String COL_PRICE_PER_SHARE = "PricePerShare";
    private static final String COL_TOTAL_COST = "TotalCost";
    private static final String KEY_TICKER = "ticker";
    private static final String KEY_SHARES = "shares";
    private static final String STRING_FORMATTER = "%,.2f";
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
                if (rs.next()) return rs.getBigDecimal(DB_BALANCE);
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
             PreparedStatement stmt = conn.prepareStatement("""
                 SELECT TransactionType, Ticker, TotalCashAmount, Shares, PricePerShare, TransactionDate
                 FROM Transactions WHERE InvestorID = ?
                 ORDER BY TransactionDate DESC
                 """)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("type", rs.getString("TransactionType"));
                    row.put("amount", String.format(STRING_FORMATTER, rs.getBigDecimal(COL_TOTAL_CASH_AMOUNT)));
                    row.put("date", rs.getDate(COL_TRANSACTION_DATE).toString());
                    row.put("typeLower", rs.getString("TransactionType").toLowerCase());
                    row.put(KEY_TICKER, rs.getString(COL_TICKER));
                    row.put(KEY_SHARES, rs.getInt(COL_SHARES) > 0 ? String.valueOf(rs.getInt(COL_SHARES)) : "—");
                    row.put("pricePerShare", rs.getBigDecimal(COL_PRICE_PER_SHARE) != null &&
                            rs.getBigDecimal(COL_PRICE_PER_SHARE).compareTo(BigDecimal.ZERO) > 0
                            ? String.format(STRING_FORMATTER, rs.getBigDecimal(COL_PRICE_PER_SHARE)) : "—");
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
             PreparedStatement stmt = conn.prepareStatement("""
                     SELECT h.Ticker, h.Shares, h.TotalCost,
                            COALESCE(e.Name, etf.Name, 'Unknown') AS Name,
                            COALESCE(e.Sector, 'ETF') AS Sector
                     FROM Holdings h
                     LEFT JOIN Equities e ON h.Ticker = e.Ticker
                     LEFT JOIN ETFs etf ON h.Ticker = etf.Ticker
                     WHERE h.InvestorID = ? AND h.Shares > 0
                     ORDER BY h.TotalCost DESC
                     """)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> holding = new HashMap<>();
                    holding.put(KEY_TICKER, rs.getString(COL_TICKER));
                    holding.put(KEY_SHARES, rs.getInt(COL_SHARES));
                    holding.put("totalCost", rs.getBigDecimal(COL_TOTAL_COST));
                    holding.put("name", rs.getString("Name") != null ? rs.getString("Name") : "Unknown");
                    holding.put("sector", rs.getString("Sector") != null ? rs.getString("Sector") : "Unknown");

                    int shares = rs.getInt(COL_SHARES);
                    double totalCost = rs.getDouble(COL_TOTAL_COST);
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
                if (rs.next()) return rs.getDouble("TotalValue");
                return 0.0;
            }
        }
    }

    public List<String> getETFConstituents(String etfTicker) throws SQLException {
        List<String> constituents = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT EquityTicker FROM ETFConstituents WHERE ETFTicker = ?")) {
            stmt.setString(1, etfTicker);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    constituents.add(rs.getString("EquityTicker"));
                }
            }
        }
        return constituents;
    }

    public boolean isETF(String ticker) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM ETFs WHERE Ticker = ?")) {
            stmt.setString(1, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    //no year transactions
    public List<Map<String, Object>> getSellTransactions(String accountId) throws SQLException {
        List<Map<String, Object>> sells = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 SELECT TransactionID, Ticker, TotalCashAmount, TransactionDate
                 FROM Transactions
                 WHERE InvestorID = ?
                 AND TransactionType = 'SELL'
                 ORDER BY TransactionDate ASC
                 """)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sell = new HashMap<>();
                    sell.put("transactionId", rs.getString("TransactionID"));
                    sell.put(KEY_TICKER, rs.getString(COL_TICKER));
                    sell.put("proceeds", rs.getBigDecimal(COL_TOTAL_CASH_AMOUNT));
                    sell.put("date", rs.getDate(COL_TRANSACTION_DATE).toString());
                    sells.add(sell);
                }
            }
        }
        return sells;
    }

    public List<Map<String, Object>> getSellTransactions(String accountId, int year) throws SQLException {
        List<Map<String, Object>> sells = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 SELECT TransactionID, Ticker, TotalCashAmount, TransactionDate
                 FROM Transactions
                 WHERE InvestorID = ?
                 AND TransactionType = 'SELL'
                 AND YEAR(TransactionDate) = ?
                 ORDER BY TransactionDate ASC
                 """)) {
            stmt.setString(1, accountId);
            stmt.setInt(2, year);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sell = new HashMap<>();
                    sell.put("transactionId", rs.getString("TransactionID"));
                    sell.put(KEY_TICKER, rs.getString(COL_TICKER));
                    sell.put("proceeds", rs.getBigDecimal(COL_TOTAL_CASH_AMOUNT));
                    sell.put("date", rs.getDate(COL_TRANSACTION_DATE).toString());
                    sells.add(sell);
                }
            }
        }
        return sells;
    }

    public Map<String, Object> getHoldingForTicker(String accountId, String ticker) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT Shares, TotalCost FROM Holdings WHERE InvestorID = ? AND Ticker = ?")) {
            stmt.setString(1, accountId);
            stmt.setString(2, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> holding = new HashMap<>();
                    holding.put(KEY_SHARES, rs.getInt(COL_SHARES));
                    holding.put("totalCost", rs.getBigDecimal(COL_TOTAL_COST));
                    return holding;
                }
            }
        }
        return new HashMap<>();
    }

    public List<Map<String, Object>> getBuyTransactions(String accountId) throws SQLException {
        List<Map<String, Object>> buys = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 SELECT Ticker, TotalCashAmount
                 FROM Transactions
                 WHERE InvestorID = ?
                 AND TransactionType = 'BUY'
                 ORDER BY TransactionDate ASC
                 """)) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> buy = new HashMap<>();
                    buy.put(KEY_TICKER, rs.getString(COL_TICKER));
                    buy.put("amount", rs.getBigDecimal(COL_TOTAL_CASH_AMOUNT));
                    buys.add(buy);
                }
            }
        }
        return buys;
    }
}