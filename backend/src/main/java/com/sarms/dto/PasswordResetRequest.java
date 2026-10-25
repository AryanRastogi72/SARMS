package com.sarms.dto;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String userId;
    private String email;        // must match the email on file (solo security fix after fork)
    private String newPassword;
}
