package com.app.itemservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

//Con la biblioteca de Spring Cloud Feign se pueden crear clientes de otra forma
//para consumir las apis. Es una alternativa al RestTemplate
@EnableFeignClients
//Configurar con Ribbon. Ribbon viene implícito con Eureka
//@RibbonClient(name = "products-service")
@EnableEurekaClient // Se habilita que sea cliente del servidor Eureka
//@EnableCircuitBreaker // Para usar Hystrix para la tolerancia a fallos y timeouts
@SpringBootApplication
public class ItemsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemsServiceApplication.class, args);
	}

}
