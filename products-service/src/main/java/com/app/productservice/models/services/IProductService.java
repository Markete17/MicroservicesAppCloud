package com.app.productservice.models.services;

import java.util.List;

import com.app.productservice.models.entities.Product;

public interface IProductService {
	
	public List<Product> findAll();
	public Product findById(Long id);

}
