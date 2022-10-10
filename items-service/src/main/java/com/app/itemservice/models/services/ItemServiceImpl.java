package com.app.itemservice.models.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.app.itemservice.models.Item;
import com.app.commonservice.models.entities.Product;

@Service("serviceRestTemplate")
public class ItemServiceImpl implements ItemService {
	
	@Autowired
	private RestTemplate restClient;

	// IMPORTANTE! Es mejor usar la biblioteca Feign para crear un cliente que use la api de productos
	//Esto en vez de hacerlo con REST TEMPLATE, se puede hacer mediante interfaces con la bliblioteca Spring Feign
	//Ir a ItemServiceFeign que usa ClientRestProduct
	
	@Override
	public List<Item> findAll() {
		// Al usar RestTemplate con balanceo de carga, ya no es necesario indicar la URL ya que Ribbon elegirá el puerto mejor disponible
		
		//List<Product> products = Arrays.asList(this.restClient.getForObject("http://localhost:8001/products", Product[].class));
		
		// Ahora hay que elegir el servicio
		List<Product> products = Arrays.asList(this.restClient.getForObject("http://products-service/", Product[].class));
		return products.stream().map(
				p -> new Item(p,1)
				).collect(Collectors.toList());
	}

	@Override
	public Item findById(Long id, Integer quantity) {
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		
		//Mismo motivo que en findAll
		//Product product = this.restClient.getForObject("http://localhost:8001/products/{id}", Product.class, pathVariablesMap);
		Product product = this.restClient.getForObject("http://products-service/{id}", Product.class, pathVariablesMap);
		
		return new Item(product,quantity);
	}

	@Override
	public Product save(Product product) {
		
		HttpEntity<Product> body = new HttpEntity<Product>(product);
		
		//Con el método exchange se indica la url, el método y el tipo de objeto que se devuelve en el body. Hay que pasarle el body un HttpEntity que sera el objeto producto.
		ResponseEntity<Product> responseEntity = this.restClient.exchange("http://products-service/create", HttpMethod.POST,body,Product.class);
		Product productResponse=responseEntity.getBody();
		return productResponse;
	}

	@Override
	public Product update(Product product, Long id) {
		
		HttpEntity<Product> body = new HttpEntity<Product>(product);
		
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		
		ResponseEntity<Product> responseEntity = this.restClient.exchange("http://products-service/update/{id}", 
				HttpMethod.PUT,body,Product.class, pathVariablesMap);
		return responseEntity.getBody();
	}

	@Override
	public void delete(Long id) {
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		
		this.restClient.delete("http://products-service/delete/{id}", pathVariablesMap);
	}

}
