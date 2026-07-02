package service.login;

import Login.LoginRequestDTO;
import Login.LoginResponseDTO;
import exception.validation.ValidationException;

public class Login {

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO){
        throw new ValidationException();
    }
}
