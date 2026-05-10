<?php

use PHPUnit\Framework\TestCase;

class AuthEntityTest extends TestCase
{
    /**
     * Test Case ID: TC_S1_01
     * Description: Auth mặc định không available khi khởi tạo mới
     * Scenario Type: Positive
     * Expected Result: isAvailable() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_01
     */
    public function testIsAvailableReturnsFalseByDefault()
    {
        // Arrange
        $auth = new Auth();

        // Act
        $actual = $auth->isAvailable();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_02
     * Description: markAsAvailable đánh dấu Auth là available
     * Scenario Type: Positive
     * Expected Result: isAvailable() trả về true sau khi gọi markAsAvailable()
     * Related Module: Authentication
     * Link Anchor: #TC_S1_02
     */
    public function testMarkAsAvailableSetsAvailabilityTrue()
    {
        // Arrange
        $auth = new Auth();

        // Act
        $auth->markAsAvailable();

        // Assert
        $this->assertTrue($auth->isAvailable());
    }

    /**
     * Test Case ID: TC_S1_03
     * Description: set/get lưu và trả về giá trị string đã được trim
     * Scenario Type: Positive
     * Expected Result: get() trả về chuỗi đã trim
     * Related Module: Authentication
     * Link Anchor: #TC_S1_03
     */
    public function testSetAndGetTrimsStringValue()
    {
        // Arrange
        $auth = new Auth();

        // Act
        $auth->set('role', '  admin  ');

        // Assert
        $this->assertSame('admin', $auth->get('role'));
    }

    /**
     * Test Case ID: TC_S1_04
     * Description: set hỗ trợ cập nhật json subfield theo cú pháp dot notation
     * Scenario Type: Positive
     * Expected Result: get('preferences.language') trả về giá trị đúng
     * Related Module: Authentication
     * Link Anchor: #TC_S1_04
     */
    public function testSetDotNotationUpdatesJsonSubfield()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('preferences', json_encode(array('language' => 'en-US')));

        // Act
        $auth->set('preferences.language', 'vi-VN');

        // Assert
        $this->assertSame('vi-VN', $auth->get('preferences.language'));
    }

    /**
     * Test Case ID: TC_S1_05
     * Description: get trả về null khi field không tồn tại
     * Scenario Type: Negative
     * Expected Result: get('missing_field') trả về null
     * Related Module: Authentication
     * Link Anchor: #TC_S1_05
     */
    public function testGetReturnsNullWhenFieldDoesNotExist()
    {
        // Arrange
        $auth = new Auth();

        // Act
        $actual = $auth->get('missing_field');

        // Assert
        $this->assertNull($actual);
    }

    /**
     * Test Case ID: TC_S1_06
     * Description: isAdmin trả về true khi Auth available và role là admin
     * Scenario Type: Positive
     * Expected Result: isAdmin() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_06
     */
    public function testIsAdminReturnsTrueWhenRoleIsAdminAndAvailable()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('role', 'admin')->markAsAvailable();

        // Act
        $actual = $auth->isAdmin();

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_07
     * Description: isAdmin trả về false khi Auth chưa available dù role là admin
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_07
     */
    public function testIsAdminReturnsFalseWhenNotAvailableEvenIfRoleIsAdmin()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('role', 'admin');

        // Act
        $actual = $auth->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_08
     * Description: isAdmin trả về false khi role không phải admin
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_08
     */
    public function testIsAdminReturnsFalseWhenRoleIsNotAdmin()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('role', 'doctor')->markAsAvailable();

        // Act
        $actual = $auth->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_09
     * Description: isAdmin thất bại khi role chứa tab character
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_09
     */
    public function testIsAdminReturnsFalseWhenRoleContainsTab()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('role', "\tadmin\t");
        $auth->markAsAvailable();

        // Act
        $actual = $auth->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }
}

