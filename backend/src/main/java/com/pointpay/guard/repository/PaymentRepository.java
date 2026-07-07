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

    /**
     * 상태별 결제 건수와 금액을 DB에서 그룹 집계한 결과를 서비스 계층에 전달한다.
     */
    interface PaymentStatusAggregate {

        PaymentStatus getStatus();

        Long getPaymentCount();

        Long getTotalAmount();
    }

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("select p from Payment p join fetch p.order join fetch p.user where p.id = :id")
    Optional<Payment> findDetailById(@Param("id") Long id);

    @Query("select p from Payment p join fetch p.order join fetch p.user where p.idempotencyKey = :idempotencyKey")
    Optional<Payment> findDetailByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order join fetch p.user where p.id = :id")
    Optional<Payment> findWithLockById(@Param("id") Long id);

    Optional<Payment> findFirstByOrderIdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    // 전체 결제 엔티티를 메모리에 올리지 않고 GROUP BY 쿼리 한 번으로 상태별 집계를 계산한다.
    @Query("""
            select p.status as status,
                   count(p) as paymentCount,
                   sum(p.amount) as totalAmount
            from Payment p
            group by p.status
            """)
    List<PaymentStatusAggregate> summarizeByStatus();

    List<Payment> findByStatus(PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order join fetch p.user where p.status = :status")
    List<Payment> findWithLockByStatus(@Param("status") PaymentStatus status);
}
