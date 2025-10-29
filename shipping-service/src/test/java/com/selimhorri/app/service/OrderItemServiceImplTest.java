package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.impl.OrderItemServiceImpl;

public class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository repo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderItemServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_whenExists_throwsDuplicate() {
        OrderItemDto dto = OrderItemDto.builder().productId(1).orderId(2).orderedQuantity(3).build();
        OrderItemId id = OrderItemMappingHelper.toId(dto);
        when(repo.existsById(id)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.save(dto));
    }

    @Test
    void findById_notFound_throwsResourceNotFound() {
        OrderItemId id = new OrderItemId(9, 8);
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void save_success_returnsDto() {
        OrderItemDto dto = OrderItemDto.builder().productId(7).orderId(11).orderedQuantity(1).build();
        OrderItem entity = OrderItemMappingHelper.map(dto);
        when(repo.existsById(OrderItemMappingHelper.toId(dto))).thenReturn(false);
        when(repo.save(any(OrderItem.class))).thenReturn(entity);

        OrderItemDto result = service.save(dto);
        assertNotNull(result);
        assertEquals(dto.getProductId(), result.getProductId());
        assertEquals(dto.getOrderId(), result.getOrderId());
    }
}
