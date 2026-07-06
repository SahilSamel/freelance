package com.freelance.service.login;

import com.freelance.exception.validation.ValidationException;
import com.freelance.login.LoginRequestDTO;
import com.freelance.login.LoginResponseDTO;

public class Login {

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO){
        throw new ValidationException();
    }
}
