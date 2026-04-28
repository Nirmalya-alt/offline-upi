package com.demo.upimesh.controller;

import com.demo.upimesh.model.User;
import com.demo.upimesh.model.UserRepository;
import com.demo.upimesh.model.Wallet;
import com.demo.upimesh.model.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/link")
    public ResponseEntity<?> linkBank(@RequestBody Map<String, String> request) throws Exception {
        String phone = request.get("phone");
        String bankName = request.get("bankName");
        String mpin = request.get("mpin");

        if (phone == null || bankName == null || mpin == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Wallet already linked"));
        }

        Wallet wallet = new Wallet(
                user.getUpiId(),
                user.getId(),
                bankName,
                sha256Hex(mpin),
                new BigDecimal("10000.00") // 10k mock balance
        );
        wallet = walletRepository.save(wallet);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "walletId", "w_" + wallet.getUserId(),
                "balance", wallet.getBalance()
        ));
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
