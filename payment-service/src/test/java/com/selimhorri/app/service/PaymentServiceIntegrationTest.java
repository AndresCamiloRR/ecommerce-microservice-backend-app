package com.selimhorri.app.service;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private PaymentDto paymentDto;
    private Payment payment;

    private static final Integer DEFAULT_ORDER_ID = 1;
    private static final Boolean DEFAULT_IS_PAYED = false;
    private static final PaymentStatus DEFAULT_PAYMENT_STATUS = PaymentStatus.NOT_STARTED;
    private static final PaymentStatus UPDATED_PAYMENT_STATUS = PaymentStatus.COMPLETED;
    private static final Boolean UPDATED_IS_PAYED = true;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        OrderDto orderDto = OrderDto.builder()
                .orderId(DEFAULT_ORDER_ID)
                .orderDate(LocalDateTime.now().minusDays(1))
                .orderDesc("Test Order Description")
                .orderFee(100.50)
                .build();

        paymentDto = PaymentDto.builder()
                .isPayed(DEFAULT_IS_PAYED)
                .paymentStatus(DEFAULT_PAYMENT_STATUS)
                .orderDto(orderDto)
                .build();

        // Entidad Payment para comparaciones directas con el repositorio
        payment = Payment.builder()
                .orderId(DEFAULT_ORDER_ID)
                .isPayed(DEFAULT_IS_PAYED)
                .paymentStatus(DEFAULT_PAYMENT_STATUS)
                .build();
    }

    @Test
    void savePayment_shouldPersistPayment() {
        PaymentDto savedPaymentDto = paymentService.save(paymentDto);

        assertThat(savedPaymentDto).isNotNull();
        assertThat(savedPaymentDto.getPaymentId()).isNotNull(); // El ID debe generarse al guardar
        assertThat(savedPaymentDto.getIsPayed()).isEqualTo(DEFAULT_IS_PAYED);
        assertThat(savedPaymentDto.getPaymentStatus()).isEqualTo(DEFAULT_PAYMENT_STATUS);
        assertThat(savedPaymentDto.getOrderDto().getOrderId()).isEqualTo(DEFAULT_ORDER_ID);

        Optional<Payment> repoPayment = paymentRepository.findById(savedPaymentDto.getPaymentId());
        assertThat(repoPayment).isPresent();
        assertThat(repoPayment.get().getIsPayed()).isEqualTo(DEFAULT_IS_PAYED);
        assertThat(repoPayment.get().getPaymentStatus()).isEqualTo(DEFAULT_PAYMENT_STATUS);
        assertThat(repoPayment.get().getOrderId()).isEqualTo(DEFAULT_ORDER_ID);
    }

    @Test
    void updatePayment_shouldModifyExistingPayment() {
        Payment savedEntity = paymentRepository.saveAndFlush(payment); // Guardar entidad base

        PaymentDto dtoToUpdate = PaymentDto.builder()
                .paymentId(savedEntity.getPaymentId())
                .isPayed(UPDATED_IS_PAYED)
                .paymentStatus(UPDATED_PAYMENT_STATUS)
                .orderDto(OrderDto.builder().orderId(savedEntity.getOrderId()).build()) // Mantener el orderId
                .build();

        PaymentDto updatedDto = paymentService.update(dtoToUpdate);

        assertThat(updatedDto).isNotNull();
        assertThat(updatedDto.getPaymentId()).isEqualTo(savedEntity.getPaymentId());
        assertThat(updatedDto.getIsPayed()).isEqualTo(UPDATED_IS_PAYED);
        assertThat(updatedDto.getPaymentStatus()).isEqualTo(UPDATED_PAYMENT_STATUS);

        Optional<Payment> repoPayment = paymentRepository.findById(savedEntity.getPaymentId());
        assertThat(repoPayment).isPresent();
        assertThat(repoPayment.get().getIsPayed()).isEqualTo(UPDATED_IS_PAYED);
        assertThat(repoPayment.get().getPaymentStatus()).isEqualTo(UPDATED_PAYMENT_STATUS);
    }

    @Test
    void deletePaymentById_shouldRemovePaymentFromDatabase() {
        Payment savedEntity = paymentRepository.saveAndFlush(payment);
        Integer paymentIdToDelete = savedEntity.getPaymentId();

        assertThat(paymentRepository.findById(paymentIdToDelete)).isPresent(); // Verificar que existe antes de borrar

        paymentService.deleteById(paymentIdToDelete);

        assertThat(paymentRepository.findById(paymentIdToDelete)).isNotPresent();
    }

    @Test
    void findPaymentById_whenNotExists_shouldThrowException() {
        Integer nonExistentId = -99;
        // Asumiendo que el servicio lanza una excepción específica o genérica cuando no encuentra la entidad.
        // Esto dependerá de la implementación de PaymentServiceImpl.
        // Por ejemplo, si lanza JpaObjectRetrievalFailureException o una custom.
        // Aquí usaremos una genérica para el ejemplo, ajustar según la implementación real.
        assertThrows(Exception.class, () -> {
            paymentService.findById(nonExistentId);
        });
    }

}
