package uk.co.asepstrath.bank;

import io.jooby.test.JoobyTest;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@JoobyTest(App.class)
class LoginControllerTest {

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

    @Test
    void register_emptyFields_redirectsSomewhere(int serverPort) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("name", "")
                .add("password", "")
                .build();

        Request req = new Request.Builder()
                .url("http://localhost:" + serverPort + "/login/register")
                .post(form)
                .build();

        try (Response rsp = client.newCall(req).execute()) {
            assertEquals(302, rsp.code());

            String location = rsp.header("Location");
            System.out.println("Redirect location: " + location);

            assertNotNull(location);
            assertTrue(
                location.contains("login") ||
                location.contains("register")
            );
        }
    }
}