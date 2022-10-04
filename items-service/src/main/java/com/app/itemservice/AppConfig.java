package com.app.itemservice;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
	
	@Bean(name = "restClient")
	@LoadBalanced //Para que RestClient use Balanceo de carga con Ribbon
	public RestTemplate registerRestTemplate() {
		return new RestTemplate();
	}

}
