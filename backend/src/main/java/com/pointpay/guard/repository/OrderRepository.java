package com.pointpay.guard.repository;

import com.pointpay.guard.domain.order.PointOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<PointOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PointOrder o join fetch o.user where o.id = :id")
    Optional<PointOrder> findWithLockById(@Param("id") Long id);
}
