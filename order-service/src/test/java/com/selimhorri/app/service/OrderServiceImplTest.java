package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.InvalidOrderStatusException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;

public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_missingCart_throwsInvalidInput() {
        OrderDto dto = OrderDto.builder().cartDto(null).build();
        assertThrows(InvalidInputException.class, () -> this.orderService.save(dto));
    }

    @Test
    void save_success_savesOrder() {
        OrderDto dto = OrderDto.builder().cartDto(com.selimhorri.app.dto.CartDto.builder().cartId(2).build()).build();
        when(this.cartRepository.findById(2)).thenReturn(Optional.of(Cart.builder().cartId(2).build()));
        when(this.orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = this.orderService.save(dto);
        assertNotNull(res);
    }

    @Test
    void findById_notFound_throws() {
        when(this.orderRepository.findByOrderIdAndIsActiveTrue(77)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> this.orderService.findById(77));
    }

    @Test
    void updateStatus_fromCreated_toOrdered() {
        Order order = Order.builder().orderId(1).status(OrderStatus.CREATED).cart(Cart.builder().cartId(2).build()).isActive(true).build();
        when(this.orderRepository.findByOrderIdAndIsActiveTrue(1)).thenReturn(Optional.of(order));
        when(this.orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = this.orderService.updateStatus(1);
        assertEquals(OrderStatus.ORDERED, res.getOrderStatus());
    }

    @Test
    void deleteById_inPayment_throwsInvalidOrderStatus() {
        Order order = Order.builder().orderId(3).status(OrderStatus.IN_PAYMENT).isActive(true).cart(Cart.builder().cartId(5).build()).build();
        when(this.orderRepository.findByOrderIdAndIsActiveTrue(3)).thenReturn(Optional.of(order));
        assertThrows(InvalidOrderStatusException.class, () -> this.orderService.deleteById(3));
    }

}
