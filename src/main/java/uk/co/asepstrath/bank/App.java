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
        /*
        This section is used for setting up the Jooby Framework modules
         */
        install(new UniRestExtension());
        install(new HandlebarsModule());
        install(new HikariModule("mem"));

        setSessionStore(SessionStore.memory(Cookie.session("scotbank")));

        /*
        This will host any files in src/main/resources/assets on <host>/assets
        For example in the dice template (dice.hbs) it references "assets/dice.png" which is in resources/assets folder
         */
        assets("/assets/*", "/assets");
        assets("/service_worker.js","/service_worker.js");

        /*
        Now we set up our controllers and their dependencies
         */
        DataSource ds = require(DataSource.class);
        Logger log = getLog();

        mvc(new AccountController_(ds,log));

        /*
        Finally we register our application lifecycle methods
         */
        onStarted(() -> onStart());
        onStop(() -> onStop());
    }

    public static void main(final String[] args) {
        runApp(args, new NettyServer(new ServerOptions()), App::new);
    }

    /*
    This function will be called when the application starts up,
    it should be used to ensure that the DB is properly setup
     */
    public void onStart() {
        Logger log = getLog();
        log.info("Starting Up...");

        DataSource ds = require(DataSource.class);
        try (Connection connection = ds.getConnection()) {
            Statement stmt = connection.createStatement();

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

    /*
    This function will be called when the application shuts down
     */
    public void onStop() {
        System.out.println("Shutting Down...");
    }

}
