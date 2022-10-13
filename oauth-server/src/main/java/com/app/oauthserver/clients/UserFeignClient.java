package com.app.oauthserver.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.commonservice.models.entities.User;

@FeignClient(name = "users-service")
public interface UserFeignClient {
	
	@GetMapping("/users/search/search-username")
	public User findByUsername(@RequestParam String name);
	
	@PutMapping("/users/{id}")
	public User update(@RequestBody User user, @PathVariable Long id);

}
