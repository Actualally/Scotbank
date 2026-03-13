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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApiService {

    private static final String BASE_URL = "https://api.asep-strath.co.uk";
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

            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            JsonArray array = json.getAsJsonArray("investors");

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                Investor inv = new Investor(
                        UUID.fromString(obj.get("id").getAsString()),
                        obj.get("name").getAsString(),
                        obj.get("cashBalance").getAsDouble()
                );
                investors.add(inv);
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

            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            JsonObject data = json.getAsJsonObject("data");

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
                if (nextToken != null) {
                    url += "?nextToken=" + nextToken;
                }

                HttpResponse<String> response = Unirest.get(url).asString();

                if (!response.isSuccess()) {
                    logger.error("Failed to fetch transactions: HTTP {}", response.getStatus());
                    break;
                }

                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
                JsonArray array = json.getAsJsonArray("transactions");

                for (JsonElement element : array) {
                    try {
                        JsonObject obj = element.getAsJsonObject();

                        String id = getStringOrNull(obj, "id");
                        String dateStr = getStringOrNull(obj, "date");
                        if (dateStr == null) {
                            dateStr = getStringOrNull(obj, "timestamp");
                        }
                        String type = getStringOrNull(obj, "type");
                        String investorIdStr = getStringOrNull(obj, "investorId");
                        String ticker = getStringOrNull(obj, "ticker");
                        double amount = obj.has("totalCashAmount") && !obj.get("totalCashAmount").isJsonNull()
                                ? obj.get("totalCashAmount").getAsDouble() : 0.0;

                        if (id == null || dateStr == null || type == null || investorIdStr == null) {
                            logger.warn("Skipping transaction with null required field: id={}, date={}, type={}, investorId={}",
                                    id, dateStr, type, investorIdStr);
                            continue;
                        }

                        // Handle date - trim to just the date part if it includes time
                        LocalDate date = LocalDate.parse(dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr);

                        Transaction t = new Transaction(
                                UUID.fromString(id),
                                date,
                                type,
                                UUID.fromString(investorIdStr),
                                ticker,
                                amount
                        );
                        transactions.add(t);
                    } catch (Exception e) {
                        logger.warn("Skipping malformed transaction: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }
                }

                // Get next page token
                JsonObject pagination = json.getAsJsonObject("pagination");
                if (pagination != null && pagination.has("nextToken") && !pagination.get("nextToken").isJsonNull()) {
                    nextToken = pagination.get("nextToken").getAsString();
                } else {
                    nextToken = null;
                }

            } while (nextToken != null);

            logger.info("Fetched {} transactions from API", transactions.size());
        } catch (Exception e) {
            logger.error("Error fetching transactions", e);
        }
        return transactions;
    }

    public List<Equity> fetchEquities() {
        List<Equity> equities = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/equities").asString();

            if (!response.isSuccess()) {
                logger.error("Failed to fetch equities: HTTP {}", response.getStatus());
                return equities;
            }

            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            JsonArray array = json.getAsJsonArray("assets");

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                Equity eq = new Equity(
                        obj.get("name").getAsString(),
                        obj.get("ticker").getAsString(),
                        obj.get("sector").getAsString()
                );
                equities.add(eq);
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

            String body = response.getBody();

            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray array = json.getAsJsonArray("etfs");

            if (array == null) {
                logger.warn("ETF response has no 'etfs' array. Keys: {}", json.keySet());
                return etfs;
            }

            for (JsonElement element : array) {
                try {
                    JsonObject obj = element.getAsJsonObject();

                    List<String> constituents = new ArrayList<>();
                    if (obj.has("constituents") && !obj.get("constituents").isJsonNull()) {
                        for (JsonElement c : obj.getAsJsonArray("constituents")) {
                            if (c.isJsonObject()) {
                                JsonObject constObj = c.getAsJsonObject();
                                if (constObj.has("ticker")) {
                                    constituents.add(constObj.get("ticker").getAsString());
                                }
                            } else {
                                constituents.add(c.getAsString());
                            }
                        }
                    }

                    ETF etf = new ETF(
                            getStringOrNull(obj, "name"),
                            getStringOrNull(obj, "ticker"),
                            getStringOrNull(obj, "description"),
                            obj.has("inceptionDate") && !obj.get("inceptionDate").isJsonNull()
                                    ? LocalDate.parse(obj.get("inceptionDate").getAsString()) : null,
                            constituents
                    );
                    etfs.add(etf);
                } catch (Exception e) {
                    logger.warn("Skipping malformed ETF: {}", e.getMessage());
                }
            }

            logger.info("Fetched {} ETFs from API", etfs.size());
        } catch (Exception e) {
            logger.error("Error fetching ETFs", e);
        }
        return etfs;
    }

    public List<PricePoint> fetchPrices(String symbol) {
        List<PricePoint> prices = new ArrayList<>();
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "/api/prices/" + symbol).asString();

            if (!response.isSuccess()) {
                logger.error("Failed to fetch prices for {}: HTTP {}", symbol, response.getStatus());
                return prices;
            }

            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            JsonArray array = json.getAsJsonArray("prices");

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                PricePoint pp = new PricePoint(
                        LocalDateTime.parse(obj.get("timestamp").getAsString()),
                        obj.get("price").getAsDouble()
                );
                prices.add(pp);
            }

            logger.info("Fetched {} price points for {}", prices.size(), symbol);
        } catch (Exception e) {
            logger.error("Error fetching prices for {}", symbol, e);
        }
        return prices;
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

}