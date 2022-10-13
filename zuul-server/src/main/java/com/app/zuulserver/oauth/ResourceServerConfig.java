package com.app.zuulserver.oauth;

import java.util.Arrays;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableResourceServer
@RefreshScope
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
	
	@Value("${config.security.oauth.jwt.key}")
	private String jwtKey;

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.tokenStore(tokenStore());
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/api/security/oauth/token").permitAll()
		.antMatchers(HttpMethod.GET,"/api/products/","api/items/","/api/users/users/").permitAll()
		.antMatchers(HttpMethod.GET,"/api/products/{id}",
				"/api/items/{id}/quantity/{quantity}",
				"/api/users/users/{id}")
				.hasAnyRole("ADMIN","USER")
		//.antMatchers(HttpMethod.POST,"/api/products/create","/api/items/create","/api/users/users").hasRole("ADMIN")
		//.antMatchers(HttpMethod.PUT, "/api/products/update/{id}", "/api/items/update/{id}","/api/users/users/{id}").hasRole("ADMIN")
		//.antMatchers(HttpMethod.DELETE, "/api/products/delete/{id}", "/api/items/delete/{id}","/api/users/users/{id}").hasRole("ADMIN");
		// Se puede simplificar de la siguiente manera:
		.antMatchers("/api/products/**","/api/items/**","/api/users/users/**").hasRole("ADMIN")
		.anyRequest().authenticated()
		.and().cors().configurationSource(corsConfigurationSource());
	}
	
	/**
	 * Permite que una aplicación cliente (react,angular) se conecte al backend
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin("*"); // Con el asterisco se permite cualquier origen, si solo se quiere permitir una lista de origenes, es el método setAllowedOrigins("http://localhost:4090",...)
		corsConfiguration.setAllowedMethods(Arrays.asList("POST","GET","PUT","DELETE","OPTIONS")); // OPTIONS es importante ya que Oauth2 lo utiliza
		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		
		source.registerCorsConfiguration("/**", corsConfiguration); // ** para aplicarlo a todas las rutas
		return source;
	}
	
	/**
	 * Para configurarlo a nivel global como un filtro
	 */
	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(){
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(corsConfigurationSource()));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE); //MAXIMA Prioridad
		return bean;
	}

	@Bean
	public JwtTokenStore tokenStore() {
		return new JwtTokenStore(accesTokenConverter());
	}

	@Bean
	public JwtAccessTokenConverter accesTokenConverter() {
		JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
		
		// Firmar el token
		tokenConverter.setSigningKey(Base64.getEncoder().encodeToString(jwtKey.getBytes()));
		return tokenConverter;
	}

}
