package uk.co.asepstrath.bank;

import io.jooby.test.JoobyTest;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

@JoobyTest(App.class)
public class IntegrationTest {

    static OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(false).build();

    @Test public void depositPage_loads(int serverPort) throws IOException {
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/deposit").build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(200, rsp.code());
            String body = rsp.body().string();
            assertTrue(body.contains("Deposit Money"));
            assertTrue(body.contains("depositamount"));
        }
    }

    @Test public void accountPage_showsDemoInvestor(int serverPort) throws IOException {
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account").build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(200, rsp.code());
            assertTrue(rsp.body().string().contains("Demo Investor"));
        }
    }

    @Test public void deposit_validAmount_redirectsToAccount(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder().add("depositamount", "50.00").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/deposit/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            assertTrue(rsp.header("Location").contains("/account"));
        }
    }

    @Test public void deposit_negativeAmount_redirectsToDeposit(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder().add("depositamount", "-10").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/deposit/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            assertTrue(rsp.header("Location").contains("/deposit"));
        }
    }
}
