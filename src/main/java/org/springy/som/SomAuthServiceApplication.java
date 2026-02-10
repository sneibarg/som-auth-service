package org.springy.som;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SomAuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SomAuthServiceApplication.class, args);
    }
}
