package com.app.itemservice;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@Configuration
public class AppConfig {
	
	@Bean(name = "restClient")
	@LoadBalanced //Para que RestTemplate use Balanceo de carga con Ribbon
	public RestTemplate registerRestTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer(){
		return factory -> factory.configureDefault(id->{
			return new Resilience4JConfigBuilder(id)
					.circuitBreakerConfig(CircuitBreakerConfig.custom()
							.slidingWindowSize(10) //por defecto es 100
							.failureRateThreshold(50) //por defecto es 50% tambien
							.waitDurationInOpenState(Duration.ofSeconds(10)) // por defecto es 60000ms
							.permittedNumberOfCallsInHalfOpenState(5) //por defecto son 10
							.slowCallRateThreshold(50)//por defecto es 100%
							.slowCallDurationThreshold(Duration.ofSeconds(2L)) //por defecto 60000ms
							.build()) //si no es customizado no se necesita el build
					// TIMEOUTS
					.timeLimiterConfig(TimeLimiterConfig.custom()
							.timeoutDuration(Duration.ofSeconds(6L)) //1 por defecto
							.build())
					.build();
		});
	}

}
