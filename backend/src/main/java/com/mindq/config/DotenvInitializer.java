package com.mindq.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, Object> props = new HashMap<>();

        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(e -> props.put(e.getKey(), e.getValue()));
        } catch (Exception ignored) {}

        try {
            Dotenv parentDotenv = Dotenv.configure().directory("../").ignoreIfMissing().load();
            parentDotenv.entries().forEach(e -> props.putIfAbsent(e.getKey(), e.getValue()));
        } catch (Exception ignored) {}

        if (!props.isEmpty()) {
            applicationContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource("dotenvProperties", props));
        }
    }
}
