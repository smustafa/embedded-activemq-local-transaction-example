package com.example.order.inbound;

import com.example.order.config.JMSQueuesConfig;
import com.example.order.model.Root;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.jms.Message;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@ExtendWith({ OutputCaptureExtension.class })
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerOrdersMessageConsumerRollbackIT {

    @Autowired
    private JMSQueuesConfig jmsQueuesConfig;

    @SpyBean
    private CustomerOrdersHandler customerOrdersHandler;

    @SpyBean
    private JmsTemplate jmsTemplate;

    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;

    @BeforeAll
    private void setup() {
        //Setting ReceiveTimeout, otherwise the code will bock because .receive will wait INDEFINITELY, and we know that there should be no messages
        //so fail fast
        //Default value is to wait indefinite ==> private long receiveTimeout = RECEIVE_TIMEOUT_INDEFINITE_WAIT;
        jmsTemplate.setReceiveTimeout(1000);
    }

    @Test
    void validateRollback_onGenericCheckedException(CapturedOutput capturedOutput) {

        BDDMockito.willAnswer(invocation -> {
            throw new Exception();
        }).given(jmsTemplate).convertAndSend(Mockito.eq(jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue()), Mockito.any(Root.class), Mockito.any(MessagePostProcessor.class));

        putMessageOnQueue();

        //Verify Message is Retried Max 3 Times (RedeliveryPolicy)
        Mockito.verify(customerOrdersHandler, Mockito.timeout(1000).times(3)).processMessage(Mockito.any(Root.class));

        //Verify Message is put on Outbound Queue A
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("is put on Queue: EMBEDDED.AMQ.EXAMPLE.OUTBOUND.A.ROOT"));

        //Verify Rollback
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Getting transaction for [com.example.order.inbound.CustomerOrdersInboundListener.onMessage]"));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Completing transaction for [com.example.order.inbound.CustomerOrdersInboundListener.onMessage] after exception: java.lang.Exception"));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Participating transaction failed - marking existing transaction as rollback-only"));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Rolling back JMS transaction on Session"));

        //Assert Processed Order put on Service A Outbound Queue is ROLLED back
        Message serviceAOutboundMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersOutboundServiceAQueue());
        Assertions.assertNull(serviceAOutboundMessage);

        //Assert Processed Order is NOT put on Service B Outbound Queue
        Message serviceBOutboundMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue());
        Assertions.assertNull(serviceBOutboundMessage);

        //Assert Processed Order is NOT put on Error Queue
        Message errorMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersErrorQueue());
        Assertions.assertNull(errorMessage);

    }

    @Test
    void validateNoRollback_onIllegalStateException_MessagePutOnErrorQueue(CapturedOutput capturedOutput) {

        BDDMockito.willAnswer(invocation -> {
            throw new IllegalStateException();
        }).given(jmsTemplate).convertAndSend(Mockito.eq(jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue()), Mockito.any(Root.class), Mockito.any(MessagePostProcessor.class));

        putMessageOnQueue();

        //Verify Message is never retried, and put on Error Queue after Exception is Caught.
        Mockito.verify(customerOrdersHandler, Mockito.timeout(10000).times(1)).processMessage(Mockito.any(Root.class));

        //Verify Message is put on Outbound Queue A
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("is put on Queue: EMBEDDED.AMQ.EXAMPLE.OUTBOUND.A.ROOT"));

        //Verify Transaction Commit without Rollback Taking Place
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Getting transaction for [com.example.order.inbound.CustomerOrdersInboundListener.onMessage]"));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Putting Message on ErrorQueue: EMBEDDED.AMQ.EXAMPLE.ERROR.ROOT"));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Completing transaction for [com.example.order.inbound.CustomerOrdersInboundListener.onMessage]"));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> capturedOutput.getOut().contains("Committing JMS transaction on Session"));

        //Assert Processed Order put on Service A Outbound Queue is NOT rolled back.
        Message serviceAOutboundMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersOutboundServiceAQueue());
        Assertions.assertNotNull(serviceAOutboundMessage);

        //Assert Processed Order is NOT put on Service B Outbound Queue
        Message serviceBOutboundMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersOutboundServiceBQueue());
        Assertions.assertNull(serviceBOutboundMessage);

        //Assert Processed Order is put on Error Queue since IllegalStateException is caught.
        Message errorMessage = jmsTemplate.receive(jmsQueuesConfig.getCustomerOrdersErrorQueue());
        Assertions.assertNotNull(errorMessage);

    }

    private void putMessageOnQueue() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("messages/CustomerOrders.xml");

        Root rootMessageToSend = Root.class.cast(jaxb2Marshaller.unmarshal(new StreamSource(inputStream)));

        //Using MessagePostProcessor to pass extra Message Headers with the (Root) message
        jmsTemplate.convertAndSend(jmsQueuesConfig.getCustomerOrdersInboundQueue(), rootMessageToSend, message -> {
            message.setStringProperty("InboundHeader", "InboundHeaderValue");
            return message;
        });
    }

}






