package uk.co.asepstrath.bank.models;

import java.time.LocalDate;
import java.util.UUID;

//Represents one transaction from the transaction history

public class Transaction {

    private UUID id;
    private LocalDate date;
    private String type;
    private UUID investorId;
    private String ticker;
    private double totalCashAmount;

    public Transaction() {}

    public Transaction(UUID id, LocalDate date, String type,
                       UUID investorId, String ticker, double totalCashAmount) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.investorId = investorId;
        this.ticker = ticker;
        this.totalCashAmount = totalCashAmount;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UUID getInvestorId() { return investorId; }
    public void setInvestorId(UUID investorId) { this.investorId = investorId; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public double getTotalCashAmount() { return totalCashAmount; }
    public void setTotalCashAmount(double totalCashAmount) { this.totalCashAmount = totalCashAmount; }

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, type='%s', ticker='%s', amount=%.2f}",
                id, type, ticker, totalCashAmount);
    }
}