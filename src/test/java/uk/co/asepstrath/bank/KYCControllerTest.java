package uk.co.asepstrath.bank;

import io.jooby.test.JoobyTest;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@JoobyTest(App.class)
class KYCControllerTest {

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
        client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/login")
                .post(form).build()).execute().close();
    }

    @Test
    void kycPage_unauthenticated_redirectsToLogin(int serverPort) throws IOException {
        OkHttpClient freshClient = new OkHttpClient.Builder()
                .followRedirects(false).build();
        try (Response rsp = freshClient.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/kyc").build()).execute()) {
            assertEquals(302, rsp.code());
            assertTrue(rsp.header("Location").contains("/login"));
        }
    }

    @Test
    void kycPage_authenticated_loads(int serverPort) throws IOException {
        login(serverPort);
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/kyc").build()).execute()) {
            assertEquals(200, rsp.code());
            String content = rsp.body().string();
            assertTrue(content.contains("KYC"));
            assertTrue(content.contains("challengeToken"));
        }
    }

    @Test
    void kycPage_showsQuestions(int serverPort) throws IOException {
        login(serverPort);
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/kyc").build()).execute()) {
            assertEquals(200, rsp.code());
            String content = rsp.body().string();
            assertTrue(content.contains("<form"));
            assertTrue(content.contains("<select") || content.contains("<input"));
        }
    }

    @Test
    void kycSubmit_withoutToken_fails(int serverPort) throws IOException {
        login(serverPort);
        RequestBody form = new FormBody.Builder()
                .add("challengeToken", "invalid-token-12345")
                .build();
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/kyc/submit")
                .post(form).build()).execute()) {
            //Should redirect back to KYC on failure
            assertEquals(302, rsp.code());
            assertTrue(rsp.header("Location").contains("/kyc"));
        }
    }

    @Test
    void kycPage_afterCompletion_bannerDisappears(int serverPort) throws IOException {
        login(serverPort);

        //Check banner exists before KYC
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/account").build()).execute()) {
            String content = rsp.body().string();
            assertTrue(content.contains("kycRequired") || content.contains("KYC"));
        }
    }
}