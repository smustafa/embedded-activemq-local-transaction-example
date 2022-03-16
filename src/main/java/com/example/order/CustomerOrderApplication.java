package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
@ConfigurationPropertiesScan
public class CustomerOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerOrderApplication.class, args);
    }

}
