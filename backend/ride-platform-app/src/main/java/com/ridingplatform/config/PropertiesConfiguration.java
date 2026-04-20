package com.ridingplatform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ApplicationProperties.class, SecurityProperties.class, NotificationProperties.class})
public class PropertiesConfiguration {
}
