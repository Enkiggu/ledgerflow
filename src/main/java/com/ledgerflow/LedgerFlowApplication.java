package com.ledgerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        RedisReactiveAutoConfiguration.class,
        RedisReactiveHealthContributorAutoConfiguration.class
})
@EnableScheduling
@EnableAsync
@EnableCaching
@ConfigurationPropertiesScan
public class LedgerFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerFlowApplication.class, args);
    }
}
