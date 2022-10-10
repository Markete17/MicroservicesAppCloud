package com.app.itemservice.models.services;

import java.util.List;

import com.app.commonservice.models.entities.Product;
import com.app.itemservice.models.Item;

public interface ItemService {
	
	public List<Item> findAll();
	public Item findById(Long id, Integer quantity);
	public Product save(Product product);
	public Product update(Product product, Long id);
	public void delete(Long id);

}
