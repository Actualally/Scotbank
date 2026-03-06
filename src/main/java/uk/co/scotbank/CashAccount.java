package uk.co.scotbank;

/**
 * CashAccount for QA Tests
 * This interface is required by the QA test suite. Any class that implements
 * this will be automatically discovered by the test runner via reflection.
 * All methods use double (not BigDecimal) and throw IllegalArgumentException
 * for invalid inputs. Our Account class implements this.
 */

public interface CashAccount {

    double getBalance();

    void deposit(double amount) throws IllegalArgumentException;

    void withdraw(double amount) throws IllegalArgumentException;
}