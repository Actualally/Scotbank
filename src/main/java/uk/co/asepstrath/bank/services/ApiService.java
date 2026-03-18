package uk.co.asepstrath.bank.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ApiService {

    private static final String BASE_URL = "https://api.asep-strath.co.uk";

    // JSON field constants
    private static final String FIELD_TICKER = "ticker";
    private static final String FIELD_TOTAL_CASH_AMOUNT = "totalCashAmount";
    private static final String FIELD_NEXT_TOKEN = "nextToken";
    private static final String FIELD_CONSTITUENTS = "constituents";
    private static final String FIELD_INCEPTION_DATE = "inceptionDate";
    private static final String FIELD_TIMESTAMP = "timestamp";

    private final Gson gson;
    private final Logger logger;

    public ApiService(Logger logger) {
        this.gson = new Gson();
        this.logger = logger;
    }

    public List<Investor> fetchInvestors() {
        List<Investor> investors = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/investors").asString();
            if (!response.isSuccess()) {
                logger.error("Failed to fetch investors: HTTP {}", response.getStatus());
                return investors;
            }
            JsonArray array = gson.fromJson(response.getBody(), JsonObject.class)
                    .getAsJsonArray("investors");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                investors.add(new Investor(
                        UUID.fromString(obj.get("id").getAsString()),
                        obj.get("name").getAsString(),
                        obj.get("cashBalance").getAsDouble()
                ));
            }
            logger.info("Fetched {} investors from API", investors.size());
        } catch (Exception e) {
            logger.error("Error fetching investors", e);
        }
        return investors;
    }

    public Investor fetchInvestorDetails(UUID investorId) {
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/investors/" + investorId).asString();
            if (!response.isSuccess()) {
                logger.error("Failed to fetch investor {}: HTTP {}", investorId, response.getStatus());
                return null;
            }
            JsonObject data = gson.fromJson(response.getBody(), JsonObject.class).getAsJsonObject("data");
            Investor inv = new Investor(
                    UUID.fromString(data.get("id").getAsString()),
                    data.get("name").getAsString(),
                    data.get("cashBalance").getAsDouble()
            );
            inv.setEmail(data.get("email").getAsString());
            inv.setPhone(data.get("phone").getAsString());
            inv.setAddress(data.get("address").getAsString());
            inv.setTaxId(data.get("taxId").getAsString());
            logger.info("Fetched details for investor {}", investorId);
            return inv;
        } catch (Exception e) {
            logger.error("Error fetching investor details for {}", investorId, e);
            return null;
        }
    }

    public List<Transaction> fetchAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String nextToken = null;
        try {
            do {
                String url = BASE_URL + "/api/transactions";
                if (nextToken != null) url += "?" + FIELD_NEXT_TOKEN + "=" + nextToken;

                HttpResponse<String> response = Unirest.get(url).asString();
                if (!response.isSuccess()) {
                    logger.error("Failed to fetch transactions: HTTP {}", response.getStatus());
                    break;
                }

                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
                for (JsonElement element : json.getAsJsonArray("transactions")) {
                    parseTransaction(element.getAsJsonObject(), transactions);
                }

                nextToken = extractNextToken(json);
            } while (nextToken != null);

            logger.info("Fetched {} transactions from API", transactions.size());
        } catch (Exception e) {
            logger.error("Error fetching transactions", e);
        }
        return transactions;
    }

    private void parseTransaction(JsonObject obj, List<Transaction> transactions) {
        try {
            String id = getStringOrNull(obj, "id");
            String dateStr = getStringOrNull(obj, "date");
            if (dateStr == null) dateStr = getStringOrNull(obj, FIELD_TIMESTAMP);
            String type = getStringOrNull(obj, "type");
            String investorIdStr = getStringOrNull(obj, "investorId");
            String ticker = getStringOrNull(obj, FIELD_TICKER);
            double amount = obj.has(FIELD_TOTAL_CASH_AMOUNT) && !obj.get(FIELD_TOTAL_CASH_AMOUNT).isJsonNull()
                    ? obj.get(FIELD_TOTAL_CASH_AMOUNT).getAsDouble() : 0.0;

            if (id == null || dateStr == null || type == null || investorIdStr == null) {
                logger.warn("Skipping transaction with null required field: id={}, date={}, type={}, investorId={}",
                        id, dateStr, type, investorIdStr);
                return;
            }

            LocalDate date = LocalDate.parse(dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr);
            transactions.add(new Transaction(UUID.fromString(id), date, type,
                    UUID.fromString(investorIdStr), ticker, amount));
        } catch (Exception e) {
            logger.warn("Skipping malformed transaction: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String extractNextToken(JsonObject json) {
        JsonObject pagination = json.getAsJsonObject("pagination");
        if (pagination != null && pagination.has(FIELD_NEXT_TOKEN)
                && !pagination.get(FIELD_NEXT_TOKEN).isJsonNull()) {
            return pagination.get(FIELD_NEXT_TOKEN).getAsString();
        }
        return null;
    }

    public List<Equity> fetchEquities() {
        List<Equity> equities = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/equities").asString();
            if (!response.isSuccess()) {
                logger.error("Failed to fetch equities: HTTP {}", response.getStatus());
                return equities;
            }
            JsonArray array = gson.fromJson(response.getBody(), JsonObject.class)
                    .getAsJsonArray("assets");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                equities.add(new Equity(
                        obj.get("name").getAsString(),
                        obj.get(FIELD_TICKER).getAsString(),
                        obj.get("sector").getAsString()
                ));
            }
            logger.info("Fetched {} equities from API", equities.size());
        } catch (Exception e) {
            logger.error("Error fetching equities", e);
        }
        return equities;
    }

    public List<ETF> fetchETFs() {
        List<ETF> etfs = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/etfs").asString();
            if (!response.isSuccess()) {
                logger.error("Failed to fetch ETFs: HTTP {}", response.getStatus());
                return etfs;
            }
            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            JsonArray array = json.getAsJsonArray("etfs");
            if (array == null) {
                logger.warn("ETF response has no 'etfs' array. Keys: {}", json.keySet());
                return etfs;
            }
            for (JsonElement element : array) {
                parseETF(element.getAsJsonObject(), etfs);
            }
            logger.info("Fetched {} ETFs from API", etfs.size());
        } catch (Exception e) {
            logger.error("Error fetching ETFs", e);
        }
        return etfs;
    }

    private void parseETF(JsonObject obj, List<ETF> etfs) {
        try {
            etfs.add(new ETF(
                    getStringOrNull(obj, "name"),
                    getStringOrNull(obj, FIELD_TICKER),
                    getStringOrNull(obj, "description"),
                    obj.has(FIELD_INCEPTION_DATE) && !obj.get(FIELD_INCEPTION_DATE).isJsonNull()
                            ? LocalDate.parse(obj.get(FIELD_INCEPTION_DATE).getAsString()) : null,
                    parseConstituents(obj)
            ));
        } catch (Exception e) {
            logger.warn("Skipping malformed ETF: {}", e.getMessage());
        }
    }

    private List<String> parseConstituents(JsonObject obj) {
        List<String> constituents = new ArrayList<>();
        if (!obj.has(FIELD_CONSTITUENTS) || obj.get(FIELD_CONSTITUENTS).isJsonNull()) {
            return constituents;
        }
        for (JsonElement c : obj.getAsJsonArray(FIELD_CONSTITUENTS)) {
            if (c.isJsonObject()) {
                JsonObject constObj = c.getAsJsonObject();
                if (constObj.has(FIELD_TICKER)) constituents.add(constObj.get(FIELD_TICKER).getAsString());
            } else {
                constituents.add(c.getAsString());
            }
        }
        return constituents;
    }

    //making cache for api fetch so dont have to fetch 1 billion things
    private final Map<String, List<PricePoint>> priceCache = new HashMap<>();

    public List<PricePoint> fetchPrices(String symbol) {
        if (priceCache.containsKey(symbol)) return priceCache.get(symbol);

        List<PricePoint> prices = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/prices/" + symbol).asString();
            if (!response.isSuccess()) {
                logger.error("Failed to fetch prices for {}: HTTP {}", symbol, response.getStatus());
                return prices;
            }
            JsonArray array = gson.fromJson(response.getBody(), JsonObject.class)
                    .getAsJsonArray("prices");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                prices.add(new PricePoint(
                        LocalDateTime.parse(obj.get(FIELD_TIMESTAMP).getAsString()),
                        obj.get("price").getAsDouble()
                ));
            }
            logger.info("Fetched {} price points for {}", prices.size(), symbol);
        } catch (Exception e) {
            logger.error("Error fetching prices for {}", symbol, e);
        }

        priceCache.put(symbol, prices);
        return prices;
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    public double fetchLatestPrice(String symbol) {
        List<PricePoint> prices = fetchPrices(symbol);
        if (prices == null || prices.isEmpty()) return 0.0;
        return prices.get(prices.size() - 1).getPrice();
    }

    public double fetchPriceOnDate(String symbol, LocalDate date) {
        if (priceCache.containsKey(symbol)) {
            return findClosestPrice(priceCache.get(symbol), date);
        }

        // Only fetch first page - enough to get opening prices
        List<PricePoint> prices = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/prices/" + symbol).asString();
            if (response.isSuccess()) {
                JsonArray array = gson.fromJson(response.getBody(), JsonObject.class)
                        .getAsJsonArray("prices");
                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    prices.add(new PricePoint(
                            LocalDateTime.parse(obj.get(FIELD_TIMESTAMP).getAsString()),
                            obj.get("price").getAsDouble()
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching price for {}", symbol, e);
        }

        priceCache.put(symbol, prices);
        return findClosestPrice(prices, date);
    }

    private double findClosestPrice(List<PricePoint> prices, LocalDate date) {
        if (prices == null || prices.isEmpty()) return 0.0;
        return prices.stream()
                .min(Comparator.comparingLong(p ->
                        Math.abs(ChronoUnit.DAYS.between(p.getTimestamp().toLocalDate(), date))))
                .map(PricePoint::getPrice)
                .orElse(0.0);
    }
}