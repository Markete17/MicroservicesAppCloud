package com.app.gatewayserver.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity // Habilitar la seguridad en Web Flux. Es una clase que no implementa nada
//Con esta anotación, se va a tener una anotación Bean para configurar las rutas de seguridad.
public class SpringSecurityConfig {
	
	@Autowired
	private JwtAuthenticationFilter authenticationFilter;
	
	@Bean
	public SecurityWebFilterChain configure(ServerHttpSecurity http) {
		return http.authorizeExchange()
				.pathMatchers("/api/security/oauth/**").permitAll()
				.pathMatchers(HttpMethod.GET,
						"/api/products/",
						"/api/items/",
						"/api/users/users/",
						"/api/items/{id}/quantity/{quantity}",
						"/api/products/{id}").permitAll()
				.pathMatchers(HttpMethod.GET,"/api/users/users/{id}").hasAnyRole("ADMIN","USER")
				.pathMatchers("/api/products/**","/api/items/**","/api/users/users/**").hasRole("ADMIN")
				.anyExchange()
				.authenticated()
				.and()
				.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.csrf().disable()
				.build();
		
	}

}