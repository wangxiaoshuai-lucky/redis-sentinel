package cn.wzy.redisSentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication
public class RedisSentinelApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RedisSentinelApplication.class, args);
        StringRedisTemplate template = context.getBean("redisTemplate", StringRedisTemplate.class);
        while (true) {
            try {
                template.execute((RedisCallback<Object>) (conn) -> {
                    conn.set("username".getBytes(), "wangzy".getBytes());
                    return "ok";
                });
                Thread.sleep(1000);
                template.execute((RedisCallback<Object>) (conn) -> {
                    byte[] data = conn.get("username".getBytes());
                    if (data != null && data.length != 0) {
                        System.out.println("username is :" + template.getStringSerializer().deserialize(data));
                    }
                    return "ok";
                });
            } catch (Exception e) {
                System.out.println("主从切换中......");
            }
        }
    }
}
