package uk.co.asepstrath.bank;

import io.jooby.test.JoobyTest;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

@JoobyTest(App.class)
class IntegrationTest {

    static OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(false)
            .cookieJar(new CookieJar() {
                private java.util.List<Cookie> cookies = new java.util.ArrayList<>();

                @Override
                public void saveFromResponse(HttpUrl url, java.util.List<Cookie> newCookies) {
                    cookies.clear();
                    cookies.addAll(newCookies);
                }

                @Override
                public java.util.List<Cookie> loadForRequest(HttpUrl url) {
                    return cookies;
                }
            })
            .build();

    private void login(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("accountid", "investor-001")
                .add("password", "password")
                .build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/login")
                .post(form)
                .build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
        }
    }

    @Test void accountPage_showsDemoInvestor(int serverPort) throws IOException {
        login(serverPort);
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account").build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(200, rsp.code());
            ResponseBody body = rsp.body();
            assertNotNull(body);
            assertTrue(body.string().contains("Demo Investor"));
        }
    }

    @Test void depositPage_loads(int serverPort) throws IOException {
        login(serverPort);
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

    @Test void deposit_validAmount_redirectsToAccount(int serverPort) throws IOException {
        login(serverPort);
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
        login(serverPort);
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

    @Test void withdrawal_page_loads(int serverPort) throws IOException {
        login(serverPort);
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/withdraw").build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(200, rsp.code());
            ResponseBody body = rsp.body();
            assertNotNull(body);
            String content = body.string();
            assertTrue(content.contains("Withdraw Money"));
            assertTrue(content.contains("withdrawamount"));
        }
    }

    @Test void withdraw_valid_amount_redirectsToAccount(int serverPort) throws IOException {
        login(serverPort);
        RequestBody form = new FormBody.Builder().add("withdrawamount", "50.00").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/withdraw/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            String location = rsp.header("Location");
            assertNotNull(location);
            assertTrue(location.contains("/account"));
        }
    }

    @Test void withdraw_negative_amount_redirectsToWithdraw(int serverPort) throws IOException {
        login(serverPort);
        RequestBody form = new FormBody.Builder().add("withdrawamount", "-10.00").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/withdraw/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            String location = rsp.header("Location");
            assertNotNull(location);
            assertTrue(location.contains("/withdraw"));
        }
    }

    @Test void withdraw_more_than_balance_redirectsToWithdraw(int serverPort) throws IOException {
        login(serverPort);
        RequestBody form = new FormBody.Builder().add("withdrawamount", "100000.00").build();
        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/withdraw/process").post(form).build();
        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());
            String location = rsp.header("Location");
            assertNotNull(location);
            assertTrue(location.contains("/withdraw"));
        }
    }
}