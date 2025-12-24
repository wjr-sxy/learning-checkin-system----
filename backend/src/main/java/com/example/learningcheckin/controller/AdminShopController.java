package com.example.learningcheckin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learningcheckin.annotation.Log;
import com.example.learningcheckin.common.Result;
import com.example.learningcheckin.entity.Order;
import com.example.learningcheckin.mapper.OrderMapper;
import com.example.learningcheckin.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/shop")
public class AdminShopController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private IProductService productService;

    @GetMapping("/orders")
    public Result<Page<Order>> listOrders(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) Long orderId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (orderId != null) {
            wrapper.eq(Order::getId, orderId);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/orders/{id}/refund")
    @Log(module = "商城运营", action = "退款")
    public Result<String> refundOrder(@PathVariable Long id) {
        try {
            productService.refundProduct(id);
            return Result.success("Refund successful");
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/orders/{id}/abnormal")
    @Log(module = "商城运营", action = "标记异常")
    public Result<String> markAbnormal(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error(404, "Order not found");
        }
        order.setIsAbnormal(true);
        orderMapper.updateById(order);
        return Result.success("Marked as abnormal");
    }

    @PostMapping("/orders/{id}/cancel-abnormal")
    @Log(module = "商城运营", action = "取消异常")
    public Result<String> cancelAbnormal(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error(404, "Order not found");
        }
        order.setIsAbnormal(false);
        orderMapper.updateById(order);
        return Result.success("Cancelled abnormal status");
    }

    @PostMapping("/orders/{id}/ship")
    @Log(module = "商城运营", action = "发货")
    public Result<String> shipOrder(@PathVariable Long id, @RequestBody Map<String, String> data) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error(404, "Order not found");
        }
        String trackingNumber = data.get("trackingNumber");
        if (trackingNumber == null || trackingNumber.isEmpty()) {
            return Result.error(400, "Tracking number is required");
        }
        order.setTrackingNumber(trackingNumber);
        order.setShippingStatus(1); // Shipped
        orderMapper.updateById(order);
        return Result.success("Order shipped");
    }
}
