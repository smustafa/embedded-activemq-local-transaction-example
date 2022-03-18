package com.example.order.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.support.converter.MarshallingMessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@Configuration
@Slf4j
public class JMSConfiguration {

    @Bean
    public ActiveMQConnectionFactoryCustomizer configureActiveMqRedeliveryPolicy() {

        return connectionFactory -> {

            RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
            redeliveryPolicy.setInitialRedeliveryDelay(2000);
            redeliveryPolicy.setBackOffMultiplier(2);
            redeliveryPolicy.setUseExponentialBackOff(true);
            redeliveryPolicy.setMaximumRedeliveries(2);

            connectionFactory.setRedeliveryPolicy(redeliveryPolicy);

            log.info("Configuring ActiveMQConnectionFactoryCustomizer with RedeliveryPolicy");

        };
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(@Autowired ConnectionFactory connectionFactory, @Autowired PlatformTransactionManager jmsTransactionManager) {

        var factory = new DefaultJmsListenerContainerFactory();
        factory.setTransactionManager(jmsTransactionManager);
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        factory.setMessageConverter(createMarshallingMessageConverter());

        log.info("Configuring jmsListenerContainerFactory");

        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(@Autowired ConnectionFactory connectionFactory) {

        log.info("Configuring jmsTransactionManager");

        return new JmsTransactionManager(connectionFactory);
    }

    /**
     * We need to setPackagesToScan for the parent folder that contains all generated classes from our XSD(s)
     */
    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {

        var jaxb2Marshaller = new Jaxb2Marshaller();

        jaxb2Marshaller.setPackagesToScan("com.example.order.model");

        return jaxb2Marshaller;
    }

    @Bean
    public MarshallingMessageConverter createMarshallingMessageConverter() {

        var marshallingMessageConverter = new MarshallingMessageConverter(jaxb2Marshaller());
        marshallingMessageConverter.setTargetType(MessageType.TEXT);

        log.info("Configuring MarshallingMessageConverter for TEXT message type");

        return marshallingMessageConverter;
    }

}
