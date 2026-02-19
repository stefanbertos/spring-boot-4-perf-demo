package com.example.perftester.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Forwards unknown routes to index.html so React Router can handle client-side navigation.
 * API paths (/api/**) and actuator paths (/actuator/**) are excluded and handled normally.
 */
@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward top-level SPA routes (e.g. /send, /test-runs, /admin) to index.html
        registry.addViewController("/{path:^(?!api|actuator)[a-zA-Z0-9-]+$}")
                .setViewName("forward:/index.html");
        // Forward two-segment SPA routes (e.g. /test-runs/123)
        registry.addViewController("/{path:^(?!api|actuator)[a-zA-Z0-9-]+}/{subPath:[a-zA-Z0-9-]+}")
                .setViewName("forward:/index.html");
    }
}
