package uk.co.asepstrath.bank;

import io.jooby.test.JoobyTest;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@JoobyTest(App.class)
class CapitalGainsTest {

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

    private void loginAsCarmen(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("accountid", "00000000-0000-0003-aba9-b4f609570ca0")
                .add("password", "password")
                .build();
        client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/login")
                .post(form).build()).execute().close();
    }

    @Test
    void portfolioPage_showsCapitalGainsSection(int serverPort) throws IOException {
        loginAsCarmen(serverPort);
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/portfolio").build()).execute()) {
            assertEquals(200, rsp.code());
            String content = rsp.body().string();
            assertTrue(content.contains("Capital Gains"));
        }
    }

    @Test
    void portfolioPage_showsYearSelector(int serverPort) throws IOException {
        loginAsCarmen(serverPort);
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/portfolio").build()).execute()) {
            assertEquals(200, rsp.code());
            String content = rsp.body().string();
            assertTrue(content.contains("name=\"year\""));
        }
    }

    @Test
    void portfolioPage_defaultYear_isCurrentYear(int serverPort) throws IOException {
        loginAsCarmen(serverPort);
        int currentYear = java.time.LocalDate.now().getYear();
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/portfolio").build()).execute()) {
            assertEquals(200, rsp.code());
            assertTrue(rsp.body().string().contains("Capital Gains — " + currentYear));
        }
    }

    @Test
    void portfolioPage_carmen2025_hasSalesData(int serverPort) throws IOException {
        loginAsCarmen(serverPort);
        try (Response rsp = client.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/portfolio?year=2025").build()).execute()) {
            assertEquals(200, rsp.code());
            String content = rsp.body().string();
            assertTrue(content.contains("Total Gains"));
            assertTrue(content.contains("Total Losses"));
            assertTrue(content.contains("Estimated Tax Owed"));
            assertTrue(content.contains("MOG"));
        }
    }

    @Test
    void portfolioPage_unauthenticated_redirectsToLogin(int serverPort) throws IOException {
        OkHttpClient freshClient = new OkHttpClient.Builder()
                .followRedirects(false).build();
        try (Response rsp = freshClient.newCall(new Request.Builder()
                .url("http://localhost:" + serverPort + "/account/portfolio").build()).execute()) {
            assertEquals(302, rsp.code());
            assertTrue(rsp.header("Location").contains("/login"));
        }
    }
}