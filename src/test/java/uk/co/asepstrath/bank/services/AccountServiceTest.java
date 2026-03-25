package uk.co.asepstrath.bank.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;


class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setup() {
        Logger logger = LoggerFactory.getLogger("test");
        accountService = new AccountService(null, null, logger);
    }

    // ── parseAndValidateAmount ──────────────────────────────────────────────

    @Test
    void parseAndValidateAmount_validInteger() {
        BigDecimal result = accountService.parseAndValidateAmount("100");
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void parseAndValidateAmount_validDecimal() {
        BigDecimal result = accountService.parseAndValidateAmount("49.99");
        assertEquals(new BigDecimal("49.99"), result);
    }

    @Test
    void parseAndValidateAmount_roundsHalfUp() {
        BigDecimal result = accountService.parseAndValidateAmount("10.555");
        assertEquals(new BigDecimal("10.56"), result);
    }

    @Test
    void parseAndValidateAmount_withWhitespace() {
        BigDecimal result = accountService.parseAndValidateAmount("  25.00  ");
        assertEquals(new BigDecimal("25.00"), result);
    }

    @Test
    void parseAndValidateAmount_null_throwsArithmeticException() {
        assertThrows(ArithmeticException.class,
                () -> accountService.parseAndValidateAmount(null));
    }

    @Test
    void parseAndValidateAmount_emptyString_throwsArithmeticException() {
        assertThrows(ArithmeticException.class,
                () -> accountService.parseAndValidateAmount(""));
    }

    @Test
    void parseAndValidateAmount_whitespaceOnly_throwsArithmeticException() {
        assertThrows(ArithmeticException.class,
                () -> accountService.parseAndValidateAmount("   "));
    }

    @Test
    void parseAndValidateAmount_letters_throwsArithmeticException() {
        assertThrows(ArithmeticException.class,
                () -> accountService.parseAndValidateAmount("abc"));
    }

    @Test
    void parseAndValidateAmount_mixed_throwsArithmeticException() {
        assertThrows(ArithmeticException.class,
                () -> accountService.parseAndValidateAmount("12abc"));
    }

    @Test
    void parseAndValidateAmount_negativeIsAllowed_returnsBigDecimal() {
        // parseAndValidateAmount doesn't reject negatives — that's Account.deposit/withdraw's job
        BigDecimal result = accountService.parseAndValidateAmount("-10");
        assertEquals(new BigDecimal("-10.00"), result);
    }

    @Test
    void parseAndValidateAmount_zero_returnsZero() {
        BigDecimal result = accountService.parseAndValidateAmount("0");
        assertEquals(new BigDecimal("0.00"), result);
    }

    // ── getPortfolioSummary ─────────────────────────────────────────────────

    @Test
    void getPortfolioSummary_emptyHoldings_returnsZeros() {
        Map<String, String> summary = accountService.getPortfolioSummary(new ArrayList<>());
        assertEquals("0.00", summary.get("totalCurrentValue"));
        assertEquals("0.00", summary.get("totalGainLoss"));
        assertEquals("0.00", summary.get("totalGainLossPct"));
        assertEquals("true", summary.get("totalGainLossPositive")); // 0 gain is non-negative
    }

    @Test
    void getPortfolioSummary_singleHolding_positiveGain() {
        List<Map<String, Object>> holdings = new ArrayList<>();
        Map<String, Object> holding = new HashMap<>();
        holding.put("currentValue", "1200.00");
        holding.put("totalCost", new BigDecimal("1000.00"));
        holdings.add(holding);

        Map<String, String> summary = accountService.getPortfolioSummary(holdings);

        assertEquals("1,200.00", summary.get("totalCurrentValue"));
        assertEquals("200.00", summary.get("totalGainLoss"));
        assertEquals("20.00", summary.get("totalGainLossPct"));
        assertEquals("true", summary.get("totalGainLossPositive"));
    }

    @Test
    void getPortfolioSummary_singleHolding_negativeGain() {
        List<Map<String, Object>> holdings = new ArrayList<>();
        Map<String, Object> holding = new HashMap<>();
        holding.put("currentValue", "800.00");
        holding.put("totalCost", new BigDecimal("1000.00"));
        holdings.add(holding);

        Map<String, String> summary = accountService.getPortfolioSummary(holdings);

        assertEquals("800.00", summary.get("totalCurrentValue"));
        assertEquals("-200.00", summary.get("totalGainLoss"));
        assertEquals("false", summary.get("totalGainLossPositive"));
    }

    @Test
    void getPortfolioSummary_multipleHoldings_aggregatesCorrectly() {
        List<Map<String, Object>> holdings = new ArrayList<>();

        Map<String, Object> h1 = new HashMap<>();
        h1.put("currentValue", "500.00");
        h1.put("totalCost", new BigDecimal("400.00"));

        Map<String, Object> h2 = new HashMap<>();
        h2.put("currentValue", "300.00");
        h2.put("totalCost", new BigDecimal("300.00"));

        holdings.add(h1);
        holdings.add(h2);

        Map<String, String> summary = accountService.getPortfolioSummary(holdings);

        assertEquals("800.00", summary.get("totalCurrentValue"));
        assertEquals("100.00", summary.get("totalGainLoss"));
        assertEquals("true", summary.get("totalGainLossPositive"));
    }

    @Test
    void getPortfolioSummary_zeroCost_gainLossPctIsZero() {
        List<Map<String, Object>> holdings = new ArrayList<>();
        Map<String, Object> holding = new HashMap<>();
        holding.put("currentValue", "100.00");
        holding.put("totalCost", new BigDecimal("0.00"));
        holdings.add(holding);

        Map<String, String> summary = accountService.getPortfolioSummary(holdings);
        assertEquals("0.00", summary.get("totalGainLossPct"));
    }
}
