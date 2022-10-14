package com.app.oauthserver.security.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.app.commonservice.models.entities.User;
import com.app.oauthserver.services.IUserService;

import brave.Tracer;
import feign.FeignException;

@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher {

	private Logger logger = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);

	@Autowired
	private IUserService userService;
	
	@Autowired
	private Tracer tracer;

	@Override
	public void publishAuthenticationSuccess(Authentication authentication) {

		// Si la autenticación es la del cliente: postmanapp no se devuelve nada

		/*
		 * if(authentication.getName().equalsIgnoreCase("postmanapp")) { return; }
		 */

		/* Se puede hacer de dos formas */
		if (authentication.getDetails() instanceof WebAuthenticationDetails) {
			return;
		}

		UserDetails user = (UserDetails) authentication.getPrincipal();

		String successMessage = "Success Login: " + user.getUsername();
		System.out.println(successMessage);
		logger.info(successMessage);
		
		User loginUser = this.userService.findByUsername(authentication.getName());
		if(loginUser.getAttempts() != null && loginUser.getAttempts() > 0) {
			loginUser.setAttempts(0);
			this.userService.update(loginUser, loginUser.getId());
		}
	}

	@Override
	public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
		String errorMessage = "Login Error: " + exception.getMessage();
		System.out.println(errorMessage);
		logger.info(errorMessage);

		try {
			
			StringBuilder errors = new StringBuilder();
			errors.append("- "+errorMessage);
			
			User user = this.userService.findByUsername(authentication.getName());
			if (user.getAttempts() == null) {
				user.setAttempts(0);
			}
			
			logger.info("Actual attemps: "+user.getAttempts());
			
			user.setAttempts(user.getAttempts()+1);
			
			logger.info("Then attemps: "+user.getAttempts());
			
			errors.append("- "+"Then attemps: "+user.getAttempts());
			
			
			if(user.getAttempts()>=3) {
				errors.append("- " + String.format("User %s disabled due to max attemps" , user.getUsername()));
				logger.error(String.format("User %s disabled due to max attemps" , user.getUsername()));
				user.setEnabled(false);
			}
			
			this.userService.update(user, user.getId());
			
			tracer.currentSpan().tag("error.message", errors.toString());
		} catch (FeignException e) {
			logger.error(String.format("User %s does not exist", authentication.getName()));
		}
	}

}
