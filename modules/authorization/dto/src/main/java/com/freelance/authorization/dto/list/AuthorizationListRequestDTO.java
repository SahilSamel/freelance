package com.freelance.authorization.dto.list;

public record AuthorizationListRequestDTO(
        String groupPath
) {
    public static AuthorizationListRequestDTO empty() {
        return new AuthorizationListRequestDTO(null);
    }
}
