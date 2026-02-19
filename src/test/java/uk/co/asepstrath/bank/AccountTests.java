package uk.co.asepstrath.bank;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTests {

    @Test public void createAccount() {
        Account a = new Account("id-1", "Test User", BigDecimal.valueOf(100));
        assertNotNull(a);
        assertEquals("id-1", a.getAccountId());
        assertEquals("Test User", a.getName());
        assertEquals(BigDecimal.valueOf(100), a.getBalance());
    }

    @Test public void createAccountWithNullBalance_defaultsToZero() {
        Account a = new Account("id-1", "Test", null);
        assertEquals(BigDecimal.ZERO, a.getBalance());
    }

    @Test public void deposit_positiveAmount() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        a.deposit(BigDecimal.valueOf(50));
        assertEquals(BigDecimal.valueOf(150), a.getBalance());
    }

    @Test public void deposit_pennyPrecision() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(5.45));
        a.deposit(BigDecimal.valueOf(17.56));
        assertEquals(BigDecimal.valueOf(23.01), a.getBalance());
    }

    @Test public void deposit_multipleTimes() {
        Account a = new Account("id-1", "Test", BigDecimal.ZERO);
        a.deposit(BigDecimal.valueOf(10));
        a.deposit(BigDecimal.valueOf(20));
        a.deposit(BigDecimal.valueOf(30));
        assertEquals(BigDecimal.valueOf(60), a.getBalance());
    }

    @Test public void deposit_zero_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.deposit(BigDecimal.ZERO));
    }

    @Test public void deposit_negative_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.deposit(BigDecimal.valueOf(-10)));
    }

    @Test public void deposit_null_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.deposit(null));
    }

    @Test public void deposit_exceedingMaxBalance_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(999_999_990));
        assertThrows(ArithmeticException.class, () -> a.deposit(BigDecimal.valueOf(100)));
    }

    @Test public void withdraw_validAmount() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        a.withdraw(BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(60), a.getBalance());
    }

    @Test public void withdraw_entireBalance() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(50));
        a.withdraw(BigDecimal.valueOf(50));
        assertEquals(BigDecimal.ZERO, a.getBalance());
    }

    @Test public void withdraw_moreThanBalance_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(30));
        assertThrows(ArithmeticException.class, () -> a.withdraw(BigDecimal.valueOf(100)));
    }

    @Test public void withdraw_zero_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.withdraw(BigDecimal.ZERO));
    }

    @Test public void withdraw_negative_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.withdraw(BigDecimal.valueOf(-5)));
    }

    @Test public void withdraw_null_throwsException() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(100));
        assertThrows(ArithmeticException.class, () -> a.withdraw(null));
    }

    @Test public void depositThenWithdraw_complexSequence() {
        Account a = new Account("id-1", "Test", BigDecimal.valueOf(20));
        for (int i = 0; i < 5; i++) a.deposit(BigDecimal.valueOf(10));
        for (int i = 0; i < 3; i++) a.withdraw(BigDecimal.valueOf(20));
        assertEquals(BigDecimal.valueOf(10), a.getBalance());
    }
}