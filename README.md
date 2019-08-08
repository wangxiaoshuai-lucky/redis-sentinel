## redis 主从复制 哨兵机制进行切换
### redis安装
* 首先安装gcc：sudo apt-get install gcc
* 下载安装redis：
~~~
wget http://download.redis.io/releases/redis-4.0.8.tar.gz
tar xzf redis-4.0.8.tar.gz
cd redis-4.0.8
make
sudo make install
~~~
### 模拟运行多机环境：
启动多个进程，运行在不同的端口上就行  
新建目录结构：新建多个目录存放运行配置文件，复制redis-4.0.8到相应目录  
目录结构:
~~~
    /redis-4.0.8
        /src
            redis-server
            redis-cli
    /6666/redis.conf
    /7777/redis.conf
~~~
### 修改redis.conf
./6666/redis.conf修改: 作为master
~~~
1. port 6666
2. bind 192.168.227.144
3. dir ./
4. protected-mode no
5. daemonize yes
6. pidfile /var/run/redis_6666.pid
~~~
./7777/redis.conf修改: 作为slave
~~~
1. port 7777
2. bind 192.168.227.144
3. slaveof 192.168.227.144 6666
4. # master的密码 没有就不填这个
   # masterauth 123456
5. protected-mode no
6. daemonize yes
7. pidfile /var/run/redis_7777.pid
8. dir ./
~~~
### 测试主从复制
登录6666的redis:  
./redis-4.0.8/src/redis-cli -h localhost -p 6666  
输入 info：输出role为master
~~~
# Replication
role:master
connected_slaves:1
slave0:ip=127.0.0.1,port=7777,state=online,offset=580,lag=0
~~~
master可以正常的get和set
登录7777的redis：  
./redis-4.0.8/src/redis-cli -h localhost -p 7777  
输入 info：输出role为slave
~~~
# Replication
role:slave
master_host:localhost
master_port:6666
master_link_status:up
master_last_io_seconds_ago:5
master_sync_in_progress:0
~~~
set操作报错：
~~~
localhost:7777> set username username
(error) READONLY You can't write against a read only slave.
~~~
### sentinel 自动主从切换
修改./redis-4.0.8/sentinel.conf
~~~
1. protected-mode no
2. 修改sentinel monitor mymaster  192.168.227.144  6666  1
3. 修改心跳时间：sentinel down-after-milliseconds mymaster 3000
~~~
启动sentinel：  
./redis-4.0.8/src/redis-server ./redis-4.0.8/sentinel.conf --sentinel &
~~~
1819:X 08 Aug 09:44:45.277 # Redis version=4.0.8, bits=64, commit=00000000, modified=0, pid=1819, just started
1819:X 08 Aug 09:44:45.277 # Configuration loaded
1819:X 08 Aug 09:44:45.278 * Increased maximum number of open files to 10032 (it was originally set to 1024).
                _._
           _.-``__ ''-._
      _.-``    `.  `_.  ''-._           Redis 4.0.8 (00000000/0) 64 bit
  .-`` .-```.  ```\/    _.,_ ''-._
 (    '      ,       .-`  | `,    )     Running in sentinel mode
 |`-._`-...-` __...-.``-._|'` _.-'|     Port: 26379
 |    `-._   `._    /     _.-'    |     PID: 1819
  `-._    `-._  `-./  _.-'    _.-'
 |`-._`-._    `-.__.-'    _.-'_.-'|
 |    `-._`-._        _.-'_.-'    |           http://redis.io
  `-._    `-._`-.__.-'_.-'    _.-'
 |`-._`-._    `-.__.-'    _.-'_.-'|
 |    `-._`-._        _.-'_.-'    |
  `-._    `-._`-.__.-'_.-'    _.-'
      `-._    `-.__.-'    _.-'
          `-._        _.-'
              `-.__.-'

1819:X 08 Aug 09:44:45.279 # WARNING: The TCP backlog setting of 511 cannot be enforced because /proc/sys/net/core/somaxconn is set to the lower value of 128.
1819:X 08 Aug 09:44:45.279 # Sentinel ID is e40a513a83b8712d66b0044eb3f3e6370d66d5a7
1819:X 08 Aug 09:44:45.279 # +monitor master mymaster 192.168.227.144 6666 quorum 1
1819:X 08 Aug 09:44:45.282 * +slave slave 192.168.227.144:7777 192.168.227.144 7777 @ mymaster 192.168.227.144 6666
1819:X 08 Aug 09:44:50.289 # +sdown slave 127.0.0.1:7777 127.0.0.1 7777 @ mymaster 192.168.227.144 6666
1819:X 08 Aug 09:44:55.325 * +convert-to-slave slave 192.168.227.144:6666 192.168.227.144 6666 @ mymaster 192.168.227.144 6666
~~~
### 测试主从切换
登录6666然后输入shutdown，自动切换7777为master
~~~
1819:X 08 Aug 09:49:21.093 # +odown master mymaster 192.168.227.144 6666 #quorum 1/1
1819:X 08 Aug 09:49:21.093 # +new-epoch 12
1819:X 08 Aug 09:49:21.093 # +try-failover master mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.098 # +vote-for-leader e40a513a83b8712d66b0044eb3f3e6370d66d5a7 12
1819:X 08 Aug 09:49:21.098 # +elected-leader master mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.098 # +failover-state-select-slave master mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.175 # +selected-slave slave 192.168.227.144:7777 192.168.227.144 7777 @ mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.175 * +failover-state-send-slaveof-noone slave 192.168.227.144:7777 192.168.227.144 7777 @ mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.253 * +failover-state-wait-promotion slave 192.168.227.144:7777 192.168.227.144 7777 @ mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.262 # +promoted-slave slave 192.168.227.144:7777 192.168.227.144 7777 @ mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.264 # +failover-state-reconf-slaves master mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.320 # +failover-end master mymaster 192.168.227.144 6666
1819:X 08 Aug 09:49:21.320 # +switch-master mymaster 192.168.227.144 6666 192.168.227.144 7777
1819:X 08 Aug 09:49:21.321 * +slave slave 127.0.0.1:7777 127.0.0.1 7777 @ mymaster 192.168.227.144 7777
1819:X 08 Aug 09:49:21.321 * +slave slave 192.168.227.144:6666 192.168.227.144 6666 @ mymaster 192.168.227.144 7777
1819:X 08 Aug 09:49:26.357 # +sdown slave 192.168.227.144:6666 192.168.227.144 6666 @ mymaster 192.168.227.144 7777
~~~
登录7777为master角色，能正常get、set
### java Jedis测试主从切换
pom.xml
~~~
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.7.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>cn.wzy</groupId>
    <artifactId>redis-sentinel</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>redis-sentinel</name>
    <description>Demo project for Spring Boot</description>
    <properties>
        <java.version>1.8</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-redis</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>log4j</groupId>
                    <artifactId>log4j</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>jcl-over-slf4j</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-api</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-to-slf4j</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
                <exclusion>
                    <artifactId>logback-core</artifactId>
                    <groupId>ch.qos.logback</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <!--redis 支持java的语言 -->
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>2.9.3</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

~~~
JedisConfig.java:
~~~
package cn.wzy.redisSentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;

/**
 * @author `wangzy`
 * @version 1.0
 * @since 2019/8/7 14:46
 **/
@Configuration
public class JedisConfig {

    @Value("${redis.maxIdle}")
    private Integer maxIdle;

    @Value("${redis.maxActive}")
    private Integer maxActive;

    @Value("${redis.maxWait}")
    private Long maxWait;

    @Value("${redis.testOnBorrow}")
    private Boolean testOnBorrow;

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.sentinel.port}")
    private Integer sentinelPort;

    @Bean
    public RedisSentinelConfiguration redisSentinelConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        RedisNode redisNode = new RedisNode(host, port);
        redisNode.setName("mymaster");
        configuration.setMaster(redisNode);
        configuration.setSentinels(Collections.singleton(new RedisNode(host, sentinelPort)));
        return configuration;
    }

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWait);
        config.setMaxTotal(maxActive);
        config.setTestOnBorrow(testOnBorrow);
        return config;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory(JedisPoolConfig jedisPoolConfig,
                                                         RedisSentinelConfiguration redisSentinelConfiguration) {
        return new JedisConnectionFactory(redisSentinelConfiguration, jedisPoolConfig);
    }

    @Bean
    public StringRedisTemplate redisTemplate(
            JedisConnectionFactory jedisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        return redisTemplate;
    }
}
~~~
测试启动类：
~~~
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
~~~
运行：两个服务启动6666（master），7777（slave），中途对6666使用shutdown模拟挂掉
输出：表明这个连接总是保证连接到master
~~~
2019-08-08 09:58:14.520  INFO 12668 --- [           main] redis.clients.jedis.JedisSentinelPool    : Redis master running at 192.168.227.144:6666, starting Sentinel listeners...
2019-08-08 09:58:14.536  INFO 12668 --- [           main] redis.clients.jedis.JedisSentinelPool    : Created JedisPool to master at 192.168.227.144:6666
2019-08-08 09:58:14.958  INFO 12668 --- [           main] c.w.r.RedisSentinelApplication           : Started RedisSentinelApplication in 2.647 seconds (JVM running for 4.344)
username is :wangzy
username is :wangzy
username is :wangzy
username is :wangzy
主从切换中......
主从切换中......
主从切换中......
主从切换中......
主从切换中......
2019-08-08 09:58:28.299  INFO 12668 --- [.227.144:26379]] redis.clients.jedis.JedisSentinelPool    : Created JedisPool to master at 192.168.227.144:7777
主从切换中......
username is :wangzy
username is :wangzy
username is :wangzy
username is :wangzy
~~~
