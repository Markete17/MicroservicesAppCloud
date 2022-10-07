package com.app.gatewayserver.filters;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class GlobalFilterExample implements GlobalFilter, Ordered {

	private final Logger logger = LoggerFactory.getLogger(GlobalFilterExample.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		/*
		 * chain -> cadena exchange -> contiene la respuesta y request chain.filter ->
		 * se continua con la request al microservicio y con el then se hace una vez
		 * obtenida la respuesta.
		 * 
		 * Mono.fromRunnable crea un objeto reactivo para implementar la tarea después
		 * del método filter(exchange) fromRunnable para que sea una función lambda
		 * 
		 * TODO lo que está antes del return es el PRE y lo de dentro del Then es el
		 * POST
		 */

		logger.info("Execute PRE Filter");

		/* MODIFICAR LA REQUEST */
		exchange.getRequest().mutate().headers(h -> {
			h.add("token", "123456");
		});

		return chain.filter(exchange).then(Mono.fromRunnable(() -> {
			logger.info("Execute POST Filter");

			/* MODIFICAR LA RESPONSE */

			// Ejemplo para añadir una cookie a la respuesta
			exchange.getResponse().getCookies().add("color", ResponseCookie.from("color", "red").build());
			// Ejemplo para transformar la respuesta en Texto plano
			//exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);

			/* MANIPULAR la request que hemos modificado */
			// getFirst para obtener por nombre
			Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("token"))
					.ifPresent(value -> exchange.getResponse().getHeaders().add("token", value));

		}));
	}

	@Override
	public int getOrder() {
		return 100; // con -1 es de lectura y no se va a poder guardar datos.
	}

}