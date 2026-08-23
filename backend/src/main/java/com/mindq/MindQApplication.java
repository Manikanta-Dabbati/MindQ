package com.mindq;

import com.mindq.config.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MindQApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MindQApplication.class);
        app.addInitializers(new DotenvInitializer());
        app.run(args);
    }
}
