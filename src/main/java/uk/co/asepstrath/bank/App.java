package uk.co.asepstrath.bank;

import io.jooby.Cookie;
import io.jooby.ServerOptions;
import io.jooby.SessionStore;
import io.jooby.netty.NettyServer;
import io.jooby.Jooby;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.helper.UniRestExtension;
import io.jooby.hikari.HikariModule;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.controllers.AccountController_;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class App extends Jooby {

    {
        install(new UniRestExtension());
        install(new HandlebarsModule());
        install(new HikariModule("mem"));

        setSessionStore(SessionStore.memory(Cookie.session("scotbank")));

        assets("/assets/*", "/assets");
        assets("/service_worker.js","/service_worker.js");

        DataSource ds = require(DataSource.class);
        Logger log = getLog();

        mvc(new AccountController_(ds, log));

        onStarted(() -> onStart());
        onStop(() -> onStop());
    }

    public static void main(final String[] args) {
        runApp(args, new NettyServer(new ServerOptions()), App::new);
    }

    public void onStart() {
        Logger log = getLog();
        log.info("Starting Up...");

        DataSource ds = require(DataSource.class);
        try (Connection connection = ds.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Accounts (
                    AccountID VARCHAR(64) NOT NULL,
                    Name VARCHAR(128) NOT NULL,
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
                MERGE INTO Accounts (AccountID, Name, Balance)
                VALUES ('investor-001', 'Demo Investor', 1000.00)
            """);

            log.info("Database tables created and seeded successfully");
        } catch (SQLException e) {
            log.error("Database Creation Error", e);
        }
    }

    public void onStop() {
        Logger log = getLog();
        log.info("Shutting Down...");
    }
}