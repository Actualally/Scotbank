package uk.co.asepstrath.bank;

public class Account {
    int balance;

    public Account(){
        balance = 0;
    }


    public void deposit(int amount) {
        if (amount <=0){
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
    }

    public int getBalance() {
        return balance;
    }

}
