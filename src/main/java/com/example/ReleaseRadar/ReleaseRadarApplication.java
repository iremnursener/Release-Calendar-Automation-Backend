package com.example.ReleaseRadar;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication

public class ReleaseRadarApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReleaseRadarApplication.class, args);
	}

	@Bean
	public OpenAPI api(
			@Value("${app.title}") String title,
			@Value("${app.description}") String desc,
			@Value("${app.version}") String ver) {
		return new OpenAPI().info(new Info()
				.title(title)
				.description(desc)
				.version(ver)
				.license(new License().name("ReleaseRadar API Licence")));
	}


  }
