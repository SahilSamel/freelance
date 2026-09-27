package com.freelance.gym.member.dto;

import java.io.Serializable;
import java.util.List;

public record MemberListResponseDTO(
        List<MemberRecordDTO> members
) implements Serializable {
}
