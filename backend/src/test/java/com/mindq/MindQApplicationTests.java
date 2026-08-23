package com.mindq;

import com.mindq.config.DotenvInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
class MindQApplicationTests {

    @Test
    void contextLoads() {
    }
}
