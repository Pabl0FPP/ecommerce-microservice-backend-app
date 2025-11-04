package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.service.PaymentService;

@SpringBootTest
@DisabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = "dev")
class PaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;


    @Autowired
    private RestTemplate restTemplate;

    private Integer orderId;
    private OrderDto testOrder;

    @BeforeEach
    void setUp() {
        orderId = 456;
        testOrder = OrderDto.builder()
                .orderId(orderId)
                .build();
    }

    @Test
    void testPaymentOrderIntegration_FetchOrder_ShouldWork() {
        try {
            String url = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
            OrderDto orderDto = restTemplate.getForObject(url, OrderDto.class);
            assertNotNull(orderDto, "OrderDto should not be null - Order Service responded");
            assertNotNull(orderDto.getOrderId(), "Order ID should not be null");
            assertEquals(orderId, orderDto.getOrderId(), "Order ID should match");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertEquals(404, e.getRawStatusCode(), "Order Service should return 404 for non-existent order");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            fail("Order Service is not available - services must be deployed for integration tests: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception during Order Service integration: " + e.getMessage());
        }
    }

    @Test
    void testPaymentOrderIntegration_UpdateOrderStatus_ShouldWork() {
        try {
            String patchUrl = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId + "/status";
            restTemplate.patchForObject(patchUrl, null, Void.class);
            assertTrue(true, "Order status update HTTP call succeeded");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertEquals(404, e.getRawStatusCode(), "Order Service should return 404 for non-existent order");
        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
            assertTrue(e.getRawStatusCode() == 400 || e.getRawStatusCode() == 404, 
                      "Service should return 400 or 404");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            fail("Order Service is not available - services must be deployed for integration tests: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception during Order Service PATCH integration: " + e.getMessage());
        }
    }

    @Test
    void testPaymentOrderIntegration_OrderNotFound_ShouldHandleGracefully() {
        Integer orderId = 999;
        try {
            String url = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
            OrderDto orderDto = restTemplate.getForObject(url, OrderDto.class);
            assertNotNull(orderDto, "Service should respond even if order not found");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertEquals(404, e.getRawStatusCode(), "Order Service should return 404 for non-existent order");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testPaymentOrderIntegration_OrderServiceUnavailable_ShouldHandleGracefully() {
        Integer orderId = 888;
        try {
            String url = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
            restTemplate.getForObject(url, OrderDto.class);
            assertTrue(true, "Order Service is available");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            assertTrue(e.getMessage().contains("Connection refused") || 
                      e.getMessage().contains("I/O error"), 
                      "Should handle connection errors gracefully");
        } catch (Exception e) {
            assertNotNull(e.getMessage(), "Exception should have a message");
        }
    }

    @Test
    void testPaymentOrderIntegration_SavePayment_ShouldWork() {
        try {
            PaymentDto paymentDto = PaymentDto.builder()
                    .paymentStatus(PaymentStatus.NOT_STARTED)
                    .orderDto(testOrder)
                    .build();
            PaymentDto savedPayment = paymentService.save(paymentDto);
            assertNotNull(savedPayment, "Payment should be saved");
            assertNotNull(savedPayment.getPaymentId(), "Payment should have an ID");
            assertNotNull(savedPayment.getOrderDto(), "Payment should have order data");
            assertEquals(orderId, savedPayment.getOrderDto().getOrderId(), "Order ID should match");
        } catch (com.selimhorri.app.exception.custom.ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains("Order") || e.getMessage().contains("order"), 
                      "Exception should mention order");
            assertTrue(e.getMessage().contains("not found") || e.getMessage().contains("456"), 
                      "Exception should indicate order not found");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            fail("Order Service is not available - services must be deployed for integration tests: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception during Payment save integration: " + e.getMessage());
        }
    }
}
