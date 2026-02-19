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

    @Test void depositPage_loads(int serverPort) throws IOException {
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/deposit").build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(200, rsp.code());
            ResponseBody body = rsp.body();
            assertNotNull(body);
            String content = body.string();
            assertTrue(content.contains("Deposit Money"));
            assertTrue(content.contains("depositamount"));
        }
    }

    @Test void accountPage_showsDemoInvestor(int serverPort) throws IOException {
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account").build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(200, rsp.code());
            ResponseBody body = rsp.body();
            assertNotNull(body);
            assertTrue(body.string().contains("Demo Investor"));
        }
    }

    @Test void deposit_validAmount_redirectsToAccount(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder().add("depositamount", "50.00").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/deposit/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            String location = rsp.header("Location");
            assertNotNull(location);
            assertTrue(location.contains("/account"));
        }
    }

    @Test void deposit_negativeAmount_redirectsToDeposit(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder().add("depositamount", "-10").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/deposit/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            String location = rsp.header("Location");
            assertNotNull(location);
            assertTrue(location.contains("/deposit"));
        }
    }
}