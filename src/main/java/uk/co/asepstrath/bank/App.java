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

import uk.co.asepstrath.bank.controllers.*;
import uk.co.asepstrath.bank.repositories.*;
import uk.co.asepstrath.bank.services.*;

import javax.sql.DataSource;

public class App extends Jooby {

    private AppLifecycleManager lifecycleManager;

    public App() {
        install(new UniRestExtension());
        install(new HandlebarsModule());
        install(new HikariModule("mem"));

        setSessionStore(SessionStore.memory(Cookie.session("scotbank")));

        assets("/assets/*", "/assets");
        assets("/service_worker.js", "/service_worker.js");

        DataSource ds = require(DataSource.class);
        Logger log = getLog();

        AccountRepository accountRepository = new AccountRepository(ds);
        AccountService accountService = new AccountService(accountRepository, log);

        mvc(new AccountController_(accountService, log));
        mvc(new LoginController_(ds, log));
        mvc(new LogoutController_());

        lifecycleManager = new AppLifecycleManager(ds, log);
        onStarted(lifecycleManager::onStart);
        onStop(lifecycleManager::onStop);
    }

    public static void main(final String[] args) {
        runApp(args, new NettyServer(new ServerOptions()), App::new);
    }
}