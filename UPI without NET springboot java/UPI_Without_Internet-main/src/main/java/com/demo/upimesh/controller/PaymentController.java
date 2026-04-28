package com.demo.upimesh.controller;

import com.demo.upimesh.model.Transaction;
import com.demo.upimesh.model.TransactionRepository;
import com.demo.upimesh.model.Wallet;
import com.demo.upimesh.model.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("/pay/online")
    @Transactional
    public ResponseEntity<?> payOnline(@RequestBody Map<String, String> request) throws Exception {
        String senderUpiId = request.get("senderUpiId");
        String receiverUpiId = request.get("receiverUpiId");
        String amountStr = request.get("amount");
        String mpin = request.get("mpin");

        if (senderUpiId == null || receiverUpiId == null || amountStr == null || mpin == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }

        BigDecimal amount = new BigDecimal(amountStr);

        Wallet sender = walletRepository.findById(senderUpiId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));

        Wallet receiver = walletRepository.findById(receiverUpiId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (!sender.getMpinHash().equals(sha256Hex(mpin))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid MPIN"));
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance"));
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        walletRepository.save(sender);
        walletRepository.save(receiver);

        String transactionId = UUID.randomUUID().toString();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setSenderVpa(senderUpiId);
        tx.setReceiverVpa(receiverUpiId);
        tx.setAmount(amount);
        tx.setSignedAt(Instant.now());
        tx.setSettledAt(Instant.now());
        tx.setStatus(Transaction.Status.SETTLED);
        tx.setType(Transaction.Type.ONLINE);
        transactionRepository.save(tx);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "transactionId", transactionId
        ));
    }

    @GetMapping("/history/{upiId}")
    public ResponseEntity<?> getHistory(@PathVariable String upiId) {
        List<Transaction> userTxs = transactionRepository.findBySenderVpaOrReceiverVpaOrderByIdDesc(upiId, upiId);
        return ResponseEntity.ok(userTxs);
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
