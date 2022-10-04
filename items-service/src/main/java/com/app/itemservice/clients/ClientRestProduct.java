package com.app.itemservice.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.itemservice.models.Product;

@FeignClient(name = "products-service", url = "localhost:8001")
public interface ClientRestProduct {
	
	@GetMapping("/products")
	public ResponseEntity<List<Product>> getAllProducts();
	
	@GetMapping("/products/{id}")
	public ResponseEntity<Product> getProduct(@PathVariable Long id);

}
