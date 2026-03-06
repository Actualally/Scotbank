package uk.co.asepstrath.bank;

import uk.co.scotbank.CashAccount;
import uk.co.scotbank.InvestmentAccount;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static uk.co.asepstrath.bank.Constants.MAX_BALANCE;

/**
 * Account - CORE DOMAIN MODEL
 * Represents a client's account at Scotbank Investments. Implements both the
 * CashAccount and InvestmentAccount interfaces required by the QA test suite.
 * The QA test runner discovers this class via reflection as a subtype of both
 * interfaces, so the method signatures must match exactly.
 * CashAccount: deposit, withdraw, and balance tracking.
 * InvestmentAccount: buying/selling stocks and tracking holdings with average book price.
 */

public class Account implements CashAccount, InvestmentAccount {

    private final String accountId;
    private final String name;
    private double balance;

    //Tracks number of shares held per ticker
    private final Map<String, Integer> sharesHeld;

    //Tracks total cost invested per ticker (used to calculate average book price)
    private final Map<String, Double> totalCost;

    //Default constructor for QA tests (creates account with zero balance)
    public Account() {
        this("", "", 0.0);
    }

    public Account(String accountId, String name, BigDecimal balance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = (balance != null) ? balance.doubleValue() : 0.0;
        this.sharesHeld = new HashMap<>();
        this.totalCost = new HashMap<>();
    }

    public Account(String accountId, String name, double balance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = balance;
        this.sharesHeld = new HashMap<>();
        this.totalCost = new HashMap<>();
    }

    //CashAccount interface methods

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public void deposit(double amount) throws IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        if (hasMoreThanTwoDecimalPlaces(amount)) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places");
        }
        if (balance + amount > MAX_BALANCE.doubleValue()) {
            throw new IllegalArgumentException("Deposit would exceed maximum account balance");
        }
        balance = roundToTwoDecimals(balance + amount);
    }

    @Override
    public void withdraw(double amount) throws IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than 0");
        }
        if (hasMoreThanTwoDecimalPlaces(amount)) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        balance = roundToTwoDecimals(balance - amount);
    }

    //BigDecimal overloads for the service/repository layer
    public void deposit(BigDecimal amount) throws ArithmeticException {
        if (amount == null) {
            throw new ArithmeticException("Deposit amount cannot be null");
        }
        try {
            deposit(amount.doubleValue());
        } catch (IllegalArgumentException e) {
            throw new ArithmeticException(e.getMessage());
        }
    }

    public void withdraw(BigDecimal amount) throws ArithmeticException {
        if (amount == null) {
            throw new ArithmeticException("Withdrawal amount cannot be null");
        }
        try {
            withdraw(amount.doubleValue());
        } catch (IllegalArgumentException e) {
            throw new ArithmeticException(e.getMessage());
        }
    }

    public BigDecimal getBalanceAsBigDecimal() {
        return BigDecimal.valueOf(balance);
    }

    //InvestmentAccount interface methods

    @Override
    public void buyStock(String ticker, int shares, double price) throws IllegalArgumentException {
        validateTradeInputs(ticker, shares, price);

        double cost = roundToTwoDecimals(shares * price);
        sharesHeld.merge(ticker, shares, Integer::sum);
        totalCost.merge(ticker, cost, Double::sum);
    }

    @Override
    public void sellStock(String ticker, int shares, double price) throws IllegalArgumentException {
        validateTradeInputs(ticker, shares, price);

        int currentShares = sharesHeld.getOrDefault(ticker, 0);
        if (currentShares == 0) {
            throw new IllegalArgumentException("No position in " + ticker);
        }
        if (shares > currentShares) {
            throw new IllegalArgumentException("Cannot sell more shares than owned");
        }

        double currentBookPrice = getBookPrice(ticker);
        int remainingShares = currentShares - shares;

        if (remainingShares == 0) {
            sharesHeld.remove(ticker);
            totalCost.remove(ticker);
        } else {
            sharesHeld.put(ticker, remainingShares);
            totalCost.put(ticker, remainingShares * currentBookPrice);
        }
    }

    @Override
    public int getShares(String ticker) {
        return sharesHeld.getOrDefault(ticker, 0);
    }

    @Override
    public double getBookPrice(String ticker) {
        int shares = sharesHeld.getOrDefault(ticker, 0);
        if (shares == 0) {
            return 0.0;
        }
        return totalCost.getOrDefault(ticker, 0.0) / shares;
    }

    //Utility methods

    public String getAccountId() { return accountId; }
    public String getName() { return name; }

    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance.doubleValue();
    }

    private void validateTradeInputs(String ticker, int shares, double price) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares must be greater than 0");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (hasMoreThanTwoDecimalPlaces(price)) {
            throw new IllegalArgumentException("Price cannot have more than two decimal places");
        }
    }

    private boolean hasMoreThanTwoDecimalPlaces(double value) {
        double shifted = value * 100;
        return Math.abs(shifted - Math.round(shifted)) > 1e-9;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return String.format("Account{id='%s', name='%s', balance=%.2f}", accountId, name, balance);
    }

    public static boolean isValidTicker(String ticker) {
        if (ticker == null) {
            return false;
        }
        return ticker.matches(Constants.TICKER_REGEX);
    }
}