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
            VALUES ('investor-001', 'Demo Investor', 'password', 1000.00)
        """);

            log.info("Demo account seeded");

        } catch (SQLException e) {
            log.error("Failed to seed demo account", e);
        }
    }

    public void seedDemoHolding() {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
            MERGE INTO Holdings (InvestorID, Ticker, Shares, TotalCost)
            VALUES ('investor-001', 'MRH', 10, 1000.00)
        """);
            log.info("Demo holding seeded");
        } catch (SQLException e) {
            log.error("Failed to seed demo holding", e);
        }
    }
}
