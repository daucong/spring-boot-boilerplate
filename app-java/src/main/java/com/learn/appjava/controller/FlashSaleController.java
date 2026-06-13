package com.learn.appjava.controller;

import com.learn.appjava.model.Order;
import com.learn.appjava.model.Product;
import com.learn.appjava.repository.ProductRepository;
import com.learn.appjava.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final ProductRepository productRepository;

    // Gọi trước khi mở sale để init tồn kho vào Redis
    @PostMapping("/init/{productId}")
    public ResponseEntity<Void> initStock(@PathVariable Long productId) {
        flashSaleService.initStock(productId);
        return ResponseEntity.ok().build();
    }

    // User đặt hàng flash sale
    @PostMapping("/order")
    public ResponseEntity<Order> placeOrder(
            @RequestParam Long productId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(flashSaleService.placeOrder(productId, userId));
    }

    @PostMapping("/product")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productRepository.save(product));
    }
}