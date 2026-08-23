package com.mindq.material.service;

import com.mindq.model.User;
import com.mindq.repository.StudyMaterialRepository;
import com.mindq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StudyMaterialRepository materialRepository;
    private final UserRepository userRepository;

    @Value("${app.storage.free-limit-bytes:524288000}")
    private long freeLimitBytes;

    @Value("${app.storage.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    public record StorageInfo(long usedBytes, long limitBytes, long remainingBytes, double usedPercentage, long maxFileSizeBytes) {}

    @Transactional(readOnly = true)
    public StorageInfo getStorageInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Single aggregate query — avoids loading all materials into memory
        long usedBytes = materialRepository.sumFileSizeBytesByUserId(user.getId());

        long remaining = Math.max(0, freeLimitBytes - usedBytes);
        double percentage = freeLimitBytes > 0 ? (usedBytes * 100.0 / freeLimitBytes) : 0;

        return new StorageInfo(usedBytes, freeLimitBytes, remaining, percentage, maxFileSizeBytes);
    }

    public boolean hasEnoughSpace(String email, long additionalBytes) {
        StorageInfo info = getStorageInfo(email);
        return info.remainingBytes() >= additionalBytes;
    }
}
