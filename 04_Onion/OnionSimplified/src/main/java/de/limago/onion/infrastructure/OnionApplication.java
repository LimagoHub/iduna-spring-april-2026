package de.limago.onion.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.limago.onion")
public class OnionApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnionApplication.class, args);
    }
}
