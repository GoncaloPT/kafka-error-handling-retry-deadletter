package pt.goncalo.kafka.demo.deadletter.kafkaconsumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import pt.goncalo.kafka.demo.deadletter.kafkaconsumer.model.CarPart;
import pt.goncalo.kafka.demo.deadletter.persistence.CarPartRespository;
import pt.goncalo.kafka.demo.deadletter.persistence.entities.CarPartEntity;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Consumer that has an operation that might fail,
 * and therefore it should try to retry. If the number of retries reaches a predetermined
 * threshold it should send the message to deadletter
 */
@Component
@Slf4j
public class ErrorPruneConsumer {
    private final CarPartRespository respository;
    private final Long nackIntervalMs;
    private final KafkaTemplate<String, CarPart> kafkaProducer;
    private static final String KAFKA_DL_RETRY_COUNT = "kafka_deadletter_retry_count";
    private static final String DL_POSTFIX = "-deadletter";

    public ErrorPruneConsumer(CarPartRespository repository,
                              @Value("${nack-interval-ms}") Long nackIntervalMs,
                              KafkaTemplate<String, CarPart> kafkaProducer) {
        this.respository = repository;
        this.nackIntervalMs = nackIntervalMs;
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(topics = "${car-part-topic-name}",
            errorHandler = "ListenerErrorHandler")
    public void listenTo(@Payload CarPart carPart,
                         @Header(value = KafkaHeaders.RECEIVED_MESSAGE_KEY, required = false) String key,
                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            /*@Header(KafkaHeaders.RECEIVED_TIMESTAMP) long receivedTimestamp,*/
                         @Header(value = KAFKA_DL_RETRY_COUNT, defaultValue = "1", required = false) int retryCount,
                         Acknowledgment ack
    ) throws InterruptedException {
        log.info("Consuming topic {}, with key {}, with payload {}", topic, key, carPart);
        try {
            respository.save(CarPartEntity.fromModel(carPart));
        } catch (Exception e) {
            log.error("exception: ", e);
            if (e.getCause() instanceof JDBCConnectionException) {
                /*
                 * Connection error, maybe the db is down; so we will try to retry later
                 * There is no point in going for the next message here since the consumer will fail again.
                 * Therefore, we will try again latter
                 */
                log.error("Connection Exception while trying to save record to the database. trying again in {}", nackIntervalMs * retryCount);
                ack.nack(nackIntervalMs * retryCount);
                return;
            }
            /*
                Any other problems we are not ready to handle should go to deadletter. We are assuming that, since
                it's an unknown error we will not be able to resolve it just by retrying.
                        */
            log.error("Unforeseen exception occurred :\"{}\"! sending to deadletter topic: {}", e.getMessage(), topic + DL_POSTFIX);
            try {
                sendToDeadLetterTopicAsync(carPart, topic, e.getMessage(), retryCount);
                // the message was sent to DL so we can move to the next message
                log.info("message successfully sent to DL. Error ");
                ack.acknowledge();
            } catch (ExecutionException | TimeoutException dlSendEx) {
                log.error("exception ocurred while sending to DL; will retry later to avoid losing the message ", dlSendEx);
                ack.nack(nackIntervalMs * retryCount);
            }

        }

    }

    private void sendToDeadLetterTopicAsync(CarPart carPart, String topic, String message, int retryCount) throws ExecutionException, InterruptedException, TimeoutException {
        var record = new ProducerRecord<String, CarPart>(topic + DL_POSTFIX, carPart);
        record.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, topic.getBytes());
        record.headers().add(KafkaHeaders.DLT_EXCEPTION_MESSAGE, message.getBytes());
        record.headers().add(KAFKA_DL_RETRY_COUNT, ByteBuffer.allocate(4).putInt(retryCount + 1).array());
        kafkaProducer.send(record).get(1, TimeUnit.MINUTES);
    }


    /*

                            ---------------  SEPARATOR ---------------
     */

















   /*@KafkaListener(topics = "${car-part-topic-name}",
            errorHandler = "ListenerErrorHandler")*/

    /**
     * same as above but using completable future
     */
    public void listenTo2(@Payload CarPart carPart,
                          @Header(value = KafkaHeaders.RECEIVED_MESSAGE_KEY, required = false) String key,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            /*@Header(KafkaHeaders.RECEIVED_TIMESTAMP) long receivedTimestamp,*/
                          @Header(value = KAFKA_DL_RETRY_COUNT, defaultValue = "0", required = false) int retryCount,
                          Acknowledgment ack
    ) {

        log.info("Consuming topic {}, with key {}, with payload {}", topic, key, carPart);
        try {

            respository.save(CarPartEntity.fromModel(carPart));
        } catch (JDBCConnectionException jdbcConnectionEx) {
            /*
             * Connection error, maybe the db is down; so we will try to retry later
             * There is no point in going for the next message here since the consumer will fail again.
             * Therefore, we will try again latter
             */
            log.error("Connection Exception while trying to save record to the database. trying again in {}", nackIntervalMs);
            ack.nack(nackIntervalMs);
        } catch (Exception e) {
            /*
                Any other problems we are not ready to handle should go to deadletter. We are assuming that, since
                it's an unknown error we will not be able to resolve it just by retrying.
             */
            log.error("Unforeseen exception occurred {}, sending to deadletter topic: {}", e.getMessage(), topic + "-deadletter");
            sendToDeadLetterTopic(carPart, topic, e.getMessage(), retryCount).whenComplete((sendResult, throwable) -> {
                if (throwable != null) {
                    log.error("Error while sending to DL {}, will try again in {}", throwable, nackIntervalMs);
                    ack.nack(nackIntervalMs);
                } else {
                    log.info("Sent to DL queue!");
                    ack.acknowledge();
                }
            }).join();
            /*
                OR, using the ListenableFuture:
            sendToDeadLetterTopic(carPart, topic, e.getMessage(), retryCount).addCallback(new ListenableFutureCallback() {
                @Override
                public void onFailure(Throwable ex) {
                    log.error("Error while sending to DL {}, will try again in {}", ex, nackIntervalMs);
                    ack.nack(nackIntervalMs);
                }

                @Override
                public void onSuccess(SendResult<String, CarPart> result) {
                    ack.acknowledge();

                }
            });
            */

        }

    }

    private CompletableFuture<SendResult<String, CarPart>> sendToDeadLetterTopic(CarPart carPart, String topic, String message, int retryCount) {
        return CompletableFuture.supplyAsync(() -> {
            var record = new ProducerRecord<String, CarPart>(topic + "-deadletter", carPart);
            record.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, topic.getBytes());
            record.headers().add(KafkaHeaders.DLT_EXCEPTION_MESSAGE, message.getBytes());
            record.headers().add(KAFKA_DL_RETRY_COUNT, ByteBuffer.allocate(4).putInt(retryCount + 1).array());

            return record;
        }).thenComposeAsync(suppliedRecord -> kafkaProducer.send(suppliedRecord).completable());


    }
}
