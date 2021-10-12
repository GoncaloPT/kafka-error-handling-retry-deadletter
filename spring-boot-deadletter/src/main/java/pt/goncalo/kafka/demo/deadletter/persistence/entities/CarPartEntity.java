package pt.goncalo.kafka.demo.deadletter.persistence.entities;

import lombok.Getter;
import lombok.Setter;
import pt.goncalo.kafka.demo.deadletter.kafkaconsumer.model.CarPart;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CAR_PART")
@Getter
@Setter
public class CarPartEntity {
    @Id
    private String id;
    @Column(length = 20)
    private String code;
    @Column()
    private String name;

    public CarPartEntity() {
    }

    public CarPartEntity(String id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    /**
     * Boundary converter
     *
     * @param carPart
     * @return
     */
    public static CarPartEntity fromModel(CarPart carPart) {
        return new CarPartEntity(carPart.getId(), carPart.getCode(), carPart.getName());
    }

}
