package com.app.itemservice.models.services;

import java.util.List;

import com.app.itemservice.models.Item;

public interface ItemService {
	
	public List<Item> findAll();
	public Item findById(Long id, Integer quantity);

}
