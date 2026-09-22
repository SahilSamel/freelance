package com.freelance.user.assembler;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.user.contact.UserContactResponseDTO;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.user.create.assembler.UserCreateAssembler;
import com.freelance.user.entity.User;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.read.assembler.UserReadAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateAssembler & UserReadAssembler Unit Tests")
class UserAssemblerTest {

    private final UserCreateAssembler createAssembler = new UserCreateAssembler();
    private final UserReadAssembler readAssembler = new UserReadAssembler();

    @Test
    @DisplayName("UserCreateAssembler converts create request to entity and entity to response DTO")
    void testUserCreateAssembler() {
        assertNull(createAssembler.toEntity(null));
        assertNull(createAssembler.fromEntity(null));
        assertNull(createAssembler.fromEntity(null, List.of()));
        assertNull(createAssembler.fromEntity(null, List.of(), List.of()));
        assertNull(createAssembler.toContactResponseDTO(null));

        UserCreateRequestDTO request = new UserCreateRequestDTO("testuser", "Password@123", AlertChannel.WHATSAPP, "+1234567890");
        User user = createAssembler.toEntity(request);
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertNotNull(user.getPassword());

        UUID userId = UUID.randomUUID();
        user.setId(userId);

        UserCreateResponseDTO response1 = createAssembler.fromEntity(user);
        assertNotNull(response1);
        assertEquals(userId, response1.id());
        assertEquals("testuser", response1.username());

        UUID roleId = UUID.randomUUID();
        UserCreateResponseDTO response2 = createAssembler.fromEntity(user, List.of(roleId));
        assertNotNull(response2);
        assertEquals(userId, response2.id());
        assertEquals(1, response2.roleIds().size());
        assertEquals(roleId, response2.roleIds().get(0));

        UserCreateResponseDTO response3 = createAssembler.fromEntity(user, null);
        assertNotNull(response3);
        assertTrue(response3.roleIds().isEmpty());

        UserContactEntity contactEntity = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+1234567890");
        UserContactResponseDTO contactDto = createAssembler.toContactResponseDTO(contactEntity);
        assertNotNull(contactDto);
        assertEquals(contactEntity.getId(), contactDto.id());
        assertEquals(userId, contactDto.userId());
        assertEquals(AlertChannel.WHATSAPP, contactDto.channel());
        assertEquals("+1234567890", contactDto.contactValue());

        UserCreateResponseDTO response4 = createAssembler.fromEntity(user, List.of(roleId), List.of(contactDto));
        assertNotNull(response4);
        assertEquals(1, response4.contacts().size());
        assertEquals("+1234567890", response4.contacts().get(0).contactValue());

        UserCreateResponseDTO response5 = createAssembler.fromEntity(user, null, null);
        assertNotNull(response5);
        assertTrue(response5.roleIds().isEmpty());
        assertTrue(response5.contacts().isEmpty());
    }

    @Test
    @DisplayName("UserReadAssembler converts read request to entity and entity to response DTO")
    void testUserReadAssembler() {
        assertNull(readAssembler.toEntity(null));
        assertNull(readAssembler.fromEntity(null));

        UUID userId = UUID.randomUUID();
        UserReadRequestDTO request = new UserReadRequestDTO(userId, "readuser");
        User user = readAssembler.toEntity(request);
        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals("readuser", user.getUsername());

        LocalDateTime now = LocalDateTime.now();
        user.setPassword("hashedPass");
        user.setCreatedDate(now);
        user.setLastUpdatedDate(now);

        UserReadResponseDTO response = readAssembler.fromEntity(user);
        assertNotNull(response);
        assertNotNull(response.userDTO());
        assertEquals(userId, response.userDTO().id());
        assertEquals("readuser", response.userDTO().username());
        assertEquals("hashedPass", response.userDTO().password());
        assertEquals(now, response.userDTO().createdDate());
        assertEquals(now, response.userDTO().lastUpdatedDate());
    }

    @Test
    @DisplayName("UserUpdateAssembler updates username, password, and contacts")
    void testUserUpdateAssembler() {
        com.freelance.user.update.assembler.UserUpdateAssembler updateAssembler = new com.freelance.user.update.assembler.UserUpdateAssembler();
        assertNull(updateAssembler.fromEntity(null));
        assertNull(updateAssembler.toContactResponseDTO(null));

        User user = new User("original_user", "original_pass");
        user.setId(UUID.randomUUID());
        user.addContact(new UserContactEntity(AlertChannel.WHATSAPP, "+1111111111"));

        // apply null request does nothing
        updateAssembler.applyUpdates(user, null);
        updateAssembler.applyUpdates(null, new com.freelance.user.update.UserUpdateRequestDTO(user.getId(), "new_user", "new_pass"));
        assertEquals("original_user", user.getUsername());

        // apply updates with new username, password, and contacts
        com.freelance.user.update.UserUpdateRequestDTO request = new com.freelance.user.update.UserUpdateRequestDTO(
                user.getId(),
                "updated_user",
                "NewPassword123",
                List.of(new com.freelance.user.contact.UserContactRequestDTO(AlertChannel.PUSH, "fcm_token_999"))
        );
        updateAssembler.applyUpdates(user, request);

        assertEquals("updated_user", user.getUsername());
        assertEquals(1, user.getContacts().size());
        assertEquals(AlertChannel.PUSH, user.getContacts().get(0).getChannel());
        assertEquals("fcm_token_999", user.getContacts().get(0).getContactValue());

        com.freelance.user.update.UserUpdateResponseDTO response = updateAssembler.fromEntity(user);
        assertNotNull(response);
        assertEquals(user.getId(), response.id());
        assertEquals("updated_user", response.username());
        assertEquals(1, response.contacts().size());
        assertEquals("fcm_token_999", response.contacts().get(0).contactValue());
    }

    @Test
    @DisplayName("User entity helper methods add, remove, and get contacts")
    void testUserEntityMethods() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("pass");
        assertNotNull(user.getContacts());

        UserContactEntity contact1 = new UserContactEntity(AlertChannel.WHATSAPP, "+123");
        user.addContact(contact1);
        user.addContact(null);
        assertEquals(1, user.getContacts().size());

        user.removeContact(contact1);
        user.removeContact(null);
        assertEquals(0, user.getContacts().size());

        user.setContacts(null);
        assertNotNull(user.getContacts());

        User userWithContacts = new User("u", "p", List.of(contact1));
        assertEquals(1, userWithContacts.getContacts().size());

        User userWithIdAndContacts = new User(UUID.randomUUID(), "u", "p", List.of(contact1));
        assertEquals(1, userWithIdAndContacts.getContacts().size());
    }
}
