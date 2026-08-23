package com.mindq.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private String planCode;
    private long materialCount;
    private long quizCount;
    private LocalDateTime createdAt;
}
