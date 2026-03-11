package uk.co.asepstrath.bank.controllers;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import org.slf4j.Logger;

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
	public ModelAndView<Map<String, Object>> showLoginPage(Context ctx) {

		Map<String, Object> model = new HashMap<>();

		var session = ctx.sessionOrNull();


		//checks if user is logged in and redirects if they are
        if (session != null && session.get(SESSION_ACCOUNT_ID).isPresent()) {
            ctx.sendRedirect(ROUTE_ACCOUNT);
        }
		transferFlashMessages(ctx, model);
		return new ModelAndView<>(TEMPLATE_LOGIN, model);
		
	}

	//for creating an account
	@GET(ROUTE_CREATEACC)
	public ModelAndView<Map<String, Object>> showCreateAccPage(Context ctx){

		Map<String, Object> model = new HashMap<>();

		var session = ctx.sessionOrNull();


		//checks if user is logged in and redirects if they are
        if (session != null && session.get(SESSION_ACCOUNT_ID).isPresent()) {
            ctx.sendRedirect(ROUTE_ACCOUNT);
        }
		transferFlashMessages(ctx, model);
		return new ModelAndView<>(TEMPLATE_CREATE, model);

	}

	@POST
	public void Login(Context ctx){
		String accountID = ctx.form("accountid").valueOrNull();
		String Password = ctx.form("password").valueOrNull();

		if (accountID == null || Password == null) {
			ctx.session().put(SESSION_ERROR_MESSAGE, "Please enter account ID and password");
			ctx.sendRedirect(ROUTE_LOGIN);
			return;
		}

		try (Connection conn = dataSource.getConnection();

			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT AccountID, Name, Password FROM Accounts WHERE AccountID = ?")) {

			stmt.setString(1, accountID);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					String inputPassword = rs.getString("Password");
					if(inputPassword.equals(Password)) {
						ctx.session().put(SESSION_ACCOUNT_ID, rs.getString("AccountID"));
						ctx.session().put(SESSION_ACCOUNT_NAME, rs.getString("Name"));
						logger.info("Login Succesful");
						ctx.sendRedirect(ROUTE_ACCOUNT);
					} else {
						logger.error("Failed login attempt");
						ctx.session().put(SESSION_ERROR_MESSAGE, "Invalid account ID or password");
						ctx.sendRedirect(ROUTE_LOGIN);
					}

				} else {
					logger.error("Account not found");
					ctx.session().put(SESSION_ERROR_MESSAGE, "Invalid account ID or password");
					ctx.sendRedirect(ROUTE_LOGIN);
				}
			
				
			}
			
	} catch (SQLException e) {
			logger.error("Database error during login", e);
			ctx.session().put(SESSION_ERROR_MESSAGE, "An error occurred. Please try again.");
			ctx.sendRedirect(ROUTE_LOGIN);
		}
	}


	@POST(ROUTE_CREATEACC)
	public void processCreateAcc(Context ctx){
		String name = ctx.form("name").valueOrNull();
		String Password = ctx.form("password").valueOrNull();

		if(name == null || Password == null){
			ctx.session().put(SESSION_ERROR_MESSAGE, "Please enter name and password");
			ctx.sendRedirect(ROUTE_LOGIN + ROUTE_CREATEACC);
			return;
		}

		String accountID = UUID.randomUUID().toString();

		try (Connection conn = dataSource.getConnection();

			 PreparedStatement stmt = conn.prepareStatement(
					 "INSERT INTO Accounts (AccountID, Name, Balance, Password) VALUES (?, ?, 0.00, ?)")) {

			stmt.setString(1, accountID);
			stmt.setString(2, name);
			stmt.setString(3, Password);
			stmt.executeUpdate();

			logger.info("Account created with ID: {}", accountID);
			ctx.session().put(SESSION_SUCCESS_MESSAGE, "Account created! Your unique ID is: " + accountID);
			ctx.sendRedirect(ROUTE_LOGIN);
			
		} catch (SQLException e) {
			logger.error("Error creating account", e);
			ctx.session().put(SESSION_ERROR_MESSAGE, "Could not create account. Please try again.");
			ctx.sendRedirect(ROUTE_LOGIN + ROUTE_CREATEACC);
		}
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
