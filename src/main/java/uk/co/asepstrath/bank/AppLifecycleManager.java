package uk.co.asepstrath.bank;

import org.h2.tools.Server;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.services.DataSyncService;


import javax.sql.DataSource;

import java.nio.file.Path;
import java.sql.*;

public class AppLifecycleManager {

    private final DataSource ds;
    private final Logger log;
    private Server h2Server;

    private static final Path ACCOUNTS_CSV = Path.of("data/accounts.csv");

    public AppLifecycleManager(DataSource ds, Logger log) {
        this.ds = ds;
        this.log = log;
    }

    public void onStart() {
        log.info("Starting Up...");
        startH2Console();

        DatabaseInitialiser dbInit = new DatabaseInitialiser(ds, log);
        dbInit.initialiseSchema();
        dbInit.seedDemoAccount(); // seeds account only


        DataSyncService syncService = new DataSyncService(ds, log);
        syncService.syncAll();

        dbInit.seedDemoHolding(); // seed holding after sync so it doesn't get nuked
        log.info("Startup complete");
    }

    public void onStop() {
        log.info("Shutting Down...");
        if (h2Server != null) {
            h2Server.stop();
            log.info("H2 Console stopped");
        }
    }


    private void startH2Console() {
        try {
            h2Server = Server.createWebServer("-webPort", "8082", "-webAllowOthers").start();
            log.info("H2 Console available at http://localhost:8082");
        } catch (SQLException e) {
            log.error("Failed to start H2 console", e);
        }
    }
}