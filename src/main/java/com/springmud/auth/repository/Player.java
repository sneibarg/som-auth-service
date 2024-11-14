package com.springmud.auth.repository;

import lombok.Data;

import java.util.List;

@Data
public class Player {
    private String firstName;
    private String lastName;
    private String accountName;
    private String emailAddress;
    private String password;
    private List<String> playerCharacterList;
}
