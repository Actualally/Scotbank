package uk.co.asepstrath.bank;

import uk.co.scotbank.CashAccount;

import java.math.BigDecimal;

import static uk.co.asepstrath.bank.Constants.MAX_BALANCE;

/**
 * Account - CORE DOMAIN MODEL
 * Represents a client's account at Scotbank Investments. Implements the
 * CashAccount interface required by the QA test suite for deposit/withdraw
 * functionality. The QA test runner discovers this class via reflection
 * as a subtype of CashAccount, so the method signatures must match exactly.
 */

public class Account implements CashAccount {

    private final String accountId;
    private final String name;
    private double balance;

    // Default constructor for QA tests (creates account with zero balance)
    public Account() {
        this("", "", 0.0);
    }

    public Account(String accountId, String name, BigDecimal balance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = (balance != null) ? balance.doubleValue() : 0.0;
    }

    public Account(String accountId, String name, double balance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = balance;
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

    //Utility methods

    public String getAccountId() { return accountId; }
    public String getName() { return name; }

    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance.doubleValue();
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
        return String.format("Account{id='%s', name='%s', balance=%.2f}",
                accountId, name, balance);
    }

    public static boolean isValidTicker(String ticker) {
        if (ticker == null) {
            return false;
        }
        return ticker.matches(Constants.TICKER_REGEX);
    }
}