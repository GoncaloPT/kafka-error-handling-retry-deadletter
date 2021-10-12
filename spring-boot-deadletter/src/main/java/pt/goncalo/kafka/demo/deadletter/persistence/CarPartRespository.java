package pt.goncalo.kafka.demo.deadletter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.goncalo.kafka.demo.deadletter.kafkaconsumer.model.CarPart;
import pt.goncalo.kafka.demo.deadletter.persistence.entities.CarPartEntity;

@Repository
public interface CarPartRespository extends JpaRepository<CarPartEntity,String> {
}
