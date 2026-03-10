package uk.co.asepstrath.bank.models;

import org.junit.jupiter.api.Test;
import uk.co.asepstrath.bank.Account;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTests {

    @Test
    void investorFieldsAndDefaults() {
        Investor empty = new Investor();
        assertNull(empty.getId());
        assertNull(empty.getEmail());

        UUID id = UUID.randomUUID();
        Investor inv = new Investor(id, "Mark Zuckerberg", 9999999999.99);
        assertEquals(id, inv.getId());
        assertEquals("Mark Zuckerberg", inv.getName());
        assertEquals(9999999999.99, inv.getCashBalance(), 0.001);
        assertNull(inv.getEmail());

        inv.setEmail("zuck@meta.com");
        inv.setPhone("07700900123");
        inv.setAddress("1459 Hamilton Ave");
        inv.setTaxId("AB123456C");
        assertEquals("zuck@meta.com", inv.getEmail());
        assertEquals("07700900123", inv.getPhone());
        assertEquals("1459 Hamilton Ave", inv.getAddress());
        assertEquals("AB123456C", inv.getTaxId());
    }

    @Test
    void equityFieldsAndDefaults() {
        Equity empty = new Equity();
        assertNull(empty.getTicker());

        Equity eq = new Equity("Citadel Trust", "CTT", "Finance");
        assertEquals("Citadel Trust", eq.getName());
        assertEquals("CTT", eq.getTicker());
        assertEquals("Finance", eq.getSector());
    }

    @Test
    void etfFieldsAndDefaults() {
        ETF empty = new ETF();
        assertNull(empty.getConstituents());

        ETF etf = new ETF("Small Cap Growth", "SMLX", "High-growth fund",
                LocalDate.of(2021, 9, 1), Arrays.asList("OPE", "TAE", "PLD"));
        assertEquals("SMLX", etf.getTicker());
        assertEquals(LocalDate.of(2021, 9, 1), etf.getInceptionDate());
        assertEquals(3, etf.getConstituents().size());
        assertTrue(etf.getConstituents().contains("OPE"));
    }

    @Test
    void pricePointFieldsAndDefaults() {
        PricePoint empty = new PricePoint();
        assertNull(empty.getTimestamp());

        LocalDateTime ts = LocalDateTime.of(2025, 2, 3, 10, 30);
        PricePoint pp = new PricePoint(ts, 150.75);
        assertEquals(ts, pp.getTimestamp());
        assertEquals(150.75, pp.getPrice(), 0.001);
    }

    @Test
    void transactionFieldsAndDefaults() {
        Transaction empty = new Transaction();
        assertNull(empty.getType());

        UUID id = UUID.randomUUID();
        UUID investorId = UUID.randomUUID();
        Transaction t = new Transaction(id, LocalDate.of(2025, 2, 3), "BUY",
                investorId, "MRH", 28000.0);
        assertEquals("BUY", t.getType());
        assertEquals("MRH", t.getTicker());
        assertEquals(28000.0, t.getTotalCashAmount(), 0.001);

        Transaction deposit = new Transaction(UUID.randomUUID(), LocalDate.now(),
                "DEPOSIT", UUID.randomUUID(), null, 500.0);
        assertNull(deposit.getTicker());
    }

    @Test
    void tickerValidationOnModels() {
        Equity eq = new Equity("Citadel Trust", "CTT", "Finance");
        Transaction buy = new Transaction(UUID.randomUUID(), LocalDate.now(),
                "BUY", UUID.randomUUID(), "MRH", 28000.0);
        Transaction deposit = new Transaction(UUID.randomUUID(), LocalDate.now(),
                "DEPOSIT", UUID.randomUUID(), null, 500.0);

        assertTrue(Account.isValidTicker(eq.getTicker()));
        assertTrue(Account.isValidTicker(buy.getTicker()));
        assertFalse(Account.isValidTicker(deposit.getTicker()));
    }
}
