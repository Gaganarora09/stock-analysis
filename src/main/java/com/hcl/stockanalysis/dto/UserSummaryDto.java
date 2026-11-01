package com.hcl.stockanalysis.dto;

public class UserSummaryDto {

    private String username;
    private String role;
    private String email;

    public UserSummaryDto() {
    }

    public UserSummaryDto(String username, String role, String email) {
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
