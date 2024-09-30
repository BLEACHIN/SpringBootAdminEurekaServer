package com.bleachin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
@EnableAdminServer
public class SpcloudEurekaSpbootAdmin8761Application {
	public static void main(String[] args) {
		SpringApplication.run(SpcloudEurekaSpbootAdmin8761Application.class, args);
	}
}
