package com.demo.upimesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private String upiId; // Primary key, e.g. "9876543210@meshpay"

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String mpinHash;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal offlineLockedBalance = BigDecimal.ZERO;

    @Version
    private Long version;

    public Wallet() {}

    public Wallet(String upiId, Long userId, String bankName, String mpinHash, BigDecimal balance) {
        this.upiId = upiId;
        this.userId = userId;
        this.bankName = bankName;
        this.mpinHash = mpinHash;
        this.balance = balance;
        this.offlineLockedBalance = BigDecimal.ZERO;
    }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getMpinHash() { return mpinHash; }
    public void setMpinHash(String mpinHash) { this.mpinHash = mpinHash; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getOfflineLockedBalance() { return offlineLockedBalance; }
    public void setOfflineLockedBalance(BigDecimal offlineLockedBalance) { this.offlineLockedBalance = offlineLockedBalance; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
