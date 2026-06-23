package com.pointpay.guard.domain.wallet;

import com.pointpay.guard.domain.user.PointUser;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallets_user_id", columnNames = "user_id")
        }
)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private PointUser user;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    protected Wallet() {
    }

    public Wallet(PointUser user, Long balance) {
        this.user = user;
        this.balance = balance;
    }

    public void withdraw(Long amount) {
        if (balance < amount) {
            throw new InsufficientBalanceException(
                    ErrorCode.INSUFFICIENT_BALANCE,
                    "포인트 잔액이 부족합니다. balance=" + balance + ", amount=" + amount
            );
        }
        this.balance -= amount;
    }

    public void deposit(Long amount) {
        this.balance += amount;
    }

    public Long getId() {
        return id;
    }

    public PointUser getUser() {
        return user;
    }

    public Long getBalance() {
        return balance;
    }
}
