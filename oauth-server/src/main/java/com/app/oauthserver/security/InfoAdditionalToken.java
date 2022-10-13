package com.app.oauthserver.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import com.app.commonservice.models.entities.User;
import com.app.oauthserver.services.IUserService;

@Component
public class InfoAdditionalToken implements TokenEnhancer {
	
	@Autowired
	private IUserService userService;

	/**
	 * OAuth2Authentication authentication es el usuario autenticado
	 */
	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		Map<String, Object> info = new HashMap<>();
		
		User user = this.userService.findByUsername(authentication.getName());
		info.put("firstName", user.getFirstName());
		info.put("lastName", user.getLastName());
		info.put("email", user.getEmail());
		
		((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(info);
		return accessToken;
	}

}
