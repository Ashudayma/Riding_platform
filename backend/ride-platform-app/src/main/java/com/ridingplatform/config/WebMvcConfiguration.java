package com.ridingplatform.config;

import com.ridingplatform.security.web.AuditLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final AuditLoggingInterceptor auditLoggingInterceptor;

    public WebMvcConfiguration(AuditLoggingInterceptor auditLoggingInterceptor) {
        this.auditLoggingInterceptor = auditLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLoggingInterceptor);
    }
}
