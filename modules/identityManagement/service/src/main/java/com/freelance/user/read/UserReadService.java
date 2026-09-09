package com.freelance.user.read;

import com.freelance.user.repository.UserRepository;
import com.freelance.user.read.assembler.UserReadAssembler;

public class UserReadService extends User {

    public UserReadService() {
        super();
    }

    public UserReadService(UserRepository userRepository, UserReadAssembler assembler) {
        super(userRepository, assembler);
    }
}
