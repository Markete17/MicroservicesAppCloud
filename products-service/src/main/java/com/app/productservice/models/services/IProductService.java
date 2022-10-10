package com.app.productservice.models.services;

import java.util.List;

import com.app.commonservice.models.entities.Product;

public interface IProductService {
	
	public List<Product> findAll();
	public Product findById(Long id);
	public Product save(Product product);
	public void deleteById(Long id);

}
