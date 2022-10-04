package com.app.itemservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

//Con la biblioteca de Spring Cloud Feign se pueden crear clientes de otra forma
//para consumir las apis. Es una alternativa al RestTemplate
@EnableFeignClients
//Configurar con Ribbon
@RibbonClient(name = "products-service")
@SpringBootApplication
public class ItemsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemsServiceApplication.class, args);
	}

}
