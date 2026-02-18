package uk.co.asepstrath.bank;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountTests {
    Account account = new Account();

    @Test
    public void createAccount(){
        Account a = new Account();
        Assertions.assertTrue(a != null);
    }

    @Test
    public void initialBalanceIsZero(){
        Assertions.assertEquals(0,account.getBalance());
    }

    @Test
    public void depositPositiveAmount(){
        account.deposit(10);
        Assertions.assertEquals(10,account.getBalance());
    }

    @Test
    public void depositZeroThrowsException(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            account.deposit(0);
        });
    }

    @Test
    public void depositNegativeAmountThrowsException(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> account.deposit(-10));
    }


}
