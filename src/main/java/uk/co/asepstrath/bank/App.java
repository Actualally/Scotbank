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
import uk.co.asepstrath.bank.controllers.LoginController_;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class App extends Jooby {

    public App(){

        // Install extensions and modules
        install(new UniRestExtension());
        install(new HandlebarsModule());
        install(new HikariModule("mem"));

        // Set up in-memory session store with named cookie
        setSessionStore(SessionStore.memory(Cookie.session("scotbank")));

        // Serve static assets
        assets("/assets/*", "/assets");
        assets("/service_worker.js","/service_worker.js");

        // Obtain shared DataSource and logger
        DataSource ds = require(DataSource.class);
        Logger log = getLog();

        // Register controller(s) for MVC routes
        mvc(new AccountController_(ds, log));
        mvc(new LoginController_(ds, log));

        // Lifecycle hooks
        onStarted(this::onStart); // after the server starts
        onStop(this::onStop); // before the server stops
    }

    public static void main(final String[] args) {
        runApp(args, new NettyServer(new ServerOptions()), App::new);
    }

    // Initialize database tables and seed demo account
    public void onStart() {
        Logger log = getLog();
        log.info("Starting Up...");

        DataSource ds = require(DataSource.class);
        try (Connection connection = ds.getConnection();
             Statement stmt = connection.createStatement()) {

            //if the Accounts table does not exist already create one
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Accounts (
                    AccountID VARCHAR(64) NOT NULL,
                    Name VARCHAR(128) NOT NULL,
                    Balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    Password VARCHAR(128) NOT NULL DEFAULT '',
                    PRIMARY KEY (AccountID)
                )
            """);

            //if the Transactions table does not exist also create one
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

            //this is an example account for testing purposes
            stmt.executeUpdate("""
                MERGE INTO Accounts (AccountID, Name, Balance, Password)
                VALUES ('investor-001', 'Demo Investor', 1000.00, 'testpassword')
            """);

            log.info("Database tables created and seeded successfully");
        } catch (SQLException e) {
            log.error("Database Creation Error", e);
        }
    }

    //Logs that the server is shutting down
    public void onStop() {
        Logger log = getLog();
        log.info("Shutting Down...");
    }
}