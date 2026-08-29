package com.freelance.authorization.service.list;

import com.freelance.authorization.dto.list.AuthorizationListRequestDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.authorization.service.AuthorizationService;
import com.freelance.authorization.service.list.assembler.AuthorizationListAssembler;
import com.freelance.entity.authorization.Authorization;
import com.freelance.repository.authorization.AuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthorizationService List Tests")
class AuthorizationListServiceTest {

    static class FakeAuthorizationRepository extends AuthorizationRepository {
        final List<Authorization> storage = new ArrayList<>();

        @Override
        public List<Authorization> findAll() {
            return new ArrayList<>(storage);
        }
    }

    @Test
    @DisplayName("listAuthorizations returns all authorizations when no groupPath filter is provided")
    void listAuthorizations_noFilter_returnsAll() {
        FakeAuthorizationRepository repo = new FakeAuthorizationRepository();
        repo.storage.add(new Authorization(UUID.randomUUID(), "AUTH1", "Auth 1", "Desc", "Group.A"));
        repo.storage.add(new Authorization(UUID.randomUUID(), "AUTH2", "Auth 2", "Desc", "Group.B"));

        AuthorizationService service = new AuthorizationService(repo, new AuthorizationListAssembler());
        AuthorizationListResponseDTO response = service.listAuthorizations(new AuthorizationListRequestDTO(null));

        assertNotNull(response);
        assertEquals(2, response.totalCount());
        assertEquals(1, response.groups().size());
        assertEquals("Group", response.groups().get(0).name());
        assertEquals(2, response.groups().get(0).subGroups().size());
    }

    @Test
    @DisplayName("listAuthorizations filters by groupPath when specified")
    void listAuthorizations_withGroupPathFilter_returnsFiltered() {
        FakeAuthorizationRepository repo = new FakeAuthorizationRepository();
        repo.storage.add(new Authorization(UUID.randomUUID(), "AUTH1", "Auth 1", "Desc", "Group.A"));
        repo.storage.add(new Authorization(UUID.randomUUID(), "AUTH2", "Auth 2", "Desc", "Group.B"));

        AuthorizationService service = new AuthorizationService(repo, new AuthorizationListAssembler());
        AuthorizationListResponseDTO response = service.listAuthorizations(new AuthorizationListRequestDTO("Group.A"));

        assertNotNull(response);
        assertEquals(1, response.totalCount());
        assertEquals(1, response.groups().size());
        assertEquals("Group", response.groups().get(0).name());
        assertEquals("A", response.groups().get(0).subGroups().get(0).name());
        assertEquals("AUTH1", response.groups().get(0).subGroups().get(0).authorizations().get(0).key());
    }
}
