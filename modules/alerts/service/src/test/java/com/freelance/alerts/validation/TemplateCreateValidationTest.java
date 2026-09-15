package com.freelance.alerts.validation;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateCreateRequestDTO;
import com.freelance.template.validation.TemplateCreateValidation;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCreateValidationTest {

    private TransactionContext context;
    private TemplateCreateValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new TemplateCreateValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should throw ValidationException when request is null")
    void testNullRequest() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().containsKey("templateKey"));
        assertTrue(context.getValidationErrors().get("templateKey").contains(ValidationErrors.INVALID_TEMPLATE_KEY));
        assertTrue(context.getValidationErrors().containsKey("content"));
        assertTrue(context.getValidationErrors().get("content").contains(ValidationErrors.INVALID_TEMPLATE_CONTENT));
    }

    @Test
    @DisplayName("Should throw ValidationException when root template has blank key or content")
    void testBlankKeyOrContent() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "   ", "Name", "UTILITY", "en", "   ", null, null, null, null, null
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("templateKey"));
        assertTrue(context.getValidationErrors().containsKey("content"));
    }

    @Test
    @DisplayName("Should pass validation for a single terminal template with no buttons")
    void testSingleTerminalTemplatePasses() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_TERMINAL", "Terminal", "UTILITY", "en", "Welcome message", null, null, null, null, null
        );

        assertDoesNotThrow(() -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should throw ValidationException when child template has empty content")
    void testChildTemplateEmptyContentThrows() {
        TemplateCreateRequestDTO child = new TemplateCreateRequestDTO(
                "TPL_CHILD", "Child", "UTILITY", "en", "   ", null, null, null, null, null
        );
        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Root content", null, null, null, null,
                List.of(new TemplateButtonDTO("Next", ButtonType.QUICK_REPLY, "p", "TPL_CHILD", false, 1)),
                List.of(child)
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(root));
        assertTrue(context.getValidationErrors().containsKey("content"));
        assertTrue(context.getValidationErrors().get("content").contains(ValidationErrors.INVALID_TEMPLATE_CONTENT));
    }

    @Test
    @DisplayName("Should throw ValidationException on duplicate template keys within request")
    void testDuplicateKeysThrows() {
        TemplateCreateRequestDTO child = new TemplateCreateRequestDTO(
                "TPL_SAME", "Child", "UTILITY", "en", "Content 2", null, null, null, null, null
        );
        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_SAME", "Root", "UTILITY", "en", "Content 1", null, null, null, null, null,
                List.of(child)
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(root));
        assertTrue(context.getValidationErrors().containsKey("templateKey"));
        assertTrue(context.getValidationErrors().get("templateKey").contains(ValidationErrors.DUPLICATE_TEMPLATE_KEY));
    }

    @Test
    @DisplayName("Should throw ValidationException when quick-reply button has blank replyTemplateKey and requiresHuman is false")
    void testButtonMissingReplyKeyThrows() {
        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Root content", null, null, null, null,
                List.of(new TemplateButtonDTO("Click", ButtonType.QUICK_REPLY, "p", null, false, 1))
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(root));
        assertTrue(context.getValidationErrors().containsKey("buttons.replyTemplateKey"));
        assertTrue(context.getValidationErrors().get("buttons.replyTemplateKey").contains(ValidationErrors.INVALID_TEMPLATE_BUTTON));
    }



    @Test
    @DisplayName("Should pass validation when button chain terminates at a terminal template in same request")
    void testChainTerminatesAtTerminalTemplate() {
        TemplateCreateRequestDTO step2 = new TemplateCreateRequestDTO(
                "TPL_STEP2", "Step 2", "UTILITY", "en", "Final confirmation", null, null, null, null, Collections.emptyList()
        );
        TemplateCreateRequestDTO step1 = new TemplateCreateRequestDTO(
                "TPL_STEP1", "Step 1", "UTILITY", "en", "Step 1 info", null, null, null, null,
                List.of(new TemplateButtonDTO("Continue", ButtonType.QUICK_REPLY, "p1", "TPL_STEP2", false, 1)),
                List.of(step2)
        );

        assertDoesNotThrow(() -> validation.validateRequest(step1));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation when button chain forms a loop (template comes twice in a loop)")
    void testChainTerminatesInLoop() {
        // TPL_A -> TPL_B -> TPL_A
        TemplateCreateRequestDTO b = new TemplateCreateRequestDTO(
                "TPL_B", "Node B", "UTILITY", "en", "Content B", null, null, null, null,
                List.of(new TemplateButtonDTO("Back to A", ButtonType.QUICK_REPLY, "p_back", "TPL_A", false, 1))
        );
        TemplateCreateRequestDTO a = new TemplateCreateRequestDTO(
                "TPL_A", "Node A", "UTILITY", "en", "Content A", null, null, null, null,
                List.of(new TemplateButtonDTO("Go to B", ButtonType.QUICK_REPLY, "p_next", "TPL_B", false, 1)),
                List.of(b)
        );

        assertDoesNotThrow(() -> validation.validateRequest(a));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation on self-loop (template points to itself)")
    void testSelfLoopPasses() {
        TemplateCreateRequestDTO selfLoop = new TemplateCreateRequestDTO(
                "TPL_MENU", "Menu", "UTILITY", "en", "Current menu options", null, null, null, null,
                List.of(new TemplateButtonDTO("Refresh Menu", ButtonType.QUICK_REPLY, "refresh", "TPL_MENU", false, 1))
        );

        assertDoesNotThrow(() -> validation.validateRequest(selfLoop));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation when button requires human agent (terminal action)")
    void testButtonRequiresHumanPasses() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_SUPPORT", "Support", "UTILITY", "en", "Need help?", null, null, null, null,
                List.of(new TemplateButtonDTO("Talk to Agent", ButtonType.QUICK_REPLY, "agent_payload", null, true, 1))
        );

        assertDoesNotThrow(() -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation when button is external URL (terminal action)")
    void testButtonUrlPasses() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_TERMS", "Terms", "UTILITY", "en", "Read our terms", null, null, null, null,
                List.of(new TemplateButtonDTO("Visit Site", ButtonType.URL, "https://freelance.com", null, false, 1))
        );

        assertDoesNotThrow(() -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().isEmpty());
    }



    @Test
    @DisplayName("Should instantiate TemplateCreateValidation with default constructor and validate terminal template")
    void testDefaultConstructorInstantiatesSuccessfully() {
        TemplateCreateValidation defaultVal = new TemplateCreateValidation();
        assertNotNull(defaultVal);
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_DEFAULT", "Default", null, "en", "Content", null, null, null, null, null
        );
        assertDoesNotThrow(() -> defaultVal.validateRequest(req));
    }

    @Test
    @DisplayName("Should throw ValidationException when template has more than 10 buttons")
    void testTooManyButtonsThrows() {
        List<TemplateButtonDTO> elevenButtons = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            elevenButtons.add(new TemplateButtonDTO("B" + i, ButtonType.URL, "https://" + i + ".com", null, false, i));
        }
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_MANY_BUTTONS", "Many", "UTILITY", "en", "Content", null, null, null, null,
                elevenButtons
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons"));
        assertTrue(context.getValidationErrors().get("buttons").contains(ValidationErrors.TOO_MANY_BUTTONS));
    }

    @Test
    @DisplayName("Should throw ValidationException when template has duplicate button texts (case-insensitive)")
    void testDuplicateButtonTextThrows() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_DUP_TEXT", "Dup Text", "UTILITY", "en", "Content", null, null, null, null,
                List.of(
                        new TemplateButtonDTO("Confirm", ButtonType.URL, "https://1.com", null, false, 1),
                        new TemplateButtonDTO("confirm", ButtonType.URL, "https://2.com", null, false, 2)
                )
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.text"));
        assertTrue(context.getValidationErrors().get("buttons.text").contains(ValidationErrors.DUPLICATE_BUTTON_TEXT));
    }

    @Test
    @DisplayName("Should throw ValidationException when template has duplicate sequence orders")
    void testDuplicateButtonSequenceOrderThrows() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_DUP_SEQ", "Dup Seq", "UTILITY", "en", "Content", null, null, null, null,
                List.of(
                        new TemplateButtonDTO("Btn 1", ButtonType.URL, "https://1.com", null, false, 1),
                        new TemplateButtonDTO("Btn 2", ButtonType.URL, "https://2.com", null, false, 1)
                )
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.sequenceOrder"));
        assertTrue(context.getValidationErrors().get("buttons.sequenceOrder").contains(ValidationErrors.DUPLICATE_BUTTON_SEQUENCE));
    }

    @Test
    @DisplayName("Should throw ValidationException when duplicate button payloads exist in the request")
    void testDuplicateButtonPayloadThrows() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_DUP_PAYLOAD", "Dup Payload", "UTILITY", "en", "Content", null, null, null, null,
                List.of(
                        new TemplateButtonDTO("Btn 1", ButtonType.QUICK_REPLY, "PAYLOAD_DUP", null, true, 1),
                        new TemplateButtonDTO("Btn 2", ButtonType.QUICK_REPLY, "PAYLOAD_DUP", null, true, 2)
                )
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.payload"));
        assertTrue(context.getValidationErrors().get("buttons.payload").contains(ValidationErrors.DUPLICATE_BUTTON_PAYLOAD));
    }

    @Test
    @DisplayName("Should pass validation when URL or PHONE_NUMBER buttons share identical payloads")
    void testDuplicateUrlOrPhonePayloadPasses() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_URL_PHONE_DUP", "URL Phone Dup", "UTILITY", "en", "Content", null, null, null, null,
                List.of(
                        new TemplateButtonDTO("Website 1", ButtonType.URL, "https://example.com", null, false, 1),
                        new TemplateButtonDTO("Website 2", ButtonType.URL, "https://example.com", null, false, 2),
                        new TemplateButtonDTO("Call 1", ButtonType.PHONE_NUMBER, "+919876543210", null, false, 3),
                        new TemplateButtonDTO("Call 2", ButtonType.PHONE_NUMBER, "+919876543210", null, false, 4)
                )
        );

        assertDoesNotThrow(() -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation when WhatsApp template has up to 3 buttons")
    void testWhatsAppTemplateWithThreeButtonsPasses() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WA_3_BUTTONS", "WhatsApp 3 Buttons", "UTILITY", "en", "Content", null, null,
                AlertChannel.WHATSAPP, null,
                List.of(
                        new TemplateButtonDTO("Btn 1", ButtonType.URL, "https://1.com", null, false, 1),
                        new TemplateButtonDTO("Btn 2", ButtonType.URL, "https://2.com", null, false, 2),
                        new TemplateButtonDTO("Btn 3", ButtonType.URL, "https://3.com", null, false, 3)
                )
        );

        assertDoesNotThrow(() -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation when WhatsApp template has up to 10 buttons (interactive list)")
    void testWhatsAppTemplateWithInteractiveListPasses() {
        List<TemplateButtonDTO> fiveButtons = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fiveButtons.add(new TemplateButtonDTO("Option " + i, ButtonType.URL, "https://" + i + ".com", null, false, i));
        }
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WA_5_BUTTONS", "WhatsApp 5 Buttons", "UTILITY", "en", "Content", null, null,
                AlertChannel.WHATSAPP, null,
                fiveButtons
        );

        assertDoesNotThrow(() -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should throw ValidationException when WhatsApp template has more than 10 buttons")
    void testWhatsAppTemplateWithMoreThanTenButtonsThrows() {
        List<TemplateButtonDTO> elevenButtons = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            elevenButtons.add(new TemplateButtonDTO("Btn " + i, ButtonType.URL, "https://" + i + ".com", null, false, i));
        }
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WA_11_BUTTONS", "WhatsApp 11 Buttons", "UTILITY", "en", "Content", null, null,
                AlertChannel.WHATSAPP, null,
                elevenButtons
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons"));
        assertTrue(context.getValidationErrors().get("buttons").contains(ValidationErrors.TOO_MANY_BUTTONS));
    }

    @Test
    @DisplayName("Should pass validation when category is a valid standard category")
    void testValidStandardCategoriesPass() {
        TemplateCreateRequestDTO reqAuth = new TemplateCreateRequestDTO(
                "TPL_CAT_AUTH", "Auth", "AUTHENTICATION", "en", "OTP code", null, null, null, null, null
        );
        TemplateCreateRequestDTO reqMkt = new TemplateCreateRequestDTO(
                "TPL_CAT_MKT", "Mkt", "MARKETING", "en", "Special Offer", null, null, null, null, null
        );

        assertDoesNotThrow(() -> validation.validateRequest(reqAuth));
        assertDoesNotThrow(() -> validation.validateRequest(reqMkt));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should pass validation when priority is HIGH, NORMAL, LOW, or case-insensitive variants")
    void testValidPrioritiesPass() {
        TemplateCreateRequestDTO reqHigh = new TemplateCreateRequestDTO(
                "TPL_HIGH", "High", "UTILITY", "HIGH", "en", "Urgent notice", null, null, null, null, null, Collections.emptyList()
        );
        TemplateCreateRequestDTO reqLow = new TemplateCreateRequestDTO(
                "TPL_LOW", "Low", "UTILITY", "low", "en", "Bulk message", null, null, null, null, null, Collections.emptyList()
        );
        TemplateCreateRequestDTO reqNormal = new TemplateCreateRequestDTO(
                "TPL_NORM", "Norm", "UTILITY", "Normal", "en", "Standard update", null, null, null, null, null, Collections.emptyList()
        );

        assertDoesNotThrow(() -> validation.validateRequest(reqHigh));
        assertDoesNotThrow(() -> validation.validateRequest(reqLow));
        assertDoesNotThrow(() -> validation.validateRequest(reqNormal));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should throw ValidationException when priority is invalid")
    void testInvalidPriorityThrows() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_INV_PRIO", "Inv", "UTILITY", "SUPER_URGENT", "en", "Text", null, null, null, null, null, Collections.emptyList()
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("priority"));
        assertTrue(context.getValidationErrors().get("priority").contains(ValidationErrors.INVALID_TEMPLATE_PRIORITY));
    }
}