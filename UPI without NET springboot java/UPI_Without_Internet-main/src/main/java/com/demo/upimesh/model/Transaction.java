package com.demo.upimesh.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Permanent record of every settled transaction. Once written, never modified.
 * The packetHash is the idempotency key for MESH transactions — uniqueness is enforced at the DB level.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional for ONLINE transactions. Unique constraint can be handled manually or by DB if we make it unique conditionally.
    @Column(length = 64)
    private String packetHash; // SHA-256 hex of the encrypted packet (null for ONLINE)

    @Column(nullable = false, unique = true, length = 64)
    private String transactionId; // uuid

    @Column(nullable = false)
    private String senderVpa;

    @Column(nullable = false)
    private String receiverVpa;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant signedAt; // When the sender originally signed it (offline) or initiated it (online)

    @Column(nullable = false)
    private Instant settledAt; // When the backend actually processed it

    @Column
    private String bridgeNodeId; // Which mesh node finally delivered it (null for ONLINE)

    @Column
    private Integer hopCount; // How many devices it passed through (null for ONLINE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    public enum Status { SETTLED, REJECTED }
    public enum Type { ONLINE, MESH }

    public Transaction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPacketHash() { return packetHash; }
    public void setPacketHash(String packetHash) { this.packetHash = packetHash; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getSenderVpa() { return senderVpa; }
    public void setSenderVpa(String senderVpa) { this.senderVpa = senderVpa; }

    public String getReceiverVpa() { return receiverVpa; }
    public void setReceiverVpa(String receiverVpa) { this.receiverVpa = receiverVpa; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }

    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }

    public String getBridgeNodeId() { return bridgeNodeId; }
    public void setBridgeNodeId(String bridgeNodeId) { this.bridgeNodeId = bridgeNodeId; }

    public Integer getHopCount() { return hopCount; }
    public void setHopCount(Integer hopCount) { this.hopCount = hopCount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
