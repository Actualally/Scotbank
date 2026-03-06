package uk.co.scotbank;

/**
 * InvestmentAccount - QA INTERFACE
 * Required by the QA test suite for investment/trading functionality.
 * The test runner discovers implementing classes via reflection.
 * Handles buying/selling stocks and tracking positions with average book price.
 */

public interface InvestmentAccount {

    void buyStock(String ticker, int shares, double price) throws IllegalArgumentException;

    void sellStock(String ticker, int shares, double price) throws IllegalArgumentException;

    int getShares(String ticker);

    double getBookPrice(String ticker);
}
