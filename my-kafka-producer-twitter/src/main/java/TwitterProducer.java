import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class TwitterProducer {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    String apiKey = "fKKgTF2iBXmvPhavWgIkL7Qia";
    String apiSecret = "MhqH4OOOr2cTWHmYnc5enOM0oyx4hVhlI4RxMJgdC19GsLYEbZ";
    String accessToken = "1595675977031782400-U9Jfc6nIQHVCJgo7cARjf7a0Eh2aOZ";
    String accessSecret = "bWnpX157CiRaUjiOTVKkcvc5K1FejLsc0d1klnPPevHuS";

    String keyword = "kafka";

    public TwitterProducer() {}

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run() {
        // Step-1 : create Twitter client
        TwitterClient client = createTwitterClient();

        // Step-2 : get the tweets
        TweetList tweetList = client.searchTweets(keyword, AdditionalParameters.builder()
                        .recursiveCall(false)
                        .build());
        List<TweetV2.TweetData> tweetDataList = tweetList.getData();

        // Step-2 : create Kafka producer
        KafkaProducer<String,String> kafkaProducer = CreateKafkaProducer();

        // Step-3 : loop to send Tweets to Kafka
        for (Iterator i = tweetDataList.iterator(); i.hasNext(); ){
            TweetV2.TweetData tweet = (TweetV2.TweetData)i.next();
            logger.info("Tweet Data: " + tweet.getText());
            kafkaProducer.send(new ProducerRecord<>("twitter_tweets", tweet.getId(),
                    tweet.getText()), new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e != null) {
                        logger.error("Something bad happened", e);
                    }
                }
            });
        }

        kafkaProducer.flush();
    }

    public TwitterClient createTwitterClient() {
        TwitterClient twitterClient = new TwitterClient(TwitterCredentials.builder()
                .accessToken(accessToken)
                .accessTokenSecret(accessSecret)
                .apiKey(apiKey)
                .apiSecretKey(apiSecret)
                .build());
        return twitterClient;
    }

    public KafkaProducer<String,String> CreateKafkaProducer() {
        String bootstrapServer = "127.0.0.1:9092";

        // step-1: create producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // step-2: create a safe producer
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));

        /* If (Kafka > 2.0 version)
         *      MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 5
         * Else
         *      MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 1
         */
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        // High Throughput producer setting at the expense of a bit of latency and CPU usage

        // setting compression : gzip, snappy, lz4 or zstd
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // setting batching
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));

        // step-3: create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        return producer;
    }
}
