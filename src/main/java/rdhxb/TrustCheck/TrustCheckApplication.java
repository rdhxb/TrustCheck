package rdhxb.TrustCheck;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import rdhxb.TrustCheck.krs.KrsClient;
import rdhxb.TrustCheck.whiteList.WlClient;

@SpringBootApplication
public class TrustCheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrustCheckApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(KrsClient client){
		return args -> client.getData();
	}

}
