package pt.goncalo.kafka.demo.deadletter.kafkaconsumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Slf4j
public class ListenerErrorHandler implements KafkaListenerErrorHandler {
    @SendTo()
    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
        String topicName = (String)message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC);
        log.warn("error handler called. In here the message should be sent to DL ");
        return message;
    }

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) {
        String topicName = (String)message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC);
        log.warn("error handler called. In here the message should be sent to DL ");
        return message;
    }
}
