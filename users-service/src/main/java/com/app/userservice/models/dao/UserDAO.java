package com.app.userservice.models.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.app.userservice.models.entities.User;

@RepositoryRestResource(path = "users") //Dependencia SpringWeb Rest Repositories
public interface UserDAO extends PagingAndSortingRepository<User, Long> {

	public User findByUsername(String username);
	
	@Query("select u from User u where u.username=?1 and u.email=?2") //utilizando jpa HQL
	//@Query(value = "select * from users u where u.username=?1 and u.email=?2", nativeQuery = true)
	public User getByUsernameAndEmail(String username, String email);
}
