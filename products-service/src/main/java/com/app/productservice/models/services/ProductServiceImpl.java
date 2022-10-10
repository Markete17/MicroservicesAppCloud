package com.app.productservice.models.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.commonservice.models.entities.Product;
import com.app.productservice.models.dao.ProductDAO;

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

	@Override
	@Transactional
	public Product save(Product product) {
		return this.productDAO.save(product);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		this.productDAO.deleteById(id);
		
	}

}
