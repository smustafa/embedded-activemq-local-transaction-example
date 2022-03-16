package com.example.order.inbound;

import com.example.order.config.JMSQueuesConfig;
import com.example.order.model.Root;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomerOrdersHandler {

    @Autowired
    private JMSQueuesConfig jmsQueuesConfig;

    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;

    @Autowired
    private CustomerOrdersProcessor customerOrdersProcessor;

    @Autowired
    private JmsTemplate jmsTemplate;

    public void processMessage(Root customerOrders) {

        Timer.Sample timer = Timer.start();

        try {

            customerOrdersProcessor.processOrders(customerOrders);

            timer.stop(Metrics.timer("jms_message_receive_duration", "message_type", "Root", "format_received", "TextMessage", "exception", "None"));

        } catch (IllegalStateException illegalStateException) {

            logExceptionAndPutOnErrorQueue(customerOrders, illegalStateException, timer);

        } catch (Exception exception) {
            //Any other Exceptions should log, and trigger rollback,
            logException(exception, timer);

            throw exception;
        }

    }

    private void logException(Exception exception, Timer.Sample timer) {
        logExceptionAndPutOnErrorQueue(null, exception, timer);
    }

    private void logExceptionAndPutOnErrorQueue(Root customerOrders, Exception exception, Timer.Sample timer) {

        log.error("Encountered an Exception while processing message", exception);

        timer.stop(Metrics.timer("jms_message_receive_duration", "exception", exception.getMessage() != null ? exception.getMessage() : "No Message"));

        if (customerOrders != null) {

            log.error("Putting Message on ErrorQueue: {}", jmsQueuesConfig.getCustomerOrdersErrorQueue(), exception);

            jmsTemplate.convertAndSend(jmsQueuesConfig.getCustomerOrdersErrorQueue(), customerOrders);
        }
    }

}




