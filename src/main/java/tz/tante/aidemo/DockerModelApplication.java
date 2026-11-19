package tz.tante.aidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class DockerModelApplication
{

	public static void main(String[] args) {
		SpringApplication.run(DockerModelApplication.class, args);
	}

}
