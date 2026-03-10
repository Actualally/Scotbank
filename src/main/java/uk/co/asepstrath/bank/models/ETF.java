package uk.co.asepstrath.bank.models;

import java.time.LocalDate;
import java.util.List;

//Represents an ETF on the platform, like the S&P500, constituents is a list of stocks that make up the ETF

public class ETF {

    private String name;
    private String ticker;
    private String description;
    private LocalDate inceptionDate;
    private List<String> constituents;

    public ETF() {}

    public ETF(String name, String ticker, String description,
               LocalDate inceptionDate, List<String> constituents) {
        this.name = name;
        this.ticker = ticker;
        this.description = description;
        this.inceptionDate = inceptionDate;
        this.constituents = constituents;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getInceptionDate() { return inceptionDate; }
    public void setInceptionDate(LocalDate inceptionDate) { this.inceptionDate = inceptionDate; }

    public List<String> getConstituents() { return constituents; }
    public void setConstituents(List<String> constituents) { this.constituents = constituents; }

    @Override
    public String toString() {
        return String.format("ETF{name='%s', ticker='%s', constituents=%d}",
                name, ticker, constituents != null ? constituents.size() : 0);
    }
}