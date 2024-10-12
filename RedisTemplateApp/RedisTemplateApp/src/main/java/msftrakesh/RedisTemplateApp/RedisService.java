package msftrakesh.RedisTemplateApp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final ValueOperations<String, String> valueOperations;

    @Autowired
    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.valueOperations = redisTemplate.opsForValue();
    }

    public void saveData(String key, String value) {
        valueOperations.set(key, value);
    }

    public String getData(String key) {
        return valueOperations.get(key);
    }
}
