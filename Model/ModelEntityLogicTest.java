package com.example.do_an_tot_nghiep.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Model/Entity classes.
 *
 * NOTE:
 * - PASS tests validate current implemented behavior.
 * - FAIL tests highlight real missing validation/business-rule guards.
 */
public class ModelEntityLogicTest {

    private Setting setting;
    private Option option;
    private Handbook handbook;
    private User user;
    private Appointment appointment;

    @Before
    public void setUp() {
        setting = new Setting(1, "setting-id", "Setting Name");
        option = new Option(2, "Option Name");
        handbook = new Handbook("img.png", "Title", "https://example.com");
        user = new User();
        appointment = new Appointment();
    }

    // =============================
    // PASS TEST CASES (behavior/boundary only)
    // =============================


    // Test Case ID: TC-MODEL-PASS-007
    // Boundary value: very long strings are accepted and preserved.
    @Test
    public void givenVeryLongStringWhenSetSettingNameThenNameIsPreserved() {
        String longName = repeat('A', 1024);
        setting.setName(longName);

        assertEquals(longName, setting.getName());
    }

    // =============================
    // FAIL TEST CASES (REAL ISSUES)
    // =============================

    // Test Case ID: TC-MODEL-FAIL-001
    // Real issue: Option.setName() accepts null without fallback/validation.
    // Expected behavior in robust model: null name should be normalized to empty string.
    @Test
    public void givenNullOptionNameWhenSetNameThenShouldFallbackToEmptyString() {
        option.setName(null);

        // Fails on current code: getName() returns null.
        assertNotNull(option.getName());
        assertEquals("", option.getName());
    }

    // Test Case ID: TC-MODEL-FAIL-002
    // Real issue: Setting.setId() accepts blank IDs; no validation for identifier quality.
    @Test
    public void givenBlankSettingIdWhenSetIdThenShouldRejectBlankIdentifier() {
        setting.setId("   ");

        // Fails on current code: blank string is stored directly.
        assertTrue("Expected non-blank id", setting.getId() != null && !setting.getId().trim().isEmpty());
    }

    // Test Case ID: TC-MODEL-FAIL-003
    // Real issue: Appointment.setPosition() allows negative position though queue position should be non-negative.
    @Test
    public void givenNegativeAppointmentPositionWhenSetThenShouldClampToZeroOrReject() {
        appointment.setPosition(-5);

        // Fails on current code: value remains -5.
        assertTrue("Expected non-negative position", appointment.getPosition() != null && appointment.getPosition() >= 0);
    }

    // Test Case ID: TC-MODEL-FAIL-004
    // Real issue: User.setGender() accepts out-of-range values (domain should constrain to known codes).
    @Test
    public void givenOutOfRangeGenderWhenSetThenShouldConstrainToKnownValues() {
        user.setGender(99);

        // Fails on current code: invalid value is stored as-is.
        assertTrue("Expected gender code 0 or 1", user.getGender() == 0 || user.getGender() == 1);
    }

    // Test Case ID: TC-MODEL-FAIL-005
    // Real issue: Handbook constructor accepts null fields without normalization, causing null propagation.
    @Test
    public void givenNullHandbookFieldsWhenConstructThenShouldNormalizeToSafeDefaults() {
        Handbook nullHandbook = new Handbook(null, null, null);

        // Fails on current code: all fields remain null.
        assertNotNull(nullHandbook.getImage());
        assertNotNull(nullHandbook.getTitle());
        assertNotNull(nullHandbook.getUrl());
    }

    private String repeat(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }
}

