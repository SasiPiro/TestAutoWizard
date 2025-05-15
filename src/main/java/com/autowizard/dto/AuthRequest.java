package com.autowizard.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
} 