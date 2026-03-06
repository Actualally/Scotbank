package uk.co.asepstrath.bank.controllers;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.co.asepstrath.bank.Constants.*;
@Path(ROUTE_LOGIN)
public class LoginController {

	private final DataSource dataSource;
  	private final Logger logger;

  	public LoginController(DataSource ds, Logger log) {
    	this.dataSource = ds;
        this.logger = log;
    }


	@GET
	public ModelAndView showLoginPage(Context ctx) {

		Map<String, Object> model = new HashMap<>();

		var session = ctx.sessionOrNull();


		//checks if user is logged in and redirects if they are
        if (session != null && session.get(SESSION_ACCOUNT_ID).isPresent()) {
            ctx.sendRedirect(ROUTE_ACCOUNT);
        }
		transferFlashMessages(ctx, model);
		return new ModelAndView(TEMPLATE_LOGIN, model);
		
	}



	private void transferFlashMessages(Context ctx, Map<String, Object> model) {
        var session = ctx.sessionOrNull();
        if (session == null) return;
        for (String key : new String[]{SESSION_SUCCESS_MESSAGE, SESSION_ERROR_MESSAGE}) {
            if (session.get(key).isPresent()) {
                model.put(key, session.get(key).value());
                session.remove(key);
            }
        }
    }
  
}
