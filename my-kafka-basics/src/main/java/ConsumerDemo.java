import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class ConsumerDemo {
    public static void main(String[] args) {
        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "my-first-application-demo";
        String topic = "first_topic";

        Logger logger = LoggerFactory.getLogger(ConsumerDemo.class.getName());

        // step-1: create consumer properties
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // step-2: create the consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        // step-3: subscribe consumer to our topics
        consumer.subscribe(Arrays.asList(topic));

        // step-4: poll for new data
        while (true) {
           ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
           for(ConsumerRecord<String, String> record : records) {
               logger.info("Key: " + record.key() + " , Value: " + record.value());
               logger.info("Topic: " + record.topic() + "\n" +
                       "Offset: " + record.offset() + "\n" +
                       "Partition: " + record.partition());
           }
        }
    }
}
