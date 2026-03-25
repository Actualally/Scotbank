package uk.co.asepstrath.bank.services;

import org.slf4j.Logger;
import uk.co.asepstrath.bank.repositories.AccountRepository;

import java.sql.SQLException;
import java.util.*;

public class CategorisationService {

    private static final String FIELD_TICKER = "ticker";
    private static final String FIELD_SECTOR = "sector";
    private static final String FIELD_UNKNOWN = "Unknown";

    private final AccountRepository accountRepository;
    private final ApiService apiService;
    private final Logger logger;

    public CategorisationService(AccountRepository accountRepository,
                                 ApiService apiService,
                                 Logger logger) {
        this.accountRepository = accountRepository;
        this.apiService = apiService;
        this.logger = logger;
    }

    public Map<String, Object> getCategorisation(List<Map<String, Object>> enrichedHoldings) {
        // Collect all tickers we need metadata for
        Set<String> allTickers = new HashSet<>();
        Map<String, List<String>> etfConstituents = new HashMap<>();

        for (Map<String, Object> holding : enrichedHoldings) {
            String ticker = (String) holding.get(FIELD_TICKER);
            String sector = (String) holding.get(FIELD_SECTOR);

            if ("ETF".equals(sector)) {
                try {
                    List<String> constituents = accountRepository.getETFConstituents(ticker);
                    etfConstituents.put(ticker, constituents);
                    allTickers.addAll(constituents);
                } catch (SQLException e) {
                    logger.warn("Could not fetch constituents for ETF {}", ticker);
                }
            } else {
                allTickers.add(ticker);
            }
        }

        // Fetch metadata for all tickers at once
        Map<String, Map<String, String>> metadata = apiService.fetchEquityMetadata(
                new ArrayList<>(allTickers));

        double totalValue = 0.0;
        Map<String, Double> sectorMap = new LinkedHashMap<>();
        Map<String, Double> countryMap = new LinkedHashMap<>();
        Map<String, Double> regionMap = new LinkedHashMap<>();

        for (Map<String, Object> holding : enrichedHoldings) {
            String ticker = (String) holding.get(FIELD_TICKER);
            String sector = (String) holding.get(FIELD_SECTOR);
            double currentValue = parseValue(holding);
            totalValue += currentValue;

            if ("ETF".equals(sector)) {
                List<String> constituents = etfConstituents.getOrDefault(ticker, List.of());
                processETFHolding(currentValue, constituents, metadata,
                        sectorMap, countryMap, regionMap);
            } else {
                Map<String, String> meta = metadata.getOrDefault(ticker, Map.of());
                addToMap(sectorMap, meta.getOrDefault(FIELD_SECTOR, FIELD_UNKNOWN), currentValue);
                addToMap(countryMap, meta.getOrDefault("country", FIELD_UNKNOWN), currentValue);
                addToMap(regionMap, meta.getOrDefault("region", FIELD_UNKNOWN), currentValue);
            }
        }

        List<Map<String, Object>> sectors = buildBreakdown(sectorMap, totalValue);
        List<Map<String, Object>> countries = buildBreakdown(countryMap, totalValue);
        List<Map<String, Object>> regions = buildBreakdown(regionMap, totalValue);

        Map<String, Object> result = new HashMap<>();
        result.put("sectors", sectors);
        result.put("countries", countries);
        result.put("regions", regions);
        result.put("sectorChartLabels", chartLabels(sectors));
        result.put("sectorChartValues", chartValues(sectors));
        result.put("countryChartLabels", chartLabels(countries));
        result.put("countryChartValues", chartValues(countries));
        result.put("regionChartLabels", chartLabels(regions));
        result.put("regionChartValues", chartValues(regions));
        result.put("totalValue", String.format("%,.2f", totalValue));
        return result;
    }

    private double parseValue(Map<String, Object> holding) {
        try {
            return Double.parseDouble((String) holding.get("currentValue"));
        } catch (Exception e) {
            logger.warn("Could not parse currentValue for {}", holding.get(FIELD_TICKER));
            return 0.0;
        }
    }

    private void addToMap(Map<String, Double> map, String key, double value) {
        map.merge(key, value, Double::sum);
    }

    private List<Map<String, Object>> buildBreakdown(Map<String, Double> valueMap, double total) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(valueMap.entrySet());
        entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        List<Map<String, Object>> breakdown = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Double> entry : entries) {
            double pct = total > 0 ? (entry.getValue() / total) * 100 : 0.0;
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", String.format("%.2f", entry.getValue()));
            item.put("percentage", String.format("%.1f", pct));
            item.put("color", pct < 2.0 ? "other" : String.valueOf(colorIdx++));
            breakdown.add(item);
        }
        return breakdown;
    }

    private String chartLabels(List<Map<String, Object>> breakdown) {
        return breakdown.stream()
                .map(m -> "\"" + m.get("name") + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String chartValues(List<Map<String, Object>> breakdown) {
        return breakdown.stream()
                .map(m -> (String) m.get("value"))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private void processETFHolding(double currentValue,
                                   List<String> constituents,
                                   Map<String, Map<String, String>> metadata,
                                   Map<String, Double> sectorMap,
                                   Map<String, Double> countryMap,
                                   Map<String, Double> regionMap) {
        if (constituents.isEmpty()) {
            addToMap(sectorMap, FIELD_UNKNOWN, currentValue);
            addToMap(countryMap, FIELD_UNKNOWN, currentValue);
            addToMap(regionMap, FIELD_UNKNOWN, currentValue);
            return;
        }
        double valuePerConstituent = currentValue / constituents.size();
        for (String constituent : constituents) {
            Map<String, String> meta = metadata.getOrDefault(constituent, Map.of());
            addToMap(sectorMap, meta.getOrDefault(FIELD_SECTOR, FIELD_UNKNOWN), valuePerConstituent);
            addToMap(countryMap, meta.getOrDefault("country", FIELD_UNKNOWN), valuePerConstituent);
            addToMap(regionMap, meta.getOrDefault("region", FIELD_UNKNOWN), valuePerConstituent);
        }
    }
}