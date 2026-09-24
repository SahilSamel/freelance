package com.freelance.auth.service;

import com.freelance.auth.dto.*;
import com.freelance.auth.service.constraint.LoginConstraint;
import com.freelance.auth.service.constraint.VerifyAuthConstraint;
import com.freelance.auth.service.validation.LoginValidation;
import com.freelance.auth.service.validation.VerifyAuthValidation;
import com.freelance.auth.session.Session;
import com.freelance.auth.token.TokenUtility;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import com.freelance.user.read.User;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;

public class Authentication {

    private final Session session = new Session();
    private final TokenUtility tokenUtility = new TokenUtility();

    public AuthLoginResponseDTO login(AuthLoginRequestDTO request) {
        new LoginValidation().validateRequest(request);
        new LoginConstraint().checkConstraints(request);

        User userService = new User();
        UserReadRequestDTO userReadRequestDTO = new UserReadRequestDTO(null, request.username());
        UserReadResponseDTO userReadResponseDTO = userService.findByUsername(userReadRequestDTO);

        SessionGenerateResponseDTO session = this.session.generateSession(new SessionGenerateRequestDTO(userReadResponseDTO.userDTO().id()));
        TokenGenerateResponseDTO token = tokenUtility.generateToken(new TokenGenerateRequestDTO(userReadResponseDTO.userDTO().id(), session.sessionKey(), Transaction.getRequestContext().tenantId()));

        return new AuthLoginResponseDTO(token.token());
    }

    public void logout() {
        session.invalidateSession(new SessionInvalidateRequestDTO(Transaction.getRequestContext().sessionKey(), Transaction.getRequestContext().userId()));
    }

    public AuthVerifyResponseDTO verify(AuthVerifyRequestDTO request) {
        new VerifyAuthValidation().validateRequest(request);
        new VerifyAuthConstraint().checkConstraints(request);

        if (VendorSupportKeyUtility.isVendorToken(request.token())) {
            return new AuthVerifyResponseDTO(
                    SystemHardcoding.VENDOR_SUPPORT_USER_ID.asUuid(),
                    SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString()
            );
        }

        TokenParseResponseDTO parsed = tokenUtility.parse(new TokenParseRequestDTO(request.token()));
        session.verifySession(new SessionVerifyRequestDTO(parsed.sessionKey(), parsed.userId()));

        return new AuthVerifyResponseDTO(parsed.userId(), parsed.sessionKey());
    }

    public AuthRefreshResponseDTO refreshSession() {
        String oldSessionKey = Transaction.getRequestContext().sessionKey();
        java.util.UUID userId = Transaction.getRequestContext().userId();

        SessionRefreshResponseDTO refreshed = session.refreshSession(new SessionRefreshRequestDTO(oldSessionKey, userId));

        if (refreshed.newSessionKey() == null) {
            throw new ConstraintException(SessionConstraintErrors.INVALID_SESSION);
        }
        TokenGenerateResponseDTO token = tokenUtility.generateToken(new TokenGenerateRequestDTO(userId, refreshed.newSessionKey(), Transaction.getRequestContext().tenantId()));
        return new AuthRefreshResponseDTO(token.token());
    }
}
