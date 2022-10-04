package com.app.productservice.models.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.productservice.models.dao.ProductDAO;
import com.app.productservice.models.entities.Product;

@Service
public class ProductServiceImpl implements IProductService {
	
	@Autowired
	private ProductDAO productDAO;

	@Override
	@Transactional(readOnly = true)
	public List<Product> findAll() {
		return (List<Product>) this.productDAO.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Product findById(Long id) {
		return this.productDAO.findById(id).orElse(null);
	}

}
