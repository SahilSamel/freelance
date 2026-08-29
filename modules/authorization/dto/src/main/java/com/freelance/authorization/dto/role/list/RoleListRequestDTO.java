package com.freelance.authorization.dto.role.list;

public record RoleListRequestDTO(
        Boolean systemOnly
) {
    public static RoleListRequestDTO empty() {
        return new RoleListRequestDTO(null);
    }
}
