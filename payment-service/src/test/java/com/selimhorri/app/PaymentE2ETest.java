package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.service.PaymentService;

/**
 * Tests E2E (End-to-End) para Payment Service
 * Estos tests validan flujos completos de usuario que involucran múltiples servicios
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = "dev")
class PaymentE2ETest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    private String userServiceUrl;
    private String orderServiceUrl;

    @BeforeEach
    void setUp() {
        userServiceUrl = System.getProperty("user.service.url", 
            AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL);
        orderServiceUrl = System.getProperty("order.service.url", 
            AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL);
    }

    /**
     * Test E2E: Flujo completo de pago
     * Usuario → Orden → Crear pago → Actualizar estado de pago → Verificar estado
     */
    @Test
    void testE2E_CompletePaymentFlow() {
        try {
            ResponseEntity<Object> userResponse = restTemplate.postForEntity(
                userServiceUrl, 
                new HttpEntity<>(new java.util.HashMap<>()), 
                Object.class
            );
        } catch (Exception e) {
        }
        OrderDto orderDto = null;
        try {
            java.util.Map<String, Object> orderData = new java.util.HashMap<>();
            orderData.put("orderDesc", "E2E Payment Test Order");
            orderData.put("orderFee", 199.99);

            ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                orderServiceUrl, 
                new HttpEntity<>(orderData), 
                OrderDto.class
            );

            if (orderResponse.getStatusCode() == HttpStatus.CREATED || 
                orderResponse.getStatusCode() == HttpStatus.OK) {
                OrderDto createdOrder = orderResponse.getBody();
                if (createdOrder != null && createdOrder.getOrderId() != null) {
                    try {
                        restTemplate.exchange(
                            orderServiceUrl + "/" + createdOrder.getOrderId() + "/status",
                            HttpMethod.PATCH,
                            null,
                            OrderDto.class
                        );
                        ResponseEntity<OrderDto> updatedOrderResponse = restTemplate.getForEntity(
                            orderServiceUrl + "/" + createdOrder.getOrderId(),
                            OrderDto.class
                        );
                        if (updatedOrderResponse.getStatusCode() == HttpStatus.OK) {
                            orderDto = updatedOrderResponse.getBody();
                        } else {
                            orderDto = createdOrder;
                        }
                    } catch (Exception e2) {
                        orderDto = createdOrder;
                    }
                } else {
                    orderDto = createdOrder;
                }
            } else {
                fail("No se pudo crear una orden válida para el test E2E. El servicio de orden debe estar disponible.");
            }
        } catch (Exception e) {
            fail("El servicio de orden debe estar disponible para ejecutar tests E2E de pago. " +
                 "Error: " + e.getMessage());
        }

        assertNotNull(orderDto, "Order should be available");
        assertNotNull(orderDto.getOrderId(), "Order should have an ID");

        PaymentDto paymentDto = PaymentDto.builder()
                .orderDto(OrderDto.builder().orderId(orderDto.getOrderId()).build())
                .isPayed(false)
                .build();

        PaymentDto createdPayment = paymentService.save(paymentDto);
        assertNotNull(createdPayment, "Payment should be created");
        assertNotNull(createdPayment.getPaymentId(), "Payment should have an ID");
        assertNotNull(createdPayment.getOrderDto(), "Payment should be linked to order");
        assertEquals(orderDto.getOrderId(), createdPayment.getOrderDto().getOrderId(), 
                "Payment should be linked to correct order");
        assertFalse(createdPayment.getIsPayed(), "Payment should initially not be paid");

        PaymentDto updatedPayment = paymentService.updateStatus(createdPayment.getPaymentId());
        assertNotNull(updatedPayment, "Payment should be updated");
        assertEquals(createdPayment.getPaymentId(), updatedPayment.getPaymentId(), 
                "Payment ID should remain the same");
        assertNotNull(updatedPayment.getPaymentStatus(), "Payment should have a status");

        PaymentDto retrievedPayment = paymentService.findById(createdPayment.getPaymentId());
        assertNotNull(retrievedPayment, "Payment should be retrievable");
        assertEquals(createdPayment.getPaymentId(), retrievedPayment.getPaymentId(), 
                "Payment IDs should match");
        assertEquals(updatedPayment.getPaymentStatus(), retrievedPayment.getPaymentStatus(), 
                "Payment status should be updated");

        var allPayments = paymentService.findAll();
        assertNotNull(allPayments, "Payments list should not be null");
        assertTrue(allPayments.size() > 0, "Should have at least one payment");

        boolean paymentFound = allPayments.stream()
                .anyMatch(p -> p.getPaymentId().equals(createdPayment.getPaymentId()));
        assertTrue(paymentFound, "Created payment should be in the list");
        assertNotNull(createdPayment.getPaymentId(), "Payment should have been created");
        assertNotNull(updatedPayment.getPaymentStatus(), "Payment status should have been updated");
        assertNotNull(retrievedPayment, "Payment should be retrievable");
    }

    /**
     * Test E2E: Flujo de múltiples pagos
     * Usuario → Múltiples órdenes → Crear múltiples pagos → Verificar todos
     */
    @Test
    void testE2E_MultiplePaymentsFlow() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        Integer userId = 1;
        OrderDto[] orders = new OrderDto[3];
        try {
            for (int i = 0; i < 3; i++) {
                java.util.Map<String, Object> orderData = new java.util.HashMap<>();
                orderData.put("orderDesc", "E2E Payment Test Order " + (i + 1));
                orderData.put("orderFee", (i + 1) * 50.0);

                ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                    orderServiceUrl, 
                    new HttpEntity<>(orderData), 
                    OrderDto.class
                );

                if (orderResponse.getStatusCode() == HttpStatus.CREATED || 
                    orderResponse.getStatusCode() == HttpStatus.OK) {
                    OrderDto createdOrder = orderResponse.getBody();
                    if (createdOrder != null && createdOrder.getOrderId() != null) {
                        try {
                            restTemplate.exchange(
                            orderServiceUrl + "/" + createdOrder.getOrderId() + "/status",
                            HttpMethod.PATCH,
                            null,
                            OrderDto.class
                        );
                            ResponseEntity<OrderDto> updatedOrderResponse = restTemplate.getForEntity(
                                orderServiceUrl + "/" + createdOrder.getOrderId(),
                                OrderDto.class
                            );
                            if (updatedOrderResponse.getStatusCode() == HttpStatus.OK) {
                                orders[i] = updatedOrderResponse.getBody();
                            } else {
                                orders[i] = createdOrder;
                            }
                        } catch (Exception e2) {
                            orders[i] = createdOrder;
                        }
                    } else {
                        fail("No se pudo crear orden " + (i + 1) + " para el test E2E");
                    }
                } else {
                    fail("No se pudo crear orden " + (i + 1) + " para el test E2E");
                }
            }
        } catch (Exception e) {
            fail("El servicio de orden debe estar disponible para ejecutar este test E2E. " +
                 "Error: " + e.getMessage());
        }

        PaymentDto[] createdPayments = new PaymentDto[3];
        for (int i = 0; i < 3; i++) {
            PaymentDto paymentDto = PaymentDto.builder()
                    .orderDto(OrderDto.builder().orderId(orders[i].getOrderId()).build())
                    .isPayed(false)
                    .build();

            createdPayments[i] = paymentService.save(paymentDto);
            assertNotNull(createdPayments[i], "Payment " + (i + 1) + " should be created");
            assertNotNull(createdPayments[i].getPaymentId(), 
                    "Payment " + (i + 1) + " should have an ID");
            assertEquals(orders[i].getOrderId(), 
                    createdPayments[i].getOrderDto().getOrderId(), 
                    "Payment " + (i + 1) + " should be linked to order");
        }

        var allPayments = paymentService.findAll();
        assertNotNull(allPayments, "Payments list should not be null");
        assertTrue(allPayments.size() >= 3, "Should have at least 3 payments");

        for (int i = 0; i < 3; i++) {
            final int index = i;
            boolean found = allPayments.stream()
                    .anyMatch(p -> p.getPaymentId().equals(createdPayments[index].getPaymentId()));
            assertTrue(found, "Payment " + (i + 1) + " should be in the list");
        }
        for (int i = 0; i < 3; i++) {
            PaymentDto updatedPayment = paymentService.updateStatus(
                    createdPayments[i].getPaymentId());
            assertNotNull(updatedPayment, "Payment " + (i + 1) + " should be updated");
            assertNotNull(updatedPayment.getPaymentStatus(), 
                    "Payment " + (i + 1) + " should have a status");
        }
        assertEquals(3, createdPayments.length, "Should have created 3 payments");
        assertTrue(allPayments.size() >= 3, "Should have at least 3 payments in list");
    }

    /**
     * Test E2E: Flujo de cancelación de pago
     * Usuario → Orden → Crear pago → Cancelar pago → Verificar cancelación
     */
    @Test
    void testE2E_PaymentCancellationFlow() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        Integer userId = 1;
        OrderDto orderDto = null;
        try {
            java.util.Map<String, Object> orderData = new java.util.HashMap<>();
            orderData.put("orderDesc", "E2E Payment Cancellation Test Order");
            orderData.put("orderFee", 299.99);

            ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                orderServiceUrl, 
                new HttpEntity<>(orderData), 
                OrderDto.class
            );

            if (orderResponse.getStatusCode() == HttpStatus.CREATED || 
                orderResponse.getStatusCode() == HttpStatus.OK) {
                OrderDto createdOrder = orderResponse.getBody();
                if (createdOrder != null && createdOrder.getOrderId() != null) {
                    try {
                        restTemplate.exchange(
                            orderServiceUrl + "/" + createdOrder.getOrderId() + "/status",
                            HttpMethod.PATCH,
                            null,
                            OrderDto.class
                        );
                        ResponseEntity<OrderDto> updatedOrderResponse = restTemplate.getForEntity(
                            orderServiceUrl + "/" + createdOrder.getOrderId(),
                            OrderDto.class
                        );
                        if (updatedOrderResponse.getStatusCode() == HttpStatus.OK) {
                            orderDto = updatedOrderResponse.getBody();
                        } else {
                            orderDto = createdOrder;
                        }
                    } catch (Exception e2) {
                        orderDto = createdOrder;
                    }
                } else {
                    fail("No se pudo crear orden para el test E2E");
                }
            } else {
                fail("No se pudo crear orden para el test E2E");
            }
        } catch (Exception e) {
            fail("El servicio de orden debe estar disponible para ejecutar este test E2E. " +
                 "Error: " + e.getMessage());
        }

        PaymentDto paymentDto = PaymentDto.builder()
                .orderDto(OrderDto.builder().orderId(orderDto.getOrderId()).build())
                .isPayed(false)
                .build();

        PaymentDto createdPayment = paymentService.save(paymentDto);
        assertNotNull(createdPayment, "Payment should be created");
        assertNotNull(createdPayment.getPaymentId(), "Payment should have an ID");

        PaymentDto retrievedPayment = paymentService.findById(createdPayment.getPaymentId());
        assertNotNull(retrievedPayment, "Payment should be retrievable");
        assertEquals(createdPayment.getPaymentId(), retrievedPayment.getPaymentId(), 
                "Payment IDs should match");

        paymentService.deleteById(createdPayment.getPaymentId());
        try {
            paymentService.findById(createdPayment.getPaymentId());
            fail("Payment should have been deleted");
        } catch (com.selimhorri.app.exception.custom.ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains("Payment") || 
                      e.getMessage().contains("payment") || 
                      e.getMessage().contains("not found"),
                      "Exception should indicate payment not found");
        }
        assertNotNull(createdPayment.getPaymentId(), "Payment should have been created");
    }

    /**
     * Test E2E: Flujo de actualización de estado de pago
     * Usuario → Orden → Crear pago → Actualizar estado múltiples veces → Verificar transiciones
     */
    @Test
    void testE2E_PaymentStatusTransitionFlow() {
        OrderDto orderDto = null;
        try {
            java.util.Map<String, Object> orderData = new java.util.HashMap<>();
            orderData.put("orderDesc", "E2E Payment Status Transition Test Order");
            orderData.put("orderFee", 399.99);

            ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                orderServiceUrl, 
                new HttpEntity<>(orderData), 
                OrderDto.class
            );

            if (orderResponse.getStatusCode() == HttpStatus.CREATED || 
                orderResponse.getStatusCode() == HttpStatus.OK) {
                OrderDto createdOrder = orderResponse.getBody();
                if (createdOrder != null && createdOrder.getOrderId() != null) {
                    try {
                        restTemplate.exchange(
                            orderServiceUrl + "/" + createdOrder.getOrderId() + "/status",
                            HttpMethod.PATCH,
                            null,
                            OrderDto.class
                        );
                        ResponseEntity<OrderDto> updatedOrderResponse = restTemplate.getForEntity(
                            orderServiceUrl + "/" + createdOrder.getOrderId(),
                            OrderDto.class
                        );
                        if (updatedOrderResponse.getStatusCode() == HttpStatus.OK) {
                            orderDto = updatedOrderResponse.getBody();
                        } else {
                            orderDto = createdOrder;
                        }
                    } catch (Exception e2) {
                        orderDto = createdOrder;
                    }
                } else {
                    fail("No se pudo crear orden para el test E2E");
                }
            } else {
                fail("No se pudo crear orden para el test E2E");
            }
        } catch (Exception e) {
            fail("El servicio de orden debe estar disponible para ejecutar este test E2E. " +
                 "Error: " + e.getMessage());
        }
        PaymentDto paymentDto = PaymentDto.builder()
                .orderDto(OrderDto.builder().orderId(orderDto.getOrderId()).build())
                .isPayed(false)
                .build();

        PaymentDto createdPayment = paymentService.save(paymentDto);
        assertNotNull(createdPayment, "Payment should be created");
        assertNotNull(createdPayment.getPaymentId(), "Payment should have an ID");

        PaymentDto firstUpdate = paymentService.updateStatus(createdPayment.getPaymentId());
        assertNotNull(firstUpdate, "Payment should be updated");
        assertNotNull(firstUpdate.getPaymentStatus(), "Payment should have a status");

        PaymentDto secondUpdate = paymentService.updateStatus(createdPayment.getPaymentId());
        assertNotNull(secondUpdate, "Payment should be updated again");
        assertNotNull(secondUpdate.getPaymentStatus(), "Payment should have a status");

        PaymentDto finalPayment = paymentService.findById(createdPayment.getPaymentId());
        assertNotNull(finalPayment, "Payment should still be retrievable");
        assertEquals(createdPayment.getPaymentId(), finalPayment.getPaymentId(), 
                "Payment ID should remain the same");
        assertNotNull(finalPayment.getPaymentStatus(), "Payment should have a final status");

        assertNotNull(createdPayment.getPaymentId(), "Payment should have been created");
        assertNotNull(firstUpdate.getPaymentStatus(), "First update should have status");
        assertNotNull(secondUpdate.getPaymentStatus(), "Second update should have status");
        assertNotNull(finalPayment.getPaymentStatus(), "Final payment should have status");
    }

    /**
     * Test E2E: Pago sobre orden en estado CREATED debe fallar con INVALID_ORDER_STATUS
     */
    @Test
    void testE2E_PaymentOnCreatedOrder_ShouldFail() {
        OrderDto orderDto;
        try {
            java.util.Map<String, Object> orderData = new java.util.HashMap<>();
            orderData.put("orderDesc", "E2E Payment Created Status Test Order");
            orderData.put("orderFee", 120.00);

            ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
                orderServiceUrl, 
                new HttpEntity<>(orderData), 
                OrderDto.class
            );

            if (orderResponse.getStatusCode() == HttpStatus.CREATED || 
                orderResponse.getStatusCode() == HttpStatus.OK) {
                orderDto = orderResponse.getBody();
            } else {
                fail("No se pudo crear la orden para el test negativo de pago");
                return;
            }
        } catch (Exception e) {
            fail("Servicio de orden requerido para el test: " + e.getMessage());
            return;
        }

        assertNotNull(orderDto);
        assertNotNull(orderDto.getOrderId());

        PaymentDto paymentDto = PaymentDto.builder()
                .orderDto(OrderDto.builder().orderId(orderDto.getOrderId()).build())
                .isPayed(false)
                .build();

        assertThrows(com.selimhorri.app.exception.custom.InvalidInputException.class,
                () -> paymentService.save(paymentDto));
    }
}

