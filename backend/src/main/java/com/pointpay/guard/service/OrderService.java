package com.pointpay.guard.service;

import com.pointpay.guard.api.dto.CreateOrderRequest;
import com.pointpay.guard.api.dto.OrderResponse;
import com.pointpay.guard.domain.order.PointOrder;
import com.pointpay.guard.domain.user.PointUser;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.ResourceNotFoundException;
import com.pointpay.guard.repository.OrderRepository;
import com.pointpay.guard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        PointUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + request.userId()
                ));
        PointOrder order = orderRepository.save(new PointOrder(user, request.amount()));
        return OrderResponse.from(order);
    }
}
