package com.ridingplatform.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultBeansConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
