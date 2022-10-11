package com.app.userservice.models.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import com.app.commonservice.models.entities.User;

@RepositoryRestResource(path = "users") //Dependencia SpringWeb Rest Repositories
public interface UserDAO extends PagingAndSortingRepository<User, Long> {
	

	// Para acceder a estos métodos personalizados: /api/users/search/<metodo>?queryparams
	
	// Ejemplo: http://localhost:8090/api/users/users/search/findByUsername?username=admin
	// Para editar la ruta, está la anotación @RestResource
	
	@RestResource(path = "search-username")
	public User findByUsername(@Param("name")String username);
	
	@Query("select u from User u where u.username=?1 and u.email=?2") //utilizando jpa HQL
	//@Query(value = "select * from users u where u.username=?1 and u.email=?2", nativeQuery = true)
	public User getByUsernameAndEmail(String username, String email);
}
