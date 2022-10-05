package com.app.itemservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.app.itemservice.models.Item;
import com.app.itemservice.models.Product;
import com.app.itemservice.models.services.ItemService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
//@RequestMapping(path = "/items") Ya no es necesario, se configura en el zuul-server (API Gateway) en properties
public class ItemController {

	@Autowired
	// @Qualifier("serviceRestTemplate") //Usando Rest Template
	@Qualifier("serviceFeign") // Usando Feign
	private ItemService itemService;

	@GetMapping
	public ResponseEntity<List<Item>> getAllItems() {
		return new ResponseEntity<>(this.itemService.findAll(), HttpStatus.OK);
	}

	@HystrixCommand(fallbackMethod = "alternativeMethod")
	@GetMapping("/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem(@PathVariable Long id, @PathVariable Integer quantity) {
		Item item = this.itemService.findById(id, quantity);
		if (item != null) {
			return new ResponseEntity<Item>(item, HttpStatus.OK);
		} else {
			return new ResponseEntity<Item>(HttpStatus.NOT_FOUND);
		}
	}

	public ResponseEntity<Item> alternativeMethod(@PathVariable Long id, @PathVariable Integer quantity) {
		Item item = new Item();
		Product product = new Product();

		product.setId(id);
		product.setName("Sony Camera");
		product.setPrice(500.00);

		item.setProduct(product);
		item.setQuantity(quantity);
		return new ResponseEntity<Item>(item, HttpStatus.OK);
	}

}
