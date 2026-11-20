package tz.tante.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class TanteAiApplication
{

	public static void main(String[] args) {
		SpringApplication.run(TanteAiApplication.class, args);
	}

}
