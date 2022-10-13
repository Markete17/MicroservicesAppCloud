package com.app.oauthserver.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.app.commonservice.models.entities.User;
import com.app.oauthserver.clients.UserFeignClient;

import feign.FeignException;

@Service
public class UserService implements UserDetailsService,IUserService {

	@Autowired
	private UserFeignClient userFeignClient;
	
	private Logger logger = LoggerFactory.getLogger(UserService.class);
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		try {
			
		User user = this.userFeignClient.findByUsername(username);

		List<GrantedAuthority> authorities = user.getRoles().stream().map(
				role -> new SimpleGrantedAuthority(role.getName())
				)
				.peek(authority -> logger.info("Role: "+authority.getAuthority()))
				.collect(Collectors.toList());
		
		logger.info("Authenticated User: "+username);
		
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), user.getEnabled(),
				true, true, true, authorities);
		} catch (FeignException e) {
				logger.error("Error: username "+username+" not found.");
				throw new UsernameNotFoundException("Error: username "+username+" not found.");
		}
	}

	@Override
	public User findByUsername(String username) {
		return this.userFeignClient.findByUsername(username);
	}

	@Override
	public User update(User user, Long id) {
		return this.userFeignClient.update(user, id);
	}

}
