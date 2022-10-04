package com.app.itemservice.models.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.app.itemservice.models.Item;
import com.app.itemservice.models.Product;

@Service("serviceRestTemplate")
public class ItemServiceImpl implements ItemService {
	
	@Autowired
	private RestTemplate restClient;

	// IMPORTANTE! Es mejor usar la biblioteca Feign para crear un cliente que use la api de productos
	//Esto en vez de hacerlo con REST TEMPLATE, se puede hacer mediante interfaces con la bliblioteca Spring Feign
	//Ir a ItemServiceFeign que usa ClientRestProduct
	
	@Override
	public List<Item> findAll() {
		List<Product> products = Arrays.asList(this.restClient.getForObject("http://localhost:8001/products", Product[].class));
		return products.stream().map(
				p -> new Item(p,1)
				).collect(Collectors.toList());
	}

	@Override
	public Item findById(Long id, Integer quantity) {
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		Product product = this.restClient.getForObject("http://localhost:8001/products/{id}", Product.class, pathVariablesMap);
		return new Item(product,quantity);
	}

}
