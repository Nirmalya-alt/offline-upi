package com.demo.upimesh.service;

import com.demo.upimesh.model.Wallet;
import com.demo.upimesh.model.WalletRepository;
import com.demo.upimesh.model.PaymentInstruction;
import com.demo.upimesh.model.Transaction;
import com.demo.upimesh.model.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Where the actual ledger update happens. Wrapped in a DB transaction so either
 * BOTH the debit and credit happen, or neither does.
 *
 * The @Version column on Wallet gives us optimistic locking — if two threads
 * somehow get past idempotency and both try to debit the same wallet, the
 * second one will fail with OptimisticLockException rather than corrupting
 * the balance. (In a demo the idempotency layer should always catch this first,
 * but defense in depth.)
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    @Autowired private WalletRepository wallets;
    @Autowired private TransactionRepository transactions;

    @Transactional
    public Transaction settle(PaymentInstruction instruction, String packetHash,
                              String bridgeNodeId, int hopCount) {

        Wallet sender = wallets.findById(instruction.getSenderVpa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown sender VPA: " + instruction.getSenderVpa()));

        Wallet receiver = wallets.findById(instruction.getReceiverVpa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown receiver VPA: " + instruction.getReceiverVpa()));

        BigDecimal amount = instruction.getAmount();
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!sender.getMpinHash().equals(instruction.getPinHash())) {
            log.warn("Invalid MPIN for sender {}: received {}, expected {}", 
                    sender.getUpiId(), instruction.getPinHash(), sender.getMpinHash());
            return recordRejected(instruction, packetHash, bridgeNodeId, hopCount);
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance: {} has ₹{}, tried to send ₹{}",
                    sender.getUpiId(), sender.getBalance(), amount);
            return recordRejected(instruction, packetHash, bridgeNodeId, hopCount);
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        wallets.save(sender);
        wallets.save(receiver);

        Transaction tx = new Transaction();
        tx.setTransactionId(java.util.UUID.randomUUID().toString());
        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.getSenderVpa());
        tx.setReceiverVpa(instruction.getReceiverVpa());
        tx.setAmount(amount);
        tx.setSignedAt(Instant.ofEpochMilli(instruction.getSignedAt()));
        tx.setSettledAt(Instant.now());
        tx.setBridgeNodeId(bridgeNodeId);
        tx.setHopCount(hopCount);
        tx.setStatus(Transaction.Status.SETTLED);
        tx.setType(Transaction.Type.MESH);
        transactions.save(tx);

        log.info("SETTLED ₹{} from {} to {} (packetHash={}, bridge={}, hops={})",
                amount, sender.getUpiId(), receiver.getUpiId(),
                packetHash.substring(0, 12) + "...", bridgeNodeId, hopCount);

        return tx;
    }

    @Transactional
    public Transaction onlineTransfer(String senderVpa, String receiverVpa, BigDecimal amount, String pin) {
        Wallet sender = wallets.findById(senderVpa)
                .orElseThrow(() -> new IllegalArgumentException("Unknown sender VPA: " + senderVpa));

        Wallet receiver = wallets.findById(receiverVpa)
                .orElseThrow(() -> new IllegalArgumentException("Unknown receiver VPA: " + receiverVpa));

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!sender.getMpinHash().equals(HashUtil.sha256Hex(pin))) {
            throw new IllegalArgumentException("Invalid MPIN");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        wallets.save(sender);
        wallets.save(receiver);

        Transaction tx = new Transaction();
        tx.setTransactionId(java.util.UUID.randomUUID().toString());
        tx.setSenderVpa(senderVpa);
        tx.setReceiverVpa(receiverVpa);
        tx.setAmount(amount);
        tx.setSignedAt(Instant.now());
        tx.setSettledAt(Instant.now());
        tx.setStatus(Transaction.Status.SETTLED);
        tx.setType(Transaction.Type.ONLINE);
        return transactions.save(tx);
    }

    private Transaction recordRejected(PaymentInstruction instruction, String packetHash,
                                       String bridgeNodeId, int hopCount) {
        Transaction tx = new Transaction();
        tx.setTransactionId(java.util.UUID.randomUUID().toString());
        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.getSenderVpa());
        tx.setReceiverVpa(instruction.getReceiverVpa());
        tx.setAmount(instruction.getAmount());
        tx.setSignedAt(Instant.ofEpochMilli(instruction.getSignedAt()));
        tx.setSettledAt(Instant.now());
        tx.setBridgeNodeId(bridgeNodeId);
        tx.setHopCount(hopCount);
        tx.setStatus(Transaction.Status.REJECTED);
        tx.setType(Transaction.Type.MESH);
        return transactions.save(tx);
    }
}
