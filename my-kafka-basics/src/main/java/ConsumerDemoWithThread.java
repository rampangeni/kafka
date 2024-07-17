import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {
    public static void main(String[] args) {
        new ConsumerDemoWithThread().run();
    }

    private ConsumerDemoWithThread() {

    }

    private void run() {
        Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "my-third-application-demo";
        String topic = "first_topic";

        // latch for dealing with multiple thread
        CountDownLatch latch = new CountDownLatch(1);

        logger.info("Creating the consumer thread !!");

        // create the consumer runnable
        Runnable myConsumerRunnable = new ConsumerRunnable(
                bootstrapServer,
                groupId,
                topic,
                latch);

        // start the thread
        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited !!");
        }
        ));

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted", e);
        } finally {
            logger.info("Application is closing !!");
        }
    }

    public class ConsumerRunnable implements Runnable {

        private KafkaConsumer<String, String> consumer;
        private CountDownLatch latch;
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        public ConsumerRunnable (String bootstrapServer,
                               String groupId,
                               String topic,
                               CountDownLatch latch) {
            this.latch = latch;

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
            consumer = new KafkaConsumer<String, String>(properties);

            // step-3: subscribe consumer to our topics
            consumer.subscribe(Arrays.asList(topic));
        }

        @Override
        public void run() {
            try {
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
            } catch (WakeupException e) {
                logger.info("Received shutdown signal !!");
            } finally {
                consumer.close();
                // tell our main code we're done with the consumer
                latch.countDown();
            }
        }

        public void shutdown() {
            /*
             * The wakeup() method is a special method to interrupt consumer.poll().
             * It will throw the exception WakeupException.
             */
            consumer.wakeup();
        }
    }
}
