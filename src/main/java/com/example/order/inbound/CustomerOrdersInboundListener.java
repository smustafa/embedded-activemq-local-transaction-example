package com.example.order.inbound;

import com.example.order.model.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CustomerOrdersInboundListener {

    @Autowired
    private CustomerOrdersHandler customerOrdersHandler;

    @JmsListener(concurrency = "1", destination = "${customer-orders-queue-config.CustomerOrdersInboundQueue}")
    @Transactional(rollbackFor = { Exception.class})
    public void onMessage(@Payload Root order,JmsMessageHeaderAccessor jmsMessageHeaderAccessor) throws Exception{

        log.debug("=========> Inbound Header Value {}", String.valueOf(jmsMessageHeaderAccessor.getHeader("InboundHeader")));
        customerOrdersHandler.processMessage(order);

    }

}
