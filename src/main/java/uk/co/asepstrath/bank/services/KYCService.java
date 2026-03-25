package uk.co.asepstrath.bank.services;

import com.google.gson.*;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;

import java.util.*;

import static uk.co.asepstrath.bank.Constants.*;

public class KYCService {

    private static final String FIELD_IS_STRING = "isString";
    private static final String FIELD_VALID_OPTIONS = "validOptions";
    private static final String FIELD_CHALLENGE_TOKEN = "challengeToken";
    private static final String FIELD_ANSWER_TYPE = "answerType";
    private static final String FIELD_PLACEHOLDER = "placeholder";
    private static final String FIELD_IS_DATE = "isDate";

    private final Logger logger;

    public KYCService(Logger logger) {
        this.logger = logger;
    }

    public Map<String, Object> fetchQuestionnaire() {
        try {
            HttpResponse<String> response = Unirest.get(KYC_API_URL + "/questions")
                    .header("Authorization", KYC_BEARER_TOKEN)
                    .asString();

            if (!response.isSuccess()) {
                logger.error("Failed to fetch KYC questions: HTTP {}", response.getStatus());
                return new HashMap<>();
            }

            JsonObject json = new Gson().fromJson(response.getBody(), JsonObject.class);
            List<Map<String, Object>> questions = new ArrayList<>();

            for (JsonElement el : json.getAsJsonArray("questions")) {
                parseQuestion(el.getAsJsonObject(), questions);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("questions", questions);
            result.put(FIELD_CHALLENGE_TOKEN, json.get(FIELD_CHALLENGE_TOKEN).getAsString());
            logger.info("Fetched {} KYC questions", questions.size());
            return result;

        } catch (Exception e) {
            logger.error("Error fetching KYC questionnaire", e);
            return new HashMap<>();
        }
    }

    private void parseQuestion(JsonObject q, List<Map<String, Object>> questions) {
        try {
            String id = q.get("id").getAsString();
            String answerType = q.get(FIELD_ANSWER_TYPE).getAsString();
            logger.info("Parsing question: id={} type={}", id, answerType);

            Map<String, Object> question = new HashMap<>();
            question.put("id", id);
            question.put("questionText", q.get("questionText").getAsString());
            question.put(FIELD_ANSWER_TYPE, answerType);
            question.put("isEnum", false);
            question.put(FIELD_IS_DATE, false);
            question.put("isNumber", false);
            question.put("isBoolean", false);
            question.put(FIELD_IS_STRING, true);

            if ("BOOLEAN".equals(answerType)) {
                applyBoolean(question);
            } else if ("DATE".equals(answerType)) {
                question.put(FIELD_IS_DATE, true);
                question.put(FIELD_IS_STRING, false);
            } else if (q.has(FIELD_VALID_OPTIONS) && !q.get(FIELD_VALID_OPTIONS).isJsonNull()) {
                applyValidOptions(answerType, q.get(FIELD_VALID_OPTIONS), question);
            }

            if (Boolean.TRUE.equals(question.get(FIELD_IS_STRING))) {
                question.put(FIELD_PLACEHOLDER, getPlaceholder(id));
            }

            questions.add(question);
        } catch (Exception e) {
            logger.error("Failed to parse question: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void applyBoolean(Map<String, Object> question) {
        List<Map<String, String>> boolOptions = new ArrayList<>();
        Map<String, String> yes = new HashMap<>();
        yes.put("label", "Yes");
        yes.put("value", "true");
        Map<String, String> no = new HashMap<>();
        no.put("label", "No");
        no.put("value", "false");
        boolOptions.add(yes);
        boolOptions.add(no);
        question.put("boolOptions", boolOptions);
        question.put("isBoolean", true);
        question.put(FIELD_IS_STRING, false);
    }

    private void applyValidOptions(String answerType, JsonElement opts, Map<String, Object> question) {
        switch (answerType) {
            case "ENUM":
                List<String> options = new ArrayList<>();
                for (JsonElement opt : opts.getAsJsonArray()) {
                    options.add(opt.getAsString());
                }
                question.put(FIELD_VALID_OPTIONS, options);
                question.put("isEnum", true);
                question.put(FIELD_IS_STRING, false);
                break;

            case "DATE":
                JsonObject dateRange = opts.getAsJsonObject();
                long minTs = dateRange.get("min").getAsLong();
                long maxTs = dateRange.get("max").getAsLong();
                question.put("minDate", java.time.Instant.ofEpochSecond(minTs)
                        .atZone(java.time.ZoneId.of("UTC"))
                        .toLocalDate().toString());
                question.put("maxDate", java.time.Instant.ofEpochSecond(maxTs)
                        .atZone(java.time.ZoneId.of("UTC"))
                        .toLocalDate().toString());
                question.put(FIELD_IS_DATE, true);
                question.put(FIELD_IS_STRING, false);
                break;

            case "NUMBER", "INTEGER":
                if (opts.isJsonObject()) {
                    JsonObject numRange = opts.getAsJsonObject();
                    if (numRange.has("min")) question.put("min", numRange.get("min").getAsString());
                    if (numRange.has("max")) question.put("max", numRange.get("max").getAsString());
                }
                question.put("isNumber", true);
                question.put(FIELD_IS_STRING, false);
                break;

            case "STRING", "TEXT":
                if (opts.isJsonPrimitive()) {
                    question.put("pattern", opts.getAsString());
                }
                question.put(FIELD_PLACEHOLDER, getPlaceholder(
                        (String) question.get("id")));
                break;

            default:
                logger.warn("Unknown answer type: {}", answerType);
                break;
        }
    }

    public boolean submitResponses(Map<String, String> answers, String challengeToken) {
        try {
            JsonObject body = new JsonObject();
            JsonArray answersArray = new JsonArray();

            for (Map.Entry<String, String> entry : answers.entrySet()) {
                JsonObject answer = new JsonObject();
                answer.addProperty("questionId", entry.getKey());
                answer.addProperty("answer", entry.getValue());
                answersArray.add(answer);
            }

            body.add("answers", answersArray);
            body.addProperty(FIELD_CHALLENGE_TOKEN, challengeToken);

            HttpResponse<String> response = Unirest.post(KYC_API_URL + "/submit")
                    .header("Authorization", KYC_BEARER_TOKEN)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .asString();

            if (!response.isSuccess()) {
                logger.error("KYC submission failed: HTTP {}", response.getStatus());
                return false;
            }

            JsonObject result = new Gson().fromJson(response.getBody(), JsonObject.class);
            return result.has("approved") && result.get("approved").getAsBoolean();

        } catch (Exception e) {
            logger.error("Error submitting KYC responses", e);
            return false;
        }
    }

    private String getPlaceholder(String questionId) {
        return switch (questionId) {
            case "NATIONAL_INSURANCE_NUMBER" -> "e.g. AB123456C";
            case "UK_MOBILE_PHONE_NUMBER" -> "e.g. 07745295112";
            case "FULL_LEGAL_NAME" -> "e.g. Martin Goodfellow";
            case "COUNTRY_OF_TAX_RESIDENCE" -> "e.g. United Kingdom";
            case "email" -> "e.g. martin@example.co.uk";
            default -> "Your answer...";
        };
    }
}
