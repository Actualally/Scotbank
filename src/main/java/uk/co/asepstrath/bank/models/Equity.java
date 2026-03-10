package uk.co.asepstrath.bank.models;


//Represents a single stock on the platform

public class Equity {

    private String name;
    private String ticker;
    private String sector;

    public Equity() {}

    public Equity(String name, String ticker, String sector) {
        this.name = name;
        this.ticker = ticker;
        this.sector = sector;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    @Override
    public String toString() {
        return String.format("Equity{name='%s', ticker='%s', sector='%s'}", name, ticker, sector);
    }
}
