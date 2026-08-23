package com.mindq.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStats {
    private long totalUsers;
    private long activeUsers;
    private long totalMaterials;
    private long totalQuizzes;
    private long totalAiGenerations;
    private long totalRevenue;
    private long storageUsedBytes;
}
