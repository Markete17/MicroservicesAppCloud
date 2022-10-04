package com.app.productservice.models.dao;

import org.springframework.data.repository.CrudRepository;

import com.app.productservice.models.entities.Product;

public interface ProductDAO extends CrudRepository<Product, Long> {

}
