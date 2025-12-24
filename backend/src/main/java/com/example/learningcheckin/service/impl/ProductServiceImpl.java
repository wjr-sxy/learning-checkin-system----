package com.example.learningcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.learningcheckin.entity.Order;
import com.example.learningcheckin.entity.PointsRecord;
import com.example.learningcheckin.entity.Product;
import com.example.learningcheckin.entity.User;
import com.example.learningcheckin.mapper.OrderMapper;
import com.example.learningcheckin.mapper.PointsRecordMapper;
import com.example.learningcheckin.mapper.ProductMapper;
import com.example.learningcheckin.mapper.UserMapper;
import com.example.learningcheckin.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Override
    public List<Product> getProducts() {
        return this.list();
    }

    @Override
    public List<Product> getOwnedProducts(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, 0) // Only valid orders
        );
        
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> productIds = orders.stream()
                .map(Order::getProductId)
                .distinct()
                .collect(Collectors.toList());
                
        return this.listByIds(productIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeProduct(Long userId, Long productId, java.util.Map<String, String> shippingInfo) {
        Product product = this.getById(productId);
        if (product == null || product.getStock() <= 0) {
            throw new RuntimeException("Product not found or out of stock");
        }

        User user = userMapper.selectById(userId);
        if (user.getPoints() < product.getPrice()) {
            throw new RuntimeException("Insufficient points");
        }

        // 1. Deduct Points
        user.setPoints(user.getPoints() - product.getPrice());
        userMapper.updateById(user);

        // 2. Update Stock
        product.setStock(product.getStock() - 1);
        this.updateById(product);

        // 3. Create Order
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setPrice(product.getPrice());
        order.setCreateTime(LocalDateTime.now());
        
        // Handle Shipping Info for Physical Items
        if ("PHYSICAL".equalsIgnoreCase(product.getType()) && shippingInfo != null) {
            order.setShippingAddress(shippingInfo.get("address"));
            order.setReceiverName(shippingInfo.get("name"));
            order.setReceiverPhone(shippingInfo.get("phone"));
            order.setShippingStatus(0); // Pending
        }
        
        orderMapper.insert(order);

        // 4. Record Points History
        PointsRecord pr = new PointsRecord();
        pr.setUserId(userId);
        pr.setType(2); // Consume
        pr.setAmount(product.getPrice());
        pr.setDescription("Exchange Product: " + product.getName());
        pr.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(pr);
    }

    @Override
    public List<Order> getExchangeHistory(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundProduct(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getStatus() != null && order.getStatus() == 1) {
            throw new RuntimeException("Order already refunded");
        }

        User user = userMapper.selectById(order.getUserId());
        Product product = this.getById(order.getProductId());
        
        // 1. Return Points
        user.setPoints(user.getPoints() + order.getPrice());
        userMapper.updateById(user);

        // 2. Restore Stock
        if (product != null) {
            product.setStock(product.getStock() + 1);
            this.updateById(product);
        }

        // 3. Update Order Status
        order.setStatus(1);
        orderMapper.updateById(order);

        // 4. Record Points History
        PointsRecord pr = new PointsRecord();
        pr.setUserId(user.getId());
        pr.setType(1); // Gain
        pr.setAmount(order.getPrice());
        pr.setDescription("Refund: " + (product != null ? product.getName() : "Unknown Product"));
        pr.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(pr);
    }
}
