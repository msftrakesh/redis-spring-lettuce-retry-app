package msftrakesh.RedisTemplateApp;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retry/redis")
public class RedisRetryController {

    private final RedisRetryService redisRetryService;

    @Autowired
    public RedisRetryController(RedisRetryService redisRetryService) {
        this.redisRetryService = redisRetryService;
    }

    @PostMapping("/write")
    public CompletableFuture<String> writeData(@RequestParam String key, @RequestParam String value) {
        return redisRetryService.writeWithReconnect(key, value)
                .thenApply(result -> "Write operation completed");
    }

    @GetMapping("/read")
    public CompletableFuture<String> readData(@RequestParam String key) {
        return redisRetryService.readWithReconnect(key)
                .thenApply(result -> result != null ? result : "Key not found or fallback value returned");
                
    }
   
    @DeleteMapping("/deleteAll")
    public CompletableFuture<String> deleteAllKeys() {
        return redisRetryService.deleteAllKeys();
    }
}
