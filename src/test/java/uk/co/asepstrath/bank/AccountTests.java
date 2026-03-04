package uk.co.asepstrath.bank;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AccountTests {

    @Test void createAccount() {
        Account a = new Account("id-1", "Test User", BigDecimal.valueOf(100));
        assertNotNull(a);
        assertEquals("id-1", a.getAccountId());
        assertEquals("Test User", a.getName());
        assertEquals(BigDecimal.valueOf(100), a.getBalance());
    }

    @Test void createAccountWithNullBalance_defaultsToZero() {
        Account a = new Account("id-1", "Test", null);
        assertEquals(BigDecimal.ZERO, a.getBalance());
    }

    @Test void deposit_positiveAmount() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        a.deposit(BigDecimal.valueOf(50));
        assertEquals(BigDecimal.valueOf(150), a.getBalance());
    }

    @Test void deposit_pennyPrecision() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(5.45));
        a.deposit(BigDecimal.valueOf(17.56));
        assertEquals(BigDecimal.valueOf(23.01), a.getBalance());
    }

    @Test void deposit_multipleTimes() {
        Account a = new Account("id-1", "Test", BigDecimal.ZERO);
        a.deposit(BigDecimal.valueOf(10));
        a.deposit(BigDecimal.valueOf(20));
        a.deposit(BigDecimal.valueOf(30));
        assertEquals(BigDecimal.valueOf(60), a.getBalance());
    }

    @Test void deposit_zero_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        BigDecimal amount = BigDecimal.ZERO;
        assertThrows(ArithmeticException.class, () -> a.deposit(amount));
    }

    @Test void deposit_negative_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        BigDecimal amount = BigDecimal.valueOf(-10);
        assertThrows(ArithmeticException.class, () -> a.deposit(amount));
    }

    @Test void deposit_null_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.deposit(null));
    }

    @Test void deposit_exceedingMaxBalance_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(999_999_990));
        BigDecimal depositAmount = BigDecimal.valueOf(100);
        assertThrows(ArithmeticException.class, () -> a.deposit(depositAmount));
    }

    @Test void withdraw_validAmount() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        a.withdraw(BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(60), a.getBalance());
    }

    @Test void withdraw_entireBalance() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(50));
        a.withdraw(BigDecimal.valueOf(50));
        assertEquals(BigDecimal.ZERO, a.getBalance());
    }

    @Test void withdraw_moreThanBalance_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(30));
        BigDecimal amount = BigDecimal.valueOf(100);
        assertThrows(ArithmeticException.class, () -> a.withdraw(amount));
    }

    @Test void withdraw_zero_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        BigDecimal amount = BigDecimal.ZERO;
        assertThrows(ArithmeticException.class, () -> a.withdraw(amount));
    }

    @Test void withdraw_negative_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        BigDecimal withdrawAmount = BigDecimal.valueOf(-5);
        assertThrows(ArithmeticException.class, () -> a.withdraw(withdrawAmount));
    }

    @Test void withdraw_null_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.withdraw(null));
    }

    @Test void depositThenWithdraw_complexSequence() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(20));
        for (int i = 0; i < 5; i++) a.deposit(BigDecimal.valueOf(10));
        for (int i = 0; i < 3; i++) a.withdraw(BigDecimal.valueOf(20));
        assertEquals(BigDecimal.valueOf(10), a.getBalance());
    }

    @Test
    void testTickerValidation() {
        // These should pass validation based on Story #2
        assertTrue(Account.isValidTicker("AAPL"));
        assertTrue(Account.isValidTicker("TSLA"));
        
        // These should fail (Business Rules: 1-5 Uppercase only)
        assertFalse(Account.isValidTicker("apple")); // Lowercase
        assertFalse(Account.isValidTicker("12345")); // Numbers
        assertFalse(Account.isValidTicker("TOOLONG")); // More than 5 chars
    }
}