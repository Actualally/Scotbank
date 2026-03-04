package uk.co.asepstrath.bank.controllers;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.services.AccountService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.co.asepstrath.bank.Constants.*;

@Path(ROUTE_ACCOUNT)
public class AccountController {

    private final AccountService accountService;
    private final Logger logger;
    private static final String DEMO_ACCOUNT_ID = "investor-001";
    private static final String DB_NAME = "name";
    private static final String DB_BALANCE = "balance";
    private static final String DB_ID = "accountId";

    public AccountController(AccountService accountService, Logger log) {
        this.accountService = accountService;
        this.logger = log;
    }

    private boolean isTickerValid(String ticker) {
        return Account.isValidTicker(ticker);
    }

    @GET
    public ModelAndView<Map<String, Object>> viewAccount(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        try {
            Account account = accountService.getAccountDetails(DEMO_ACCOUNT_ID);
            model.put(DB_NAME, account.getName());
            model.put(DB_BALANCE, String.format("%,.2f", account.getBalance()));
            model.put(DB_ID, DEMO_ACCOUNT_ID);

            List<Map<String, String>> transactions = accountService.getTransactionHistory(DEMO_ACCOUNT_ID);
            model.put("transactions", transactions);

        } catch (SQLException e) {
            logger.error("Error loading account", e);
            model.put(SESSION_ERROR_MESSAGE, "Could not load account data");
        }
        transferFlashMessages(ctx, model);
        return new ModelAndView<>(TEMPLATE_ACCOUNT, model);
    }

    @GET(ROUTE_DEPOSIT)
    public ModelAndView<Map<String, Object>> showDepositForm(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        try {
            BigDecimal balance = accountService.getBalance(DEMO_ACCOUNT_ID);
            if (balance != null) {
                model.put(DB_BALANCE, balance.toPlainString());
            }
        } catch (SQLException e) {
            logger.error("Error loading balance for deposit form", e);
        }
        transferFlashMessages(ctx, model);
        return new ModelAndView<>(TEMPLATE_DEPOSIT, model);
    }

    @GET(ROUTE_WITHDRAW)
    public ModelAndView<Map<String, Object>> showWithdrawalForm(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        try {
            BigDecimal balance = accountService.getBalance(DEMO_ACCOUNT_ID);
            if (balance != null) {
                model.put(DB_BALANCE, balance.toPlainString());
            }
        } catch (SQLException e) {
            logger.error("Error loading balance for withdrawal form", e);
        }
        transferFlashMessages(ctx, model);
        return new ModelAndView<>(TEMPLATE_WITHDRAW, model);
    }

    @POST(ROUTE_DEPOSIT + ROUTE_PROCESS)
    public void processDeposit(Context ctx) {
        String amountStr = ctx.form("depositamount").valueOrNull();
        logger.info("Deposit requested — raw input: '{}'", amountStr);

        try {
            BigDecimal amount = accountService.parseAndValidateAmount(amountStr);
            accountService.deposit(DEMO_ACCOUNT_ID, amount);
            ctx.session().put(SESSION_SUCCESS_MESSAGE,
                    "Successfully deposited £" + amount.toPlainString());
            ctx.sendRedirect(ROUTE_ACCOUNT);

        } catch (ArithmeticException e) {
            logger.info("Deposit failed: {}", e.getMessage());
            ctx.session().put(SESSION_ERROR_MESSAGE, e.getMessage());
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_DEPOSIT);
        } catch (SQLException e) {
            logger.error("Database error during deposit", e);
            ctx.session().put(SESSION_ERROR_MESSAGE, "A system error occurred. Please try again.");
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_DEPOSIT);
        }
    }

    @POST(ROUTE_WITHDRAW + ROUTE_PROCESS)
    public void processWithdrawal(Context ctx) {
        String amountStr = ctx.form("withdrawamount").valueOrNull();
        logger.info("Withdrawal requested — raw input: '{}'", amountStr);

        try {
            BigDecimal amount = accountService.parseAndValidateAmount(amountStr);
            accountService.withdraw(DEMO_ACCOUNT_ID, amount);
            ctx.session().put(SESSION_SUCCESS_MESSAGE,
                    "Successfully withdrawn £" + amount.toPlainString());
            ctx.sendRedirect(ROUTE_ACCOUNT);

        } catch (ArithmeticException e) {
            logger.info("Withdrawal failed: {}", e.getMessage());
            ctx.session().put(SESSION_ERROR_MESSAGE, e.getMessage());
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_WITHDRAW);
        } catch (SQLException e) {
            logger.error("Database error during withdrawal", e);
            ctx.session().put(SESSION_ERROR_MESSAGE, "A system error occurred. Please try again.");
            ctx.sendRedirect(ROUTE_ACCOUNT + ROUTE_WITHDRAW);
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