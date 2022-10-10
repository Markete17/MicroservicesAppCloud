package com.app.productservice.models.dao;

import org.springframework.data.repository.CrudRepository;

import com.app.commonservice.models.entities.Product;

public interface ProductDAO extends CrudRepository<Product, Long> {

}
