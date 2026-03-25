package uk.co.asepstrath.bank.controllers;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import uk.co.asepstrath.bank.services.KYCService;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static uk.co.asepstrath.bank.Constants.*;

@Path(ROUTE_KYC)
public class KYCController {

    private static final String CHALLENGE_TOKEN = "challengeToken";
    private final KYCService kycService;
    private final Logger logger;

    public KYCController(KYCService kycService, Logger logger) {
        this.kycService = kycService;
        this.logger = logger;
    }

    private String getAccountID(Context ctx) {
        var session = ctx.sessionOrNull();
        if (session == null || !session.get(SESSION_ACCOUNT_ID).isPresent()) {
            ctx.sendRedirect(ROUTE_LOGIN);
            return null;
        }
        return session.get(SESSION_ACCOUNT_ID).value();
    }

    @GET
    public ModelAndView<Map<String, Object>> showKYCForm(Context ctx) {
        String accountId = getAccountID(ctx);
        if (accountId == null) return new ModelAndView<>(TEMPLATE_KYC, new HashMap<>());

        Map<String, Object> model = new HashMap<>();
        Map<String, Object> questionnaire = kycService.fetchQuestionnaire();

        if (questionnaire == null) {
            model.put(SESSION_ERROR_MESSAGE, "Could not load KYC questionnaire. Please try again.");
            return new ModelAndView<>(TEMPLATE_KYC, model);
        }

        model.put("questions", questionnaire.get("questions"));
        model.put(CHALLENGE_TOKEN, questionnaire.get(CHALLENGE_TOKEN));
        transferFlashMessages(ctx, model);
        return new ModelAndView<>(TEMPLATE_KYC, model);
    }

    @POST("/submit")
    public void submitKYC(Context ctx) {
        String accountId = getAccountID(ctx);
        if (accountId == null) return;

        String challengeToken = ctx.form(CHALLENGE_TOKEN).valueOrNull();
        Map<String, String> answers = new HashMap<>();
        ctx.form().toMap().forEach((key, values) -> {
            if (!key.equals(CHALLENGE_TOKEN)) {
                String value = ctx.form(key).valueOrNull();
                if (value != null) answers.put(key, value);
            }
        });

        logger.info("Submitting KYC with token: {}", challengeToken);
        logger.info("Answers: {}", answers);

        boolean approved = kycService.submitResponses(answers, challengeToken);

        if (approved) {
            ctx.session().put("kyc_completed", "true");
            ctx.session().put(SESSION_SUCCESS_MESSAGE, "KYC questionnaire completed successfully.");
            ctx.sendRedirect(ROUTE_ACCOUNT);
        } else {
            ctx.session().put(SESSION_ERROR_MESSAGE, "Submission failed. Please check your answers and try again.");
            ctx.sendRedirect(ROUTE_KYC);
        }
    }

    private void transferFlashMessages(Context ctx, Map<String, Object> model) {
        var session = ctx.sessionOrNull();
        if (session == null) return;
        for (String key : new String[]{SESSION_SUCCESS_MESSAGE, SESSION_ERROR_MESSAGE}) {
            if (session.get(key).isPresent()) {
                model.put(key, session.get(key).value());
                session.remove(key);
            }
        }
    }
}
