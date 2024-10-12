/*

Disclaimer:

This open-source software is provided "as-is," without any express or implied warranties or guarantees. Neither the author(s) nor Microsoft Corporation make any representations or warranties, express or implied, regarding the accuracy, completeness, or performance of the software.

By using this software, you acknowledge and agree that the author(s) and Microsoft Corporation are not liable for any damages, losses, or issues that may arise from its use, including but not limited to direct, indirect, incidental, consequential, or punitive damages, regardless of the legal theory under which such claims arise.

This software is made available for free and open use, and it is your responsibility to test, evaluate, and use it at your own risk. The author(s) and Microsoft Corporation have no obligation to provide maintenance, support, updates, enhancements, or modifications to the software.

*/
package redisapitest;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;


public class App {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String BASE_URL = "http://localhost:8080/api/retry/redis";
    private static final Set<String> writtenKeys = new HashSet<>();
    private static final Random random = new Random();

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10);  // Pool for concurrent requests
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        // Delete all keys before starting
        deleteAllKeys().join();

        // Writing to Redis in parallel
        IntStream.range(0, 10).forEach(i -> {
            String key = "redis_api_test_key_" + i;
            writtenKeys.add(key);

            CompletableFuture<Void> future = writeDataToApi(key, "Test value for key " + key)
                    .thenRun(() -> {
                        log("Write " + i + ": Completed for key " + key);
                        synchronized (writtenKeys) {
                            writtenKeys.add(key);
                        }
                    });

            futures.add(future);
        });

        // Wait for all writes to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        futures.clear();

        // Reading from Redis in parallel
        Set<CompletableFuture<Void>> readFutures = new HashSet<>();

        IntStream.range(0, 200).forEach(i -> {
            CompletableFuture<Void> future;
            synchronized (writtenKeys) {
                if (!writtenKeys.isEmpty()) {
                    String randomKey = writtenKeys.stream()
                            .skip(random.nextInt(writtenKeys.size()))
                            .findFirst()
                            .orElse(null);

                    if (randomKey != null) {
                        future = readDataFromApi(randomKey)
                                .thenAccept(response -> {
                                    log("Read " + i + ": " + response.body());
                                });
                    } else {
                        log("Skipping read " + i + " as no keys have been written yet.");
                        future = CompletableFuture.completedFuture(null);
                    }
                } else {
                    log("Skipping read " + i + " as no keys have been written yet.");
                    future = CompletableFuture.completedFuture(null);
                }
            }
            readFutures.add(future);

            try {
                Thread.sleep(2000);  // Simulate 2 seconds delay between reads
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logError("Thread was interrupted.");
            }
        });

        CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

         // Delete all keys after finishing
        deleteAllKeys().join();

        log("All operations completed.");
    }

   private static CompletableFuture<Void> writeDataToApi(String key, String value) {
        try {
            // Encode the key and value for the URL
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);

            String url = BASE_URL + "/write?key=" + encodedKey + "&value=" + encodedValue;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            System.out.println("Successfully wrote key: " + key);
                        } else {
                            System.err.println("Failed to write key: " + key + ", Status Code: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompletableFuture<HttpResponse<String>> readDataFromApi(String key) {
        String url = BASE_URL + "/read?key=" + key;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        log("Successfully read key: " + key);
                    } else {
                        logError("Failed to read key: " + key + ", Status Code: " + response.statusCode());
                    }
                    return response;
                });
    }

    private static CompletableFuture<Void> deleteAllKeys() {
        String url = BASE_URL + "/deleteAll";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        log("All keys deleted.");
                    } else {
                        logError("Failed to delete all keys, Status Code: " + response.statusCode());
                    }
                });
    }

    private static void log(String message) {
        System.out.println("[INFO] " + message);
    }

    private static void logError(String message) {
        System.err.println("[ERROR] " + message);
    }
}
