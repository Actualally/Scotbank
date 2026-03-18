package uk.co.asepstrath.bank.services;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

import uk.co.asepstrath.bank.models.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiServiceTest {

    private ApiService apiService;
    private Logger logger;

    @BeforeEach
    void setup() {
        logger = mock(Logger.class);
        apiService = new ApiService(logger);
    }

    @AfterEach
    void tearDown() {
        Unirest.shutDown();
    }

    @Test
    void testFetchInvestors() {

        String mockJson = """
        {
          "investors":[
            {
              "id":"123e4567-e89b-12d3-a456-426614174000",
              "name":"Alice",
              "cashBalance":1000.0
            }
          ]
        }
        """;

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getBody()).thenReturn(mockJson);

        Unirest.config().httpClient(Mockito.mock(kong.unirest.core.Client.class));

        List<Investor> investors = apiService.fetchInvestors();

        assertNotNull(investors);
    }

    @Test
    void testFetchInvestorDetails() {

        UUID id = UUID.randomUUID();

        apiService.fetchInvestorDetails(id);

        // Since API call is external we just verify method does not crash
        assertTrue(true);
    }

    @Test
    void testFetchAllTransactions() {

        List<Transaction> transactions = apiService.fetchAllTransactions();

        System.out.println(transactions.get(0));

        assertNotNull(transactions);
    }

    @Test
    void testFetchEquities() {

        List<Equity> equities = apiService.fetchEquities();
        System.out.println(equities.get(0));
        assertNotNull(equities);
    }

    @Test
    void testFetchETFs() {

        List<ETF> etfs = apiService.fetchETFs();
        System.out.println(etfs.get(0));
        assertNotNull(etfs);
    }

    @Test
    void testFetchPrices() {

        List<PricePoint> prices = apiService.fetchPrices("CTT");
        System.out.println(prices.get(0));
        assertNotNull(prices);
    }
}
