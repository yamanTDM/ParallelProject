package com.example.Parallel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


public class AuthDto {


    @Data
    public static class RegisterRequest {

        @NotBlank(message = "Name Required")
        private String name;

        @Email(message = "Email is invalid")
        @NotBlank(message = "Email Required")
        private String email;

        @NotBlank(message = "Password Required")
        @Size(min = 6, message = "Password must contain more than 6 characters")
        private String password;
    }


    @Data
    public static class LoginRequest {

        @Email(message = "Email is invalid")
        @NotBlank(message = "Email Required")
        private String email;

        @NotBlank(message = "Password Required")
        private String password;
    }


    @Data
    public static class AuthResponse {
        private String token;
        private String email;
        private String role;

        public AuthResponse(String token, String email, String role) {
            this.token = token;
            this.email = email;
            this.role = role;
        }
    }
}
