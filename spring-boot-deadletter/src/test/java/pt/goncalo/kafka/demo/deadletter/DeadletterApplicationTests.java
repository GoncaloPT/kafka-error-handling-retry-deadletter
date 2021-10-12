package pt.goncalo.kafka.demo.deadletter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;

@SpringBootTest
class DeadletterApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void dummyTest(){
		CompletableFuture<String> a = new CompletableFuture<>();
		a.whenComplete((value,ex) -> System.out.println(">>>>>> HELLO!!!! "+value)).join();
		System.out.println(">>>>>> finished");
	}

}
