package com.example.order.inbound;

import com.example.order.config.JMSQueuesConfig;
import com.example.order.model.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
public class CustomerOrdersProcessor {

    @Autowired
    private JMSQueuesConfig jmsQueuesConfig;

    @Autowired
    private JmsTemplate jmsTemplate;

    public void processOrders(Root customerOrders) {

        customerOrders.getOrders().getOrder().forEach(order -> {
            order.setProcessedDate(LocalDate.now());
            order.setProcessed(Boolean.TRUE);
        });

        sendProcessedOrdersToServiceAOutbound(customerOrders);
        sendProcessedOrdersToServiceBOutbound(customerOrders);

    }

    private void sendProcessedOrdersToServiceAOutbound(Root customerOrders) {

        //Put message on Service A Outbound  Queue + Add Extra Headers to it.
        jmsTemplate.convertAndSend(jmsQueuesConfig.getCustomerOrdersOutboundServiceAQueue(), customerOrders, outboundMessage -> {

            outboundMessage.setStringProperty("OutboundHeader", "OutboundHeaderValue");

            log.debug("Message: {} is put on Queue: {}", customerOrders, jmsQueuesConfig.getCustomerOrdersOutboundServiceAQueue());
            return outboundMessage;
        });

    }

    private void sendProcessedOrdersToServiceBOutbound(Root customerOrders) {

        //Put message on Service B Outbound  Queue + Add Extra Headers to it.
        jmsTemplate.convertAndSend(jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue(), customerOrders, outboundMessage -> {

            outboundMessage.setStringProperty("OutboundHeader", "OutboundHeaderValue");

            log.debug("Message: {} is put on Queue: {}", customerOrders, jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue());
            return outboundMessage;
        });

    }

}




