package com.demo.upimesh.controller;

import com.demo.upimesh.model.User;
import com.demo.upimesh.model.UserRepository;
import com.demo.upimesh.model.Wallet;
import com.demo.upimesh.model.WalletRepository;
import com.demo.upimesh.service.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String name = request.get("name");
        String mpin = request.get("mpin");

        if (phone == null || phone.isEmpty() || name == null || name.isEmpty() || mpin == null || mpin.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required."));
        }

        if (userRepository.findByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number already registered."));
        }

        // Create User
        String upiId = phone + "@swiftpay";
        User user = new User(name, phone, upiId, Instant.now().toEpochMilli());
        user = userRepository.save(user);

        // Create Wallet
        Wallet wallet = new Wallet(
                upiId,
                user.getId(),
                "SwiftPay Bank",
                HashUtil.sha256Hex(mpin),
                new BigDecimal("5000.00") // Initial demo balance
        );
        walletRepository.save(wallet);

        return ResponseEntity.ok(Map.of(
                "message", "Signup successful",
                "user", user
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String mpin = request.get("mpin");

        if (phone == null || phone.isEmpty() || mpin == null || mpin.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone and MPIN are required."));
        }

        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found."));
        }

        User user = userOpt.get();
        Optional<Wallet> walletOpt = walletRepository.findById(user.getUpiId());
        
        if (walletOpt.isEmpty() || !walletOpt.get().getMpinHash().equals(HashUtil.sha256Hex(mpin))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid MPIN."));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "token", "demo_jwt_token_" + user.getId(),
                "user", user
        ));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        // Mock OTP sending
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + request.get("phone"), "otp", "1234"));
    }
}
