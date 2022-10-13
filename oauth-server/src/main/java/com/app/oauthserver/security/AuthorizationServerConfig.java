package com.app.oauthserver.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableAuthorizationServer
@RefreshScope
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private InfoAdditionalToken infoAdditionalToken;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	/* Esto se utilizará cuando se quite el deprecated de Spring Security Config
	public AuthorizationServerConfig(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
	}*/

	/**
	 * Es el permiso que va a tener los endpoints en el servidor de autorización
	 */
	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		security.tokenKeyAccess("permitAll()") // permitAll() es un método de Spring Security para generar el token y autenticarse con oauth/login y que sea publico para que todo el mundo se pueda autenticar.
		.checkTokenAccess("isAuthenticated()"); // para validar el token, se llama al método isAuthenticated() que validará que el usuario esté autenticado.
	}

	/**
	 * Registrar los clientes, es decir, las aplicaciones frontend que consuman la app.
	 * Con OAuth2 va a haber doble autenticación. Autenticación con los usuarios del backend y autenticación
	 * con los clientes frontend mediante una password.
	 */
	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory()
		.withClient(this.env.getProperty("config.security.oauth.client.id")) //se asigna un nombre al cliente
		.secret(this.passwordEncoder.encode(this.env.getProperty("config.security.oauth.client.secret"))) //se asigna una contraseña
		.scopes("read", "write") //se establecen los permisos
		.authorizedGrantTypes("password", "refresh_token")
		.accessTokenValiditySeconds(3600); //Con lo que se hará la autenticación. Con refresh_token para obtener un token de acceso renovado justo antes que caduque el actual
		
		/** REGISTRAR OTRO CLIENTE
		 * .and().
		withClient("reactapp") //se asigna un nombre al cliente
		.secret(this.passwordEncoder.encode("12345")) //se asigna una contraseña
		.scopes("read", "write") //se establecen los permisos
		.authorizedGrantTypes("password", "refresh_token").accessTokenValiditySeconds(3600); 
		**/
	}

	/**
	 * Se puede configurar el tokenStorage, 
	 * tokenConverter para guardar información al token (claims)
	 * El accesTokenConverter se encarga de obtener esta información y condificar el token.
	 */
	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {

		// Para añadir información adicional al token
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		tokenEnhancerChain.setTokenEnhancers(Arrays.asList(infoAdditionalToken,accesTokenConverter()));
		// ----
		endpoints
		.authenticationManager(this.authenticationManager)
		.tokenStore(tokenStore())
		.accessTokenConverter(accesTokenConverter())
		.tokenEnhancer(tokenEnhancerChain);
	}

	@Bean
	public JwtTokenStore tokenStore() {
		return new JwtTokenStore(accesTokenConverter());
	}

	@Bean
	public JwtAccessTokenConverter accesTokenConverter() {
		JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
		
		// Firmar el token
		tokenConverter.setSigningKey(this.env.getProperty("config.security.oauth.jwt.key"));
		return tokenConverter;
	}
	
	/**
	 * ****************************************************************************
	 */
	
}
