package uk.co.asepstrath.bank.example;

import io.jooby.test.MockRouter;
import uk.co.asepstrath.bank.App;
import io.jooby.StatusCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitTest {
    @Test
    public void welcome() {
        MockRouter router = new MockRouter(new App());
        router.get("/example", rsp -> {
            assertEquals("Welcome to Jooby!", rsp.value());
            assertEquals(StatusCode.OK, rsp.getStatusCode());
        });
    }
}
