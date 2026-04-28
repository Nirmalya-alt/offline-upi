package com.demo.upimesh.service;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.crypto.ServerKeyHolder;
import com.demo.upimesh.model.User;
import com.demo.upimesh.model.UserRepository;
import com.demo.upimesh.model.Wallet;
import com.demo.upimesh.model.WalletRepository;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

/**
 * Helper service that:
 *   - seeds demo accounts on startup
 *   - simulates "sender phone creates an encrypted packet" flow
 */
@Service
public class DemoService {

    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    @Autowired private UserRepository users;
    @Autowired private WalletRepository wallets;
    @Autowired private HybridCryptoService crypto;
    @Autowired private ServerKeyHolder serverKey;

    @PostConstruct
    public void seedAccounts() {
        if (users.count() == 0) {
            try {
                seedUser("Alice", "9000000001", "alice@meshpay", "5000.00");
                seedUser("Bob",   "9000000002", "bob@meshpay",   "1000.00");
                seedUser("Carol", "9000000003", "carol@meshpay", "2500.00");
                seedUser("Dave",  "9000000004", "dave@meshpay",  "500.00");
                log.info("Seeded 4 demo users and wallets");
            } catch (Exception e) {
                log.error("Failed to seed users", e);
            }
        }
    }

    private void seedUser(String name, String phone, String upiId, String balanceStr) throws Exception {
        User user = new User(name, phone, upiId, Instant.now().toEpochMilli());
        user = users.save(user);

        Wallet wallet = new Wallet(
                upiId,
                user.getId(),
                "Demo Bank",
                sha256Hex("1234"), // Default MPIN
                new BigDecimal(balanceStr)
        );
        wallets.save(wallet);
    }

    /**
     * Simulates the sender's phone:
     *   1. Build a PaymentInstruction with a fresh nonce + signedAt timestamp.
     *   2. Encrypt with the server's public key (hybrid RSA+AES).
     *   3. Wrap in a MeshPacket with TTL.
     *
     * In a real Android app, this exact code (minus the server-side reference)
     * would run on the phone. The phone would have already cached the server's
     * public key during a previous online session.
     */
    public MeshPacket createPacket(String senderVpa, String receiverVpa,
                                   BigDecimal amount, String pin, int ttl) throws Exception {
        PaymentInstruction instruction = new PaymentInstruction(
                senderVpa,
                receiverVpa,
                amount,
                sha256Hex(pin),
                UUID.randomUUID().toString(),       // nonce — guarantees uniqueness
                Instant.now().toEpochMilli()        // signedAt — for freshness check
        );

        String ciphertext = crypto.encrypt(instruction, serverKey.getPublicKey());

        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setTtl(ttl);
        packet.setCreatedAt(Instant.now().toEpochMilli());
        packet.setCiphertext(ciphertext);
        return packet;
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
