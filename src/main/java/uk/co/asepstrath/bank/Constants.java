package uk.co.asepstrath.bank;

import java.math.BigDecimal;

public final class Constants {

    public static final String ROUTE_ACCOUNT = "/account";
    public static final String ROUTE_DEPOSIT = "/deposit";
    public static final String ROUTE_WITHDRAW = "/withdraw"; //might violate on sonarQube, will ask on Wednesday week 6
    public static final String ROUTE_PROCESS = "/process";
    public static final String ROUTE_BALANCE = "/balance";

    public static final String TEMPLATE_DEPOSIT = "deposit.hbs";
    public static final String TEMPLATE_ACCOUNT = "account.hbs";
    public static final String TEMPLATE_WITHDRAW = "withdraw.hbs";

    public static final String SESSION_ACCOUNT_ID = "accountid"; //might violate on sonarQube, will ask on Wednesday week 6
    public static final String SESSION_ACCOUNT_NAME = "name"; //might violate on sonarQube, will ask on Wednesday week 6
    public static final String SESSION_ERROR_MESSAGE = "error";
    public static final String SESSION_SUCCESS_MESSAGE = "success";

    public static final String DB_BALANCE = "balance";

    public static final BigDecimal MAX_BALANCE = BigDecimal.valueOf(999_999_999.99);

    private Constants() {}
}