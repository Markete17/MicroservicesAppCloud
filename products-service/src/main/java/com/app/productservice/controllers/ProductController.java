package com.app.productservice.controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.commonservice.models.entities.Product;
import com.app.productservice.models.services.IProductService;

@RestController
//@RequestMapping(path = "/products") Ya no es necesario, se configura en el zuul-server (API Gateway) en properties
public class ProductController {

	@Autowired
	private IProductService productService;

	// Para obtener propiedades del archivo properties
	@Autowired
	private Environment environment;

	// Tambien se pueden obtener propiedades especificas con la anotacion @Value
	@Value("${server.port}")
	private Integer port;

	@GetMapping("/")
	public ResponseEntity<List<Product>> getAllProducts() {
		return new ResponseEntity<List<Product>>(this.productService.findAll().stream().map(product -> {
			product.setPort(Integer.parseInt(this.environment.getProperty("local.server.port")));
			//product.setPort(this.port);
			return product;
		}).collect(Collectors.toList()), HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Product> getProduct(@PathVariable Long id) throws InterruptedException {
		
		/** Para testear error con Resillence 4J Tasa de fallo*/
		if(id.equals(10L)) {
			throw new IllegalStateException("Product not found");
		}
		
		/** Para testear timeout con Resillence 4J*/
		if(id.equals(7L)) {
			TimeUnit.SECONDS.sleep(5L);
		}
		
		Product product = this.productService.findById(id);
		if (product != null) {
			// product.setPort(this.port)
			product.setPort(Integer.parseInt(this.environment.getProperty("local.server.port")));

			/** Para testear Hystrix con un fallo Runtime Exception **/
			/*
			 * boolean ok = false; if(!ok) { throw new
			 * RuntimeException("Cannot load this product."); }
			 */

			/** Para testear Timeout con Hystrix - Para esto es necesario configurarlo en el properties de item-service y si se usa Zuul Gateway tambien en sus properties **/
			 /*
			 try { Thread.sleep(2000L); } catch (InterruptedException e) {
			 e.printStackTrace(); }
			 */
			return new ResponseEntity<>(product, HttpStatus.OK);
		} else {
			return new ResponseEntity<Product>(HttpStatus.NOT_FOUND);
		}
	}
	
	@PostMapping("/create")
	@ResponseStatus(HttpStatus.CREATED)
	public Product create(@RequestBody Product product) {
		return this.productService.save(product);
	}
	
	@PutMapping("/update/{id}")
	@ResponseStatus(HttpStatus.CREATED)
	public Product update(@RequestBody Product product, @PathVariable Long id) {
		Product productDbProduct = this.productService.findById(id);	
		productDbProduct.setName(product.getName());
		productDbProduct.setPrice(product.getPrice());
		
		return this.productService.save(productDbProduct);
	}
	
	@DeleteMapping("/delete/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		this.productService.deleteById(id);
	}
}
