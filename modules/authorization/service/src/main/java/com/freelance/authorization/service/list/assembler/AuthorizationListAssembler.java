package com.freelance.authorization.service.list.assembler;

import com.freelance.authorization.dto.AuthorizationDTO;
import com.freelance.authorization.dto.list.AuthorizationGroupDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.entity.authorization.Authorization;

import java.util.*;

public class AuthorizationListAssembler {

    private static class MutableGroupNode {
        String name;
        Map<String, MutableGroupNode> subGroups = new LinkedHashMap<>();
        List<AuthorizationDTO> authorizations = new ArrayList<>();

        MutableGroupNode(String name) {
            this.name = name;
        }

        AuthorizationGroupDTO toImmutableDTO() {
            List<AuthorizationGroupDTO> subGroupDTOs = subGroups.values().stream()
                    .map(MutableGroupNode::toImmutableDTO)
                    .toList();
            return new AuthorizationGroupDTO(name, subGroupDTOs, Collections.unmodifiableList(authorizations));
        }
    }

    public AuthorizationDTO toDTO(Authorization entity) {
        if (entity == null) {
            return null;
        }
        return new AuthorizationDTO(
                entity.getId(),
                entity.getKey(),
                entity.getName(),
                entity.getDescription(),
                entity.getGroupPath()
        );
    }

    public AuthorizationListResponseDTO toResponseDTO(List<Authorization> entities) {
        if (entities == null || entities.isEmpty()) {
            return new AuthorizationListResponseDTO(Collections.emptyList(), 0);
        }

        Map<String, MutableGroupNode> rootGroups = new LinkedHashMap<>();
        int count = 0;

        for (Authorization entity : entities) {
            if (entity == null) continue;
            count++;
            AuthorizationDTO dto = toDTO(entity);
            String groupPath = entity.getGroupPath();
            String[] segments = (groupPath == null || groupPath.isBlank())
                    ? new String[]{"Default"}
                    : groupPath.split("\\.");

            Map<String, MutableGroupNode> currentLevel = rootGroups;
            MutableGroupNode targetNode = null;

            for (String segment : segments) {
                String trimmed = segment.trim();
                targetNode = currentLevel.computeIfAbsent(trimmed, MutableGroupNode::new);
                currentLevel = targetNode.subGroups;
            }

            if (targetNode != null) {
                targetNode.authorizations.add(dto);
            }
        }

        List<AuthorizationGroupDTO> groupDTOs = rootGroups.values().stream()
                .map(MutableGroupNode::toImmutableDTO)
                .toList();

        return new AuthorizationListResponseDTO(groupDTOs, count);
    }
}
