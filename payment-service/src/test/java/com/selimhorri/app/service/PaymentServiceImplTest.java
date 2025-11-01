package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.InvalidPaymentStatusException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.impl.PaymentServiceImpl;
import org.springframework.web.client.RestTemplate;

public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_missingOrder_throwsInvalidInputException() {
        // avoid NPE on top-level logging which accesses getOrderDto().getOrderId()
        OrderDto emptyOrder = OrderDto.builder().orderId(null).build();
        PaymentDto dto = PaymentDto.builder().orderDto(emptyOrder).build();
        assertThrows(InvalidInputException.class, () -> this.paymentService.save(dto));
    }

    @Test
    void findById_notFound_throws() {
        when(this.paymentRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> this.paymentService.findById(999));
    }

    @Test
    void updateStatus_inProgress_to_completed() {
        Payment payment = Payment.builder().paymentId(2).paymentStatus(PaymentStatus.IN_PROGRESS).build();
        when(this.paymentRepository.findById(2)).thenReturn(Optional.of(payment));
        when(this.paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var res = this.paymentService.updateStatus(2);
        assertEquals(PaymentStatus.COMPLETED, res.getPaymentStatus());
    }

    @Test
    void deleteById_completed_throws() {
        Payment payment = Payment.builder().paymentId(5).paymentStatus(PaymentStatus.COMPLETED).build();
        when(this.paymentRepository.findById(5)).thenReturn(Optional.of(payment));
        assertThrows(InvalidPaymentStatusException.class, () -> this.paymentService.deleteById(5));
    }

    @Test
    void deleteById_success_setsCanceled() {
        Payment payment = Payment.builder().paymentId(6).paymentStatus(PaymentStatus.NOT_STARTED).build();
        when(this.paymentRepository.findById(6)).thenReturn(Optional.of(payment));
        when(this.paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> this.paymentService.deleteById(6));
        // verify saved with CANCELED
        verify(this.paymentRepository).save(argThat(p -> p.getPaymentStatus() == PaymentStatus.CANCELED));
    }

}
