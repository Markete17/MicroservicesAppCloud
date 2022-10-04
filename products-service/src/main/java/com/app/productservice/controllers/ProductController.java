package com.app.productservice.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.productservice.models.entities.Product;
import com.app.productservice.models.services.IProductService;

@RestController
@RequestMapping(path = "/products")
public class ProductController {
	
	@Autowired
	private IProductService productService;
	
	// Para obtener propiedades del archivo properties
	@Autowired
	private Environment environment;
	
	// Tambien se pueden obtener propiedades especificas con la anotacion @Value
	@Value("${server.port}")
	private Integer port;
	
	@GetMapping
	public ResponseEntity<List<Product>> getAllProducts(){
		return new ResponseEntity<List<Product>>(
				this.productService.findAll().stream().map(product -> {
					//product.setPort(Integer.parseInt(this.environment.getProperty("local.server.port")));
					product.setPort(this.port);
					return product;
				}).collect(Collectors.toList())
				,HttpStatus.OK);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Product> getProduct(@PathVariable Long id) {
		Product product = this.productService.findById(id);
		if(product != null ) {
			//product.setPort(this.port)
			product.setPort(Integer.parseInt(this.environment.getProperty("local.server.port")));
			return new ResponseEntity<>(product,HttpStatus.OK);
		} else {
			return new ResponseEntity<Product>(HttpStatus.NOT_FOUND);
		}
	}
}
