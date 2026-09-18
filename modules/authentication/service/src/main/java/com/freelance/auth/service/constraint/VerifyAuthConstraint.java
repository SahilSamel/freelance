package com.freelance.auth.service.constraint;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.keys.authentication.support.SupportConfigKeys;
import com.freelance.keys.authentication.support.SupportConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.security.PublicKey;

public class VerifyAuthConstraint extends AbstractConstraint {

    private final SupportAccessService supportAccessService;

    public VerifyAuthConstraint() {
        this(new SupportAccessService());
    }

    public VerifyAuthConstraint(SupportAccessService supportAccessService) {
        this.supportAccessService = supportAccessService;
    }

    public void checkConstraints(AuthVerifyRequestDTO request) {
        if (request == null || request.token() == null) {
            return;
        }

        if (VendorSupportKeyUtility.isVendorToken(request.token())) {
            String pubKeyStr = ConfigurationEngine.get(SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY);
            if (pubKeyStr == null || pubKeyStr.isBlank()) {
                raiseConstraintError(SupportConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN);
            }

            try {
                PublicKey pubKey = VendorSupportKeyUtility.parsePublicKey(pubKeyStr);
                VendorSupportKeyUtility.verifyAndParseSupportToken(request.token(), pubKey);
            } catch (Exception e) {
                raiseConstraintError(SupportConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN);
            }

            if (!supportAccessService.isSupportAccessActive()) {
                raiseConstraintError(SupportConstraintErrors.SUPPORT_ACCESS_DISABLED);
            }
        }
    }
}
