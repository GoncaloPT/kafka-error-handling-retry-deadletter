package pt.goncalo.kafka.demo.deadletter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import pt.goncalo.kafka.demo.deadletter.kafkaconsumer.ListenerErrorHandler;

@SpringBootApplication
@EnableKafka
public class DeadletterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeadletterApplication.class, args);
    }

    /**
     * Register a default {@link RecordMessageConverter}
     *
     * @return
     */
    @Bean
    public RecordMessageConverter configureConverter() {
        return new StringJsonMessageConverter();
    }

    @Bean(name = "ListenerErrorHandler" )
    ListenerErrorHandler configureErrorHandler() {
        return new ListenerErrorHandler();
    }

}
