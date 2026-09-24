package com.freelance.auth.service.constraint;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.dto.TokenParseRequestDTO;
import com.freelance.auth.dto.TokenParseResponseDTO;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.auth.token.TokenUtility;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.keys.authentication.support.SupportConfigKeys;
import com.freelance.keys.authentication.support.SupportConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.Transaction;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;

public class VerifyAuthConstraint extends AbstractConstraint {

    private static final Logger logger = LoggerFactory.getLogger(VerifyAuthConstraint.class);

    private final SupportAccessService supportAccessService;
    private final TokenUtility tokenUtility;

    public VerifyAuthConstraint() {
        this(new SupportAccessService(), new TokenUtility());
    }

    public VerifyAuthConstraint(SupportAccessService supportAccessService, TokenUtility tokenUtility) {
        this.supportAccessService = supportAccessService;
        this.tokenUtility = tokenUtility;
    }

    public void checkConstraints(AuthVerifyRequestDTO request) {

        String requestTenant = Transaction.getRequestContext() != null
                ? Transaction.getRequestContext().tenantId()
                : null;

        if (VendorSupportKeyUtility.isVendorToken(request.token())) {
            String pubKeyStr = ConfigurationEngine.get(SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY);
            if (pubKeyStr == null || pubKeyStr.isBlank()) {
                raiseConstraintError(SupportConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN);
            }

            Claims claims;
            try {
                PublicKey pubKey = VendorSupportKeyUtility.parsePublicKey(pubKeyStr);
                claims = VendorSupportKeyUtility.verifyAndParseSupportToken(request.token(), pubKey);
            } catch (Exception e) {
                raiseConstraintError(SupportConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN);
                return;
            }

            if (!supportAccessService.isSupportAccessActive()) {
                raiseConstraintError(SupportConstraintErrors.SUPPORT_ACCESS_DISABLED);
            }

            String tokenTenant = claims.get("tenantId", String.class);
            if (tokenTenant != null && !tokenTenant.isBlank() && requestTenant != null && !requestTenant.isBlank()) {
                if (!tokenTenant.equalsIgnoreCase(requestTenant.trim())) {
                    logger.warn("Security rejection: Vendor support token issued for tenant '{}' used against request tenant '{}'",
                            tokenTenant, requestTenant);
                    raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
                }
            }
        } else {
            try {
                TokenParseResponseDTO parsed = tokenUtility.parse(new TokenParseRequestDTO(request.token()));
                if (parsed.tenantId() != null && !parsed.tenantId().isBlank() && requestTenant != null && !requestTenant.isBlank()) {
                    if (!parsed.tenantId().equalsIgnoreCase(requestTenant.trim())) {
                        logger.warn("Security rejection: Token issued for tenant '{}' used against request tenant '{}'",
                                parsed.tenantId(), requestTenant);
                        raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
                    }
                }
            } catch (ConstraintException e) {
                throw e;
            } catch (Exception ignored) {
            }
        }
    }
}