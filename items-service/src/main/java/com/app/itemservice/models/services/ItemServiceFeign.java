package com.app.itemservice.models.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.itemservice.clients.ClientRestProduct;
import com.app.itemservice.models.Item;
import com.app.itemservice.models.Product;

@Service("serviceFeign")
//@Primary //Para que use por defecto esta primero
//Como alternativa al Primary, utilizar los qualifiers poniendo debajo del autowired, el nombre especifico (ir a ItemController)
public class ItemServiceFeign implements ItemService {
	
	@Autowired
	private ClientRestProduct clientRestProduct;

	@Override
	public List<Item> findAll() {
		ResponseEntity<List<Product>> response = this.clientRestProduct.getAllProducts();
		System.out.println(response);
		return this.clientRestProduct.getAllProducts().getBody().stream().map(
				p -> new Item(p,1)
				).collect(Collectors.toList());
		
	}

	@Override
	public Item findById(Long id, Integer quantity) {
		Product product = this.clientRestProduct.getProduct(id).getBody();
		return new Item(product,quantity);
	}

}
