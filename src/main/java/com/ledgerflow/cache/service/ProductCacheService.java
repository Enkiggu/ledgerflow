package com.ledgerflow.cache.service;

import com.ledgerflow.cache.domain.Product;
import com.ledgerflow.cache.repository.ProductRepository;
import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);
    private final ProductRepository productRepository;

    public ProductCacheService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#productId", unless = "#result == null")
    @Transactional(readOnly = true)
    public Product getProductById(String productId) {
        log.debug("Cache miss for productId: {}. Fetching from PostgreSQL", productId);
        return productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found with id: " + productId));
    }

    @CacheEvict(value = "products", key = "#product.id")
    @Transactional
    public Product saveProduct(Product product) {
        log.info("Saving product and invalidating cache for productId: {}", product.getId());
        return productRepository.save(product);
    }
}
