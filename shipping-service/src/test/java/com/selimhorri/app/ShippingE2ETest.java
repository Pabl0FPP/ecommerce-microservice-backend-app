package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.service.OrderItemService;

@SpringBootTest
@DisabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = "dev")
class ShippingE2ETest {

    @Autowired
    private OrderItemService orderItemService;

    @Test
    void testE2E_ListAll_ShouldReturnNonNull() {
        List<OrderItemDto> list = orderItemService.findAll();
        assertNotNull(list);
    }

    @Test
    void testE2E_SaveMissingFields_ShouldFail() {
        OrderItemDto dto = OrderItemDto.builder()
                .orderedQuantity(1)
                .build();
        assertThrows(com.selimhorri.app.exception.custom.InvalidInputException.class,
                () -> orderItemService.save(dto));
    }

    @Test
    void testE2E_FindById_NotFound_ShouldFail() {
        OrderItemId id = new OrderItemId(999999, 888888);
        assertThrows(com.selimhorri.app.exception.custom.ResourceNotFoundException.class,
                () -> orderItemService.findById(id));
    }

    @Test
    void testE2E_UpdateNonExisting_ShouldFail() {
        OrderItemDto dto = OrderItemDto.builder()
                .productId(111111)
                .orderId(222222)
                .orderedQuantity(2)
                .build();
        assertThrows(com.selimhorri.app.exception.custom.ResourceNotFoundException.class,
                () -> orderItemService.update(dto));
    }

    @Test
    void testE2E_DeleteNonExisting_ShouldFail() {
        OrderItemId id = new OrderItemId(12345, 54321);
        assertThrows(com.selimhorri.app.exception.custom.ResourceNotFoundException.class,
                () -> orderItemService.deleteById(id));
    }
}


