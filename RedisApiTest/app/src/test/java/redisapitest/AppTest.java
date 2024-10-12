
package redisapitest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AppTest {

    private static final String BASE_URL = "http://localhost:8080/api/retry/redis";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void testWriteData() throws Exception {
        String key = "testKey";
        String value = "testValue";

        // Create write request to the API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/write?key=" + key + "&value=" + value))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        // Send request and verify the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Write operation failed");

        // Check that the response body contains confirmation of the write operation
        assertTrue(response.body().contains("Write operation completed"), "Write confirmation missing");
    }

    @Test
    void testReadData() throws Exception {
        String key = "testKey";

        // Create read request to the API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/read?key=" + key))
                .GET()
                .build();

        // Send request and verify the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Read operation failed");

        // Verify that the value returned matches the value we wrote earlier
        assertTrue(response.body().contains("testValue"), "Read value does not match expected value");
    }
}

