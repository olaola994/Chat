package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

import javax.swing.*;

@Slf4j
public class Main {
    public static void main(String[] args) {
        EmbeddedKafkaBroker embeddedKafkaBroker = new EmbeddedKafkaZKBroker(1).kafkaPorts(9092);
        embeddedKafkaBroker.afterPropertiesSet();
        MessageProducer messageProducer = new MessageProducer();
        SwingUtilities.invokeLater(() -> new Chat("Uzytkownik1", "chat", "user-status"));
        SwingUtilities.invokeLater(() -> new Chat("Uzytkownik2", "chat", "user-status"));

    }
}