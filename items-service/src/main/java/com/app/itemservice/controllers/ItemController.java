package com.app.itemservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.itemservice.models.Item;
import com.app.itemservice.models.services.ItemService;

@RestController
@RequestMapping(path = "/items")
public class ItemController {
	
	@Autowired
	@Qualifier("serviceFeign") //Usando Feign
	//@Qualifier("serviceRestTemplate") //Usando Rest Template
	private ItemService itemService;
	
	@GetMapping
	public ResponseEntity<List<Item>> getAllItems(){
		return new ResponseEntity<>(this.itemService.findAll(),HttpStatus.OK);
	}
	
	@GetMapping("/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem(@PathVariable Long id, @PathVariable Integer quantity){
		Item item = this.itemService.findById(id, quantity);
		if(item!=null) {
			return new ResponseEntity<Item>(item,HttpStatus.OK);
		} else {
			return new ResponseEntity<Item>(HttpStatus.NOT_FOUND);
		}
	}

}
