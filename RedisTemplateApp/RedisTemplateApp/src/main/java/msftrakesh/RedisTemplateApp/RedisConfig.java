package msftrakesh.RedisTemplateApp;



import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;


@Configuration
public class RedisConfig {
    
    private static final Duration RECONNECT_MIN_INTERVAL = Duration.ofSeconds(30);
    private static final Duration RECONNECT_MAX_DURATION = Duration.ofMinutes(30);
    
    @Value("${spring.redis.host}")
    private String redisHost;
    
    @Value("${spring.redis.port}")
    private int redisPort;
    
    @Value("${spring.redis.password}")
    private String redisPassword;
    
    
    
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);
        
        ClientOptions clientOptions = ClientOptions.builder()
        .autoReconnect(true)
        .cancelCommandsOnReconnectFailure(true)
        .build();
        
        Delay reconnectDelay = Delay.fullJitter(Duration.ofMillis(100), Duration.ofSeconds(10), 100, TimeUnit.MILLISECONDS);
        
        ClientResources clientResources =  DefaultClientResources.builder()
        .reconnectDelay(reconnectDelay)
        .build();

      
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .clientResources(clientResources)
        .useSsl()
        .and()
        .commandTimeout(Duration.ofMillis(6000))
        .build();

        
        
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);        
        
        lettuceConnectionFactory.afterPropertiesSet();
        return lettuceConnectionFactory;
    }
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use StringRedisSerializer for both key and value serialization
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
