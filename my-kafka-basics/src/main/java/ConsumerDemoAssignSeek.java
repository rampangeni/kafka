import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoAssignSeek {
    public static void main(String[] args) {
        String bootstrapServer = "127.0.0.1:9092";
        String topic = "first_topic";

        Logger logger = LoggerFactory.getLogger(ConsumerDemoAssignSeek.class.getName());

        // step-1: create consumer properties
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // step-2: create the consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        // step-3: assign and seek are mostly used to replay data and fetch a specific message
        TopicPartition partitionToReadFrom = new TopicPartition(topic, 0);
        long offsetToReadFrom = 15L;
        // step-3.1: assign
        consumer.assign(Arrays.asList(partitionToReadFrom));
        // step-3.2: seek
        consumer.seek(partitionToReadFrom, offsetToReadFrom);

        int numberOfMessageToRead = 5;
        boolean keepOnReading = true;
        int numberOfMessagesReadSoFar = 0;

        // step-4: poll for new data
        while (keepOnReading) {
           ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
           for(ConsumerRecord<String, String> record : records) {
               numberOfMessagesReadSoFar++;
               logger.info("Key: " + record.key() + " , Value: " + record.value());
               logger.info("Topic: " + record.topic() + "\n" +
                       "Offset: " + record.offset() + "\n" +
                       "Partition: " + record.partition());
               if (numberOfMessagesReadSoFar >= numberOfMessageToRead) {
                   keepOnReading = false; // to exit the while loop
                   break; // to exit the for loop
               }
           }
        }

        logger.info("Existing the application !!");
    }
}
