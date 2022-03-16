package com.example.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "customer-orders-queue-config")
@Data
public class JMSQueuesConfig {

    private String customerOrdersInboundQueue;
    private String customerOrdersOutboundServiceAQueue;
    private String customerOrdersOutboundServiceBQueue;
    private String customerOrdersErrorQueue;

}
