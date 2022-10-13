package com.app.oauthserver.services;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.app.commonservice.models.entities.User;

public interface IUserService {
	
	public User findByUsername(String username);
	
	public User update(@RequestBody User user, @PathVariable Long id);

}
