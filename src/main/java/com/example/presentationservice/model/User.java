package com.example.presentationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String userName;
    private String lastName;
    private String login;
    private String password;
    private Boolean isUserBlock;
    private UserRole userRole;
}
