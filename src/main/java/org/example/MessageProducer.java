package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

public class MessageProducer {
    KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(
            Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092",
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()
            )
    );
    public void send(ProducerRecord<String,String> producerRecord){
        kafkaProducer.send(producerRecord);
        System.out.println("*Sent message: " + producerRecord.value());
    }
    public void sendUserStatus(String topic, String message) {
        kafkaProducer.send(new ProducerRecord<>(topic, message));
        System.out.println("*Sent user status message: " + message);
    }

}
