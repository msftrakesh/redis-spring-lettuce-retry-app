package msftrakesh.RedisTemplateApp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis")
public class RedisController {

    private final RedisService redisService;

    @Autowired
    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    @PostMapping("/write")
    public String writeData(@RequestParam String key, @RequestParam String value) {
        redisService.saveData(key, value);
        return "Data written to Redis!";
    }

    @GetMapping("/read")
    public String readData(@RequestParam String key) {
        String value = redisService.getData(key);
        return value != null ? value : "Key not found!";
    }
    
}
