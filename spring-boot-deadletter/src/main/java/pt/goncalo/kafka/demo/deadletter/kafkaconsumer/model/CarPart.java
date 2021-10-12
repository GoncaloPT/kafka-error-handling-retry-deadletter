package pt.goncalo.kafka.demo.deadletter.kafkaconsumer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CarPart {
    private String id;
    private String name;
    private String code;
}
