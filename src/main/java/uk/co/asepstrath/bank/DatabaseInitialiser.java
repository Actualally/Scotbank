package uk.co.asepstrath.bank;

import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitialiser {

    private final DataSource ds;
    private final Logger log;

    public DatabaseInitialiser(DataSource ds, Logger log) {
        this.ds = ds;
        this.log = log;
    }

    public void initialiseSchema() {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Accounts (
                    AccountID VARCHAR(64) NOT NULL,
                    Name VARCHAR(128) NOT NULL,
                    Password VARCHAR(128) NOT NULL DEFAULT '',
                    Balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    PRIMARY KEY (AccountID)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Transactions (
                    TransactionID VARCHAR(64) NOT NULL,
                    InvestorID VARCHAR(64) NOT NULL,
                    TransactionType VARCHAR(20) NOT NULL,
                    Ticker VARCHAR(10) NULL,
                    TotalCashAmount DECIMAL(12,2) NOT NULL,
                    Shares INT NULL,
                    PricePerShare DECIMAL(12,2) NULL,
                    TransactionDate DATE NOT NULL,
                    PRIMARY KEY (TransactionID),
                    FOREIGN KEY (InvestorID) REFERENCES Accounts(AccountID)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Equities (
                    Ticker VARCHAR(10) NOT NULL,
                    Name VARCHAR(128) NOT NULL,
                    Sector VARCHAR(64) NOT NULL,
                    PRIMARY KEY (Ticker)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ETFs (
                    Ticker VARCHAR(10) NOT NULL,
                    Name VARCHAR(128) NOT NULL,
                    Description VARCHAR(512),
                    InceptionDate DATE,
                    PRIMARY KEY (Ticker)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ETFConstituents (
                    ETFTicker VARCHAR(10) NOT NULL,
                    EquityTicker VARCHAR(10) NOT NULL,
                    PRIMARY KEY (ETFTicker, EquityTicker),
                    FOREIGN KEY (ETFTicker) REFERENCES ETFs(Ticker)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Holdings (
                    InvestorID VARCHAR(64) NOT NULL,
                    Ticker VARCHAR(10) NOT NULL,
                    Shares INT NOT NULL DEFAULT 0,
                    TotalCost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    PRIMARY KEY (InvestorID, Ticker),
                    FOREIGN KEY (InvestorID) REFERENCES Accounts(AccountID)
                )
            """);

            log.info("Database schema initialised successfully");

        } catch (SQLException e) {
            log.error("Schema initialisation failed", e);
        }
    }

    public void seedDemoAccount() {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
            MERGE INTO Accounts (AccountID, Name, Password, Balance)
            VALUES ('investor-001', 'Demo Investor', 'password', 1000000.00)
        """);

            log.info("Demo account seeded");

        } catch (SQLException e) {
            log.error("Failed to seed demo account", e);
        }
    }

    public void seedDemoHolding() {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // Seed current holdings
            stmt.executeUpdate("""
            MERGE INTO Holdings (InvestorID, Ticker, Shares, TotalCost) VALUES
            ('investor-001', 'APX',   50, 3500.00),
            ('investor-001', 'QSY',   30, 2400.00),
            ('investor-001', 'CTT',   40, 3200.00),
            ('investor-001', 'HLN',   25, 2000.00),
            ('investor-001', 'TECHX', 20, 1800.00),
            ('investor-001', 'FINX',  15, 1200.00),
            ('investor-001', 'MEDX',  10,  850.00)
        """);

            // Seed BUY transactions matching the holdings cost basis
            stmt.executeUpdate("""
            MERGE INTO Transactions
            (TransactionID, InvestorID, TransactionType, Ticker, TotalCashAmount, Shares, PricePerShare, TransactionDate)
            VALUES
            ('demo-buy-001', 'investor-001', 'BUY', 'APX',   3500.00, 50, 70.00, '2025-01-10'),
            ('demo-buy-002', 'investor-001', 'BUY', 'QSY',   2400.00, 30, 80.00, '2025-01-10'),
            ('demo-buy-003', 'investor-001', 'BUY', 'CTT',   3200.00, 40, 80.00, '2025-01-10'),
            ('demo-buy-004', 'investor-001', 'BUY', 'HLN',   2000.00, 25, 80.00, '2025-01-10'),
            ('demo-buy-005', 'investor-001', 'BUY', 'TECHX', 1800.00, 20, 90.00, '2025-01-10'),
            ('demo-buy-006', 'investor-001', 'BUY', 'FINX',  1200.00, 15, 80.00, '2025-01-10'),
            ('demo-buy-007', 'investor-001', 'BUY', 'MEDX',   850.00, 10, 85.00, '2025-01-10')
        """);

            // Seed a SELL transaction at profit to show capital gains
            stmt.executeUpdate("""
            MERGE INTO Transactions
            (TransactionID, InvestorID, TransactionType, Ticker, TotalCashAmount, Shares, PricePerShare, TransactionDate)
            VALUES
            ('demo-sell-001', 'investor-001', 'SELL', 'APX', 5500.00, 50, 110.00, '2025-06-15'),
            ('demo-sell-002', 'investor-001', 'SELL', 'QSY', 6000.00, 30, 200.00, '2025-06-15')
        """);

            log.info("Demo holdings and transactions seeded");
        } catch (SQLException e) {
            log.error("Failed to seed demo holdings", e);
        }
    }
}
