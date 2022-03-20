package com.example.order.inbound;

import com.example.order.config.JMSQueuesConfig;
import com.example.order.model.Root;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.annotation.DirtiesContext;

import javax.jms.Message;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
class CustomerOrdersInboundListenerIT {

    @Autowired
    private JMSQueuesConfig jmsQueuesConfig;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;

    @Autowired
    private MessageConverter messageConverter;

    @Test
    void validateSuccessfulProcessing() throws Exception {

        putMessageOnQueue();

        //Assert Processed Order is put on Service A Outbound Queue
        Message outboundServiceAMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersOutboundServiceAQueue());
        Assertions.assertNotNull(outboundServiceAMessage);

        Root processedCustomerOrders = Root.class.cast(messageConverter.fromMessage(outboundServiceAMessage));

        //Verify Processing
        processedCustomerOrders.getOrders().getOrder().forEach(order -> {
            Assertions.assertTrue(order.isProcessed());
        });

        //Validate all Headers being sent with processed message
        Assertions.assertEquals("OutboundHeaderValue", outboundServiceAMessage.getStringProperty("OutboundHeader"));

        //Assert Processed Order is put on Service B Outbound Queue
        Message outboundServiceBMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue());
        Assertions.assertNotNull(outboundServiceBMessage);

    }

    private void putMessageOnQueue() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("messages/CustomerOrders.xml");

        Root rootMessageToSend = Root.class.cast(jaxb2Marshaller.unmarshal(new StreamSource(inputStream)));

        //Using MessagePostProcessor to pass extra Message Headers with the (Root) message
        jmsTemplate.convertAndSend(jmsQueuesConfig.getCustomerOrdersInboundQueue(), rootMessageToSend, message -> {
            message.setStringProperty("InboundHeader", "InboundHeaderValue");
            return message;
        });
    }

}






