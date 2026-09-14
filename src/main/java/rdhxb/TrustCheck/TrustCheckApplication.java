package rdhxb.TrustCheck;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import rdhxb.TrustCheck.krs.KrsClient;
import rdhxb.TrustCheck.sudop.SudopClient;
import rdhxb.TrustCheck.whiteList.WlClient;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrustCheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrustCheckApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(SudopClient sudopClient){
		return args -> sudopClient.getData();
	}

}
