package uk.co.asepstrath.bank;

import java.math.BigDecimal;

import static uk.co.asepstrath.bank.Constants.MAX_BALANCE;

public class Account {

    private final String accountId;
    private final String name;
    private BigDecimal balance;

    public Account(String accountId, String name, BigDecimal balance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = (balance != null) ? balance : BigDecimal.ZERO;
    }

    public void deposit(BigDecimal amount) throws ArithmeticException {
        if (amount == null) {
            throw new ArithmeticException("Deposit amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArithmeticException("Deposit amount must be greater than 0");
        }
        if (balance.add(amount).compareTo(MAX_BALANCE) > 0) {
            throw new ArithmeticException("Deposit would exceed maximum account balance");
        }
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) throws ArithmeticException {
        if (amount == null) {
            throw new ArithmeticException("Withdrawal amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArithmeticException("Withdrawal amount must be greater than 0");
        }
        if (amount.compareTo(balance) > 0) {
            throw new ArithmeticException("Insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    public BigDecimal getBalance() { return balance; }
    public String getAccountId() { return accountId; }
    public String getName() { return name; }

    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }

    @Override
    public String toString() {
        return String.format("Account{id='%s', name='%s', balance=%s}",
                accountId, name, balance);
    }

    public static boolean isValidTicker(String ticker) {
        if (ticker == null) {
            return false;
        }
        return ticker.matches(Constants.TICKER_REGEX);
    }
}
