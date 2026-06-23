package com.pointpay.guard.repository;

import com.pointpay.guard.domain.user.PointUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<PointUser, Long> {
}
