package com.homefin.common.web;

import com.homefin.common.error.GlobalExceptionHandler;
import com.homefin.common.security.JwtRolesConverterFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * A custom Spring Boot auto-configuration. Any service that has common-lib on its classpath
 * gets these beans automatically (registered in
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
 * This is exactly how every "spring-boot-starter-*" works under the hood.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return JwtRolesConverterFactory.create();
    }
}
