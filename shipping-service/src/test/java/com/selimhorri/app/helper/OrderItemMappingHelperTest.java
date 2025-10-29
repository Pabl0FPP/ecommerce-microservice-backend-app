package com.selimhorri.app.helper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderItemDto;

public class OrderItemMappingHelperTest {

    @Test
    void mapEntityNull_returnsNull() {
        assertNull(OrderItemMappingHelper.map((OrderItem) null));
    }

    @Test
    void toIdFromDto_andEntity_roundtrip() {
        OrderItemDto dto = OrderItemDto.builder().productId(5).orderId(10).orderedQuantity(2).build();
        OrderItemId idFromDto = OrderItemMappingHelper.toId(dto);
        assertNotNull(idFromDto);
        assertEquals(5, idFromDto.getProductId());
        assertEquals(10, idFromDto.getOrderId());

        OrderItem entity = OrderItemMappingHelper.map(dto);
        OrderItemId idFromEntity = OrderItemMappingHelper.toId(entity);
        assertNotNull(idFromEntity);
        assertEquals(idFromDto, idFromEntity);
    }
}
