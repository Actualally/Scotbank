package uk.co.asepstrath.bank.models;

import java.util.UUID;

//Represents an investor on the platform

public class Investor {

    private UUID id;
    private String name;
    private double cashBalance;

    //Only populated from /api/investors/{id})
    private String email;
    private String phone;
    private String address;
    private String taxId;

    public Investor() {}

    public Investor(UUID id, String name, double cashBalance) {
        this.id = id;
        this.name = name;
        this.cashBalance = cashBalance;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    @Override
    public String toString() {
        return String.format("Investor{id=%s, name='%s', cashBalance=%.2f}", id, name, cashBalance);
    }
}