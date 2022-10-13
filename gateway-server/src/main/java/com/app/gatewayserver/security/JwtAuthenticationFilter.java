package com.app.gatewayserver.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.google.common.net.HttpHeaders;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter{

	@Autowired
	private ReactiveAuthenticationManager authenticationManager;
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))//el token que viene de la cabecera http con justorempty se convierte en un Mono (objeto reactivo)
				.filter(authHeader -> authHeader.startsWith("Bearer "))//Si contiene Bearer se continua con el filtro
				.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))// Si no lo tiene se devuelve un Mono vacio
				.map(token -> token.replace("Bearer ", ""))// Se quita el Bearer
				.flatMap(token -> authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(null, token)))
				.flatMap(authentication -> chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)));
	}

}
