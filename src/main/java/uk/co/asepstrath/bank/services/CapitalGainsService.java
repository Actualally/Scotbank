package uk.co.asepstrath.bank.services;

import org.slf4j.Logger;
import uk.co.asepstrath.bank.repositories.AccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class CapitalGainsService {

    private static final BigDecimal ALLOWANCE = new BigDecimal("3000.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.24");
    private static final String FIELD_TICKER = "ticker";
    private static final String STRING_FORMATTER = "%,.2f";

    private final AccountRepository accountRepository;
    private final Logger logger;

    public CapitalGainsService(AccountRepository accountRepository, Logger logger) {
        this.accountRepository = accountRepository;
        this.logger = logger;
    }

    public Map<String, Object> calculateCapitalGains(String accountId, int year) throws SQLException {
        List<Map<String, Object>> sells = accountRepository.getSellTransactions(accountId, year);
        List<Map<String, Object>> allBuys = accountRepository.getBuyTransactions(accountId);

        // Build FIFO queue of buy costs per ticker
        Map<String, Queue<BigDecimal>> buyQueue = new HashMap<>();
        for (Map<String, Object> buy : allBuys) {
            String ticker = (String) buy.get(FIELD_TICKER);
            buyQueue.computeIfAbsent(ticker, k -> new LinkedList<>())
                    .add((BigDecimal) buy.get("amount"));
        }

        logger.info("Buy queue built: {}", buyQueue);

        BigDecimal totalGains = BigDecimal.ZERO;
        BigDecimal totalLosses = BigDecimal.ZERO;
        List<Map<String, Object>> salesDetail = new ArrayList<>();

        for (Map<String, Object> sell : sells) {
            String ticker = (String) sell.get(FIELD_TICKER);
            BigDecimal proceeds = (BigDecimal) sell.get("proceeds");

            // Pop earliest BUY for this ticker
            BigDecimal costBasis = BigDecimal.ZERO;
            Queue<BigDecimal> queue = buyQueue.get(ticker);
            if (queue != null && !queue.isEmpty()) {
                costBasis = queue.poll();
            }

            logger.info("Sell {}: proceeds={} costBasis={}", ticker, proceeds, costBasis);

            BigDecimal gain = proceeds.subtract(costBasis);

            if (gain.compareTo(BigDecimal.ZERO) >= 0) {
                totalGains = totalGains.add(gain);
            } else {
                totalLosses = totalLosses.add(gain.abs());
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("date", sell.get("date"));
            detail.put(FIELD_TICKER, ticker);
            detail.put("proceeds", String.format(STRING_FORMATTER, proceeds));
            detail.put("costBasis", String.format(STRING_FORMATTER, costBasis));
            detail.put("gain", String.format(STRING_FORMATTER, gain));
            detail.put("gainPositive", gain.compareTo(BigDecimal.ZERO) >= 0);
            salesDetail.add(detail);
        }

        BigDecimal netGains = totalGains.subtract(totalLosses);
        BigDecimal taxableGains = netGains.subtract(ALLOWANCE).max(BigDecimal.ZERO);
        BigDecimal estimatedTax = taxableGains.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal allowanceUsed = netGains.max(BigDecimal.ZERO).min(ALLOWANCE);
        BigDecimal allowanceRemaining = ALLOWANCE.subtract(allowanceUsed).max(BigDecimal.ZERO);
        double allowancePct = Math.min(100.0, netGains.compareTo(BigDecimal.ZERO) > 0
                ? netGains.divide(ALLOWANCE, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0);

        Map<String, Object> result = new HashMap<>();
        result.put("salesDetail", salesDetail);
        result.put("totalGains", String.format(STRING_FORMATTER, totalGains));
        result.put("totalLosses", String.format(STRING_FORMATTER, totalLosses));
        result.put("netGains", String.format(STRING_FORMATTER, netGains));
        result.put("netGainsPositive", netGains.compareTo(BigDecimal.ZERO) >= 0);
        result.put("taxableGains", String.format(STRING_FORMATTER, taxableGains));
        result.put("estimatedTax", String.format(STRING_FORMATTER, estimatedTax));
        result.put("allowanceUsed", String.format(STRING_FORMATTER, allowanceUsed));
        result.put("allowanceRemaining", String.format(STRING_FORMATTER, allowanceRemaining));
        result.put("allowancePct", String.format("%.1f", allowancePct));
        result.put("allowanceProgressPct", Math.min(100.0, allowancePct));
        result.put("selectedYear", year);
        result.put("hasSales", !salesDetail.isEmpty());
        return result;
    }

    public List<Integer> getAvailableYears(String accountId) throws SQLException {
        List<Map<String, Object>> allSells = accountRepository.getSellTransactions(accountId);
        logger.info("getAvailableYears: found {} sells for {}", allSells.size(), accountId);
        Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        years.add(LocalDate.now().getYear());
        for (Map<String, Object> sell : allSells) {
            String date = (String) sell.get("date");
            years.add(Integer.parseInt(date.substring(0, 4)));
        }
        return new ArrayList<>(years);
    }
}
