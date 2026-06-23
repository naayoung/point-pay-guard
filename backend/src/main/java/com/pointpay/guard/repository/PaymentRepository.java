package com.pointpay.guard.repository;

import com.pointpay.guard.domain.payment.Payment;
import com.pointpay.guard.domain.payment.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("select p from Payment p join fetch p.order join fetch p.user where p.id = :id")
    Optional<Payment> findDetailById(@Param("id") Long id);

    @Query("select p from Payment p join fetch p.order join fetch p.user where p.idempotencyKey = :idempotencyKey")
    Optional<Payment> findDetailByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order join fetch p.user where p.id = :id")
    Optional<Payment> findWithLockById(@Param("id") Long id);

    Optional<Payment> findFirstByOrderIdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    List<Payment> findByStatus(PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order join fetch p.user where p.status = :status")
    List<Payment> findWithLockByStatus(@Param("status") PaymentStatus status);
}
