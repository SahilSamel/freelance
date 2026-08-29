package com.freelance.configuration.system.assembler;

import com.freelance.configuration.dto.system.SystemConfigurationDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationGroupDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateResponseDTO;
import com.freelance.configuration.entity.system.SystemConfiguration;

import java.util.*;

public class SystemConfigurationAssembler {

    private static class MutableGroupNode {
        String name;
        Map<String, MutableGroupNode> subGroups = new LinkedHashMap<>();
        List<SystemConfigurationDTO> configurations = new ArrayList<>();

        MutableGroupNode(String name) {
            this.name = name;
        }

        SystemConfigurationGroupDTO toImmutableDTO() {
            List<SystemConfigurationGroupDTO> subGroupDTOs = subGroups.values().stream()
                    .map(MutableGroupNode::toImmutableDTO)
                    .toList();
            return new SystemConfigurationGroupDTO(name, subGroupDTOs, Collections.unmodifiableList(configurations));
        }
    }

    public SystemConfigurationDTO toDTO(SystemConfiguration entity) {
        if (entity == null) {
            return null;
        }
        return new SystemConfigurationDTO(
                entity.getConfigId(),
                entity.getConfigValue(),
                entity.getDescription(),
                entity.getCreatedDate(),
                entity.getLastUpdatedDate()
        );
    }

    public SystemConfigurationListResponseDTO toListResponseDTO(List<SystemConfiguration> entities) {
        if (entities == null || entities.isEmpty()) {
            return new SystemConfigurationListResponseDTO(Collections.emptyList(), 0);
        }

        Map<String, MutableGroupNode> rootGroups = new LinkedHashMap<>();
        int count = 0;

        for (SystemConfiguration entity : entities) {
            if (entity == null) continue;
            count++;
            SystemConfigurationDTO dto = toDTO(entity);
            String configId = entity.getConfigId();
            String[] segments = (configId == null || configId.isBlank())
                    ? new String[]{"Default"}
                    : configId.split("\\.");

            String[] groupSegments;
            if (segments.length > 1) {
                groupSegments = Arrays.copyOf(segments, segments.length - 1);
            } else {
                groupSegments = new String[]{"Default"};
            }

            Map<String, MutableGroupNode> currentLevel = rootGroups;
            MutableGroupNode targetNode = null;

            for (String segment : groupSegments) {
                String trimmed = segment.trim();
                targetNode = currentLevel.computeIfAbsent(trimmed, MutableGroupNode::new);
                currentLevel = targetNode.subGroups;
            }

            if (targetNode != null) {
                targetNode.configurations.add(dto);
            }
        }

        List<SystemConfigurationGroupDTO> groupDTOs = rootGroups.values().stream()
                .map(MutableGroupNode::toImmutableDTO)
                .toList();

        return new SystemConfigurationListResponseDTO(groupDTOs, count);
    }

    public SystemConfigurationUpdateResponseDTO toUpdateResponseDTO(SystemConfiguration entity) {
        if (entity == null) {
            return null;
        }
        return new SystemConfigurationUpdateResponseDTO(
                entity.getConfigId(),
                entity.getConfigValue(),
                entity.getDescription()
        );
    }
}
