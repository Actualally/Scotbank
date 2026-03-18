package uk.co.asepstrath.bank.services;

import org.slf4j.Logger;
import uk.co.asepstrath.bank.models.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class DataSyncService {

    private final DataSource ds;
    private final Logger log;
    private final ApiService apiService;
    private final TransactionProcessor transactionProcessor;

    public DataSyncService(DataSource ds, Logger log) {
        this.ds = ds;
        this.log = log;
        this.apiService = new ApiService(log);
        this.transactionProcessor = new TransactionProcessor(log, apiService);
    }

    public void syncAll() {
        try (Connection conn = ds.getConnection()) {
            syncInvestors(conn);
            syncEquities(conn);
            syncETFs(conn);
            syncTransactions(conn);
        } catch (SQLException e) {
            log.error("Data sync failed", e);
        }
    }

    private void syncInvestors(Connection conn) throws SQLException {
        List<Investor> investors = apiService.fetchInvestors();
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO Accounts (AccountID, Name, Password, Balance) VALUES (?, ?, 'password', ?)")) {
            for (Investor inv : investors) {
                stmt.setString(1, inv.getId().toString());
                stmt.setString(2, inv.getName());
                stmt.setBigDecimal(3, BigDecimal.valueOf(inv.getCashBalance()));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        log.info("Synced {} investors", investors.size());
    }

    private void syncEquities(Connection conn) throws SQLException {
        List<Equity> equities = apiService.fetchEquities();
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO Equities (Ticker, Name, Sector) VALUES (?, ?, ?)")) {
            for (Equity eq : equities) {
                stmt.setString(1, eq.getTicker());
                stmt.setString(2, eq.getName());
                stmt.setString(3, eq.getSector());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        log.info("Synced {} equities", equities.size());
    }

    private void syncETFs(Connection conn) throws SQLException {
        List<ETF> etfs = apiService.fetchETFs();
        try (PreparedStatement etfStmt = conn.prepareStatement(
                "MERGE INTO ETFs (Ticker, Name, Description, InceptionDate) VALUES (?, ?, ?, ?)");
             PreparedStatement constStmt = conn.prepareStatement(
                     "MERGE INTO ETFConstituents (ETFTicker, EquityTicker) VALUES (?, ?)")) {

            for (ETF etf : etfs) {
                etfStmt.setString(1, etf.getTicker());
                etfStmt.setString(2, etf.getName());
                etfStmt.setString(3, etf.getDescription());
                etfStmt.setDate(4, etf.getInceptionDate() != null
                        ? Date.valueOf(etf.getInceptionDate()) : null);
                etfStmt.addBatch();

                if (etf.getConstituents() != null) {
                    for (String constituent : etf.getConstituents()) {
                        constStmt.setString(1, etf.getTicker());
                        constStmt.setString(2, constituent);
                        constStmt.addBatch();
                    }
                }
            }
            etfStmt.executeBatch();
            constStmt.executeBatch();
        }
        log.info("Synced {} ETFs", etfs.size());
    }

    private void syncTransactions(Connection conn) throws SQLException {

        try (Statement clear = conn.createStatement()) {
            clear.executeUpdate("DELETE FROM Holdings");
        }

        List<Transaction> transactions = apiService.fetchAllTransactions();
        transactions.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        try (PreparedStatement txStmt = conn.prepareStatement(
                "MERGE INTO Transactions (TransactionID, InvestorID, TransactionType, Ticker, TotalCashAmount, TransactionDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
             PreparedStatement balanceStmt = conn.prepareStatement(
                     "UPDATE Accounts SET Balance = Balance + ? WHERE AccountID = ?");
             PreparedStatement holdingMerge = conn.prepareStatement(
                     "MERGE INTO Holdings (InvestorID, Ticker, Shares, TotalCost) KEY (InvestorID, Ticker) VALUES (?, ?, ?, ?)");
             PreparedStatement holdingGet = conn.prepareStatement(
                     "SELECT Shares, TotalCost FROM Holdings WHERE InvestorID = ? AND Ticker = ?")) {

            prewarmPriceCache(transactions);
            for (Transaction t : transactions) {
                txStmt.setString(1, t.getId().toString());
                txStmt.setString(2, t.getInvestorId().toString());
                txStmt.setString(3, t.getType());
                txStmt.setString(4, t.getTicker());
                txStmt.setBigDecimal(5, BigDecimal.valueOf(t.getTotalCashAmount()));
                txStmt.setDate(6, Date.valueOf(t.getDate()));
                txStmt.addBatch();

                transactionProcessor.applyTransaction(
                        t, holdingGet, holdingMerge);
            }
            txStmt.executeBatch();
        }
        log.info("Synced {} transactions", transactions.size());
    }

    private void prewarmPriceCache(List<Transaction> transactions) {
        transactions.stream()
                .map(Transaction::getTicker)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .forEach(apiService::fetchPrices);
        log.info("Price cache pre-warmed");
    }
}
