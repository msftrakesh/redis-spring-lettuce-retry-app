/*
https://github.com/msftrakesh/redis-lettuce-retry-app

Disclaimer:

This open-source software is provided "as-is," without any express or implied warranties or guarantees. Neither the author(s) nor Microsoft Corporation make any representations or warranties, express or implied, regarding the accuracy, completeness, or performance of the software.

By using this software, you acknowledge and agree that the author(s) and Microsoft Corporation are not liable for any damages, losses, or issues that may arise from its use, including but not limited to direct, indirect, incidental, consequential, or punitive damages, regardless of the legal theory under which such claims arise.

This software is made available for free and open use, and it is your responsibility to test, evaluate, and use it at your own risk. The author(s) and Microsoft Corporation have no obligation to provide maintenance, support, updates, enhancements, or modifications to the software.

*/

package msftrakesh.RedisTemplateApp;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRetryService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration RECONNECT_MIN_INTERVAL = Duration.ofSeconds(30);
    private static final Duration RECONNECT_MAX_DURATION = Duration.ofMinutes(30);
    private static long firstFailureTime = -1;
    private static long lastReconnectTime = System.currentTimeMillis();
    private static final Semaphore reconnectSemaphore = new Semaphore(1);
    
    @Autowired
    public RedisRetryService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public CompletableFuture<Void> writeWithReconnect(String key, String value) {
        return CompletableFuture.runAsync(() -> {
            try {
                redisTemplate.opsForValue().set(key, value);
                firstFailureTime = -1; // Reset firstFailureTime after a successful operation
                System.out.println("Write successful for key: " + key);
            } catch (Exception e) {
                System.err.println("Write failed for key: " + key + ", attempting reconnect...");
                if (handleFailure()) {
                    try {
                        redisTemplate.opsForValue().set(key, value);  // Retry the operation after reconnect
                        System.out.println("Write successful after reconnect for key: " + key);
                    } catch (Exception retryException) {
                        System.err.println("Write failed again after reconnect for key: " + key);
                    }
                } else {
                    System.err.println("Reconnect failed, giving up write.");
                }
            }
        });
    }
    
    public CompletableFuture<String> readWithReconnect(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String value = redisTemplate.opsForValue().get(key);
                firstFailureTime = -1; // Reset firstFailureTime after a successful operation
                System.out.println("Read successful for key: " + key);
                return value;
            } catch (Exception e) {
                System.err.println("Read failed for key: " + key + ", attempting reconnect...");
                if (handleFailure()) {
                    try {
                        String value = redisTemplate.opsForValue().get(key);  // Retry the operation after reconnect
                        System.out.println("Read successful after reconnect for key: " + key);
                        return value;
                    } catch (Exception retryException) {
                        System.err.println("Read failed again after reconnect for key: " + key);
                    }
                } else {
                    System.err.println("Reconnect failed, reading from fallback...");
                    return fallbackRead(key);
                }
            }
            return null;
        });
    }
    
    private boolean handleFailure() {
        if (firstFailureTime == -1) {
            firstFailureTime = System.currentTimeMillis();
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - firstFailureTime >= RECONNECT_MAX_DURATION.toMillis()) {
            System.err.println("Giving up reconnect attempts after " + RECONNECT_MAX_DURATION.toMinutes() + " minutes.");
            return false;
        }
        
        if (currentTime - lastReconnectTime >= RECONNECT_MIN_INTERVAL.toMillis()) {
            if (forceReconnect()) {
                firstFailureTime = -1; // Reset firstFailureTime after a successful reconnect
                return true;
            }
        }
        return false;
    }
    
    private boolean forceReconnect() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReconnectTime < RECONNECT_MIN_INTERVAL.toMillis()) {
            System.out.println("Reconnect skipped, too soon since the last reconnect.");
            return false;
        }
        
        boolean lockTaken = false;
        try {
            // Try acquiring the semaphore
            lockTaken = reconnectSemaphore.tryAcquire(1, TimeUnit.SECONDS);
            if (!lockTaken) {
                System.out.println("Another thread is already reconnecting.");
                return false;
            }
            
            // Perform the actual reconnection
            System.out.println("Reconnecting...");
            redisTemplate.getConnectionFactory().getConnection().close();  // Close the current connection
            lastReconnectTime = System.currentTimeMillis();
            System.out.println("Reconnected successfully.");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to reconnect: " + e.getMessage());
            return false;
        } finally {
            if (lockTaken) {
                reconnectSemaphore.release();
            }
        }
    }
    
    private String fallbackRead(String key) {
        // Add your fallback logic here, like reading from a database or returning a default value
        System.out.println("Reading from fallback source for key: " + key);
        return "fallback_value";  // Placeholder for fallback read
    }

    //delete all keys in Redis
    public CompletableFuture<String> deleteAllKeys() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> keys = redisTemplate.keys("*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    return "All keys deleted";
                } else {
                    return "No keys to delete";
                }
            } catch (Exception e) {
                return "Error deleting keys: " + e.getMessage();
            }
        });
    }
}
