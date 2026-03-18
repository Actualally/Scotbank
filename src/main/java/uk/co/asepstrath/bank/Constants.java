package uk.co.asepstrath.bank;

import java.math.BigDecimal;

public final class Constants {

    public static final String ROUTE_ACCOUNT = "/account";
    public static final String ROUTE_LOGIN = "/login";
    public static final String ROUTE_LOGOUT = "/logout";
    public static final String ROUTE_CREATEACC = "/register";
    public static final String ROUTE_FORGOT = "/forgot";
    public static final String ROUTE_DEPOSIT = "/deposit";
    public static final String ROUTE_WITHDRAW = "/withdraw";
    public static final String ROUTE_PROCESS = "/process";
    public static final String ROUTE_BALANCE = "/balance";

    public static final String TEMPLATE_DEPOSIT = "deposit.hbs";
    public static final String TEMPLATE_ACCOUNT = "account.hbs";
    public static final String TEMPLATE_WITHDRAW = "withdraw.hbs";
    public static final String TEMPLATE_LOGIN = "login.hbs";
    public static final String TEMPLATE_FORGOT = "forgot.hbs";
    public static final String TEMPLATE_CREATE = "create.hbs";

    public static final String SESSION_ACCOUNT_ID = "accountid";
    public static final String SESSION_ACCOUNT_NAME = "name";
    public static final String SESSION_ERROR_MESSAGE = "error";
    public static final String SESSION_SUCCESS_MESSAGE = "success";


    public static final BigDecimal MAX_BALANCE = BigDecimal.valueOf(999_999_999.99);

    public static final String TICKER_REGEX = "^[A-Z]{1,5}$";

    private Constants() {}
}