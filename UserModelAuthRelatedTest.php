<?php

use PHPUnit\Framework\TestCase;

class UserModelAuthRelatedTest extends TestCase
{
    private function makeAvailableUser(array $fields)
    {
        $u = new UserModel(0); // uniqid=0 => select() won't touch DB
        foreach ($fields as $k => $v) {
            $u->set($k, $v, true);
        }
        $u->markAsAvailable();
        return $u;
    }

    /**
     * Test Case ID: TC_S1_29
     * Description: UserModel isAdmin trả về true khi account_type là admin và user available
     * Scenario Type: Positive
     * Expected Result: isAdmin() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_29
     */
    public function testIsAdminReturnsTrueForAdminAccountType(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array('account_type' => 'admin'));

        // Act
        $actual = $u->isAdmin();

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_30
     * Description: UserModel isAdmin trả về false khi account_type không phải developer/admin
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_30
     */
    public function testIsAdminReturnsFalseForMemberAccountType(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array('account_type' => 'member'));

        // Act
        $actual = $u->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_31
     * Description: UserModel canEdit cho phép developer chỉnh sửa bất kỳ user nào
     * Scenario Type: Positive
     * Expected Result: canEdit() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_31
     */
    public function testCanEditReturnsTrueForDeveloperEditingOtherUser(): void
    {
        // Arrange
        $editor = $this->makeAvailableUser(array('account_type' => 'developer', 'id' => 1));
        $target = $this->makeAvailableUser(array('account_type' => 'member', 'id' => 2));

        // Act
        $actual = $editor->canEdit($target);

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_32
     * Description: UserModel canEdit cho phép user chỉnh sửa chính họ
     * Scenario Type: Positive
     * Expected Result: canEdit() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_32
     */
    public function testCanEditReturnsTrueWhenEditingSelf(): void
    {
        // Arrange
        $editor = $this->makeAvailableUser(array('account_type' => 'member', 'id' => 10));
        $target = $this->makeAvailableUser(array('account_type' => 'member', 'id' => 10));

        // Act
        $actual = $editor->canEdit($target);

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_33
     * Description: UserModel canEdit cho phép admin chỉnh sửa member
     * Scenario Type: Positive
     * Expected Result: canEdit() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_33
     */
    public function testCanEditReturnsTrueForAdminEditingMember(): void
    {
        // Arrange
        $admin = $this->makeAvailableUser(array('account_type' => 'admin', 'id' => 1));
        $member = $this->makeAvailableUser(array('account_type' => 'member', 'id' => 2));

        // Act
        $actual = $admin->canEdit($member);

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_34
     * Description: UserModel canEdit không cho phép member chỉnh sửa user khác
     * Scenario Type: Negative
     * Expected Result: canEdit() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_34
     */
    public function testCanEditReturnsFalseForMemberEditingOtherUser(): void
    {
        // Arrange
        $member = $this->makeAvailableUser(array('account_type' => 'member', 'id' => 1));
        $other = $this->makeAvailableUser(array('account_type' => 'member', 'id' => 2));

        // Act
        $actual = $member->canEdit($other);

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_35
     * Description: UserModel isExpired trả về false khi expire_date nằm trong tương lai
     * Scenario Type: Positive
     * Expected Result: isExpired() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_35
     */
    public function testIsExpiredReturnsFalseWhenExpiryInFuture(): void
    {
        // Arrange
        $future = date('Y-m-d H:i:s', time() + 86400);
        $u = $this->makeAvailableUser(array('expire_date' => $future));

        // Act
        $actual = $u->isExpired();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_36
     * Description: UserModel isExpired trả về true khi expire_date nằm trong quá khứ
     * Scenario Type: Negative
     * Expected Result: isExpired() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_36
     */
    public function testIsExpiredReturnsTrueWhenExpiryInPast(): void
    {
        // Arrange
        $past = date('Y-m-d H:i:s', time() - 86400);
        $u = $this->makeAvailableUser(array('expire_date' => $past));

        // Act
        $actual = $u->isExpired();

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_37
     * Description: UserModel getDateTimeFormat trả về null khi user chưa available
     * Scenario Type: Negative
     * Expected Result: getDateTimeFormat() trả về null
     * Related Module: Authentication
     * Link Anchor: #TC_S1_37
     */
    public function testGetDateTimeFormatReturnsNullWhenNotAvailable(): void
    {
        // Arrange
        $u = new UserModel(0);
        $u->set('preferences', json_encode(array('dateformat' => 'Y-m-d', 'timeformat' => '24')));

        // Act
        $actual = $u->getDateTimeFormat();

        // Assert
        $this->assertNull($actual);
    }

    /**
     * Test Case ID: TC_S1_38
     * Description: UserModel getDateTimeFormat trả về định dạng 24h khi timeformat=24
     * Scenario Type: Positive
     * Expected Result: Chuỗi có hậu tố H:i
     * Related Module: Authentication
     * Link Anchor: #TC_S1_38
     */
    public function testGetDateTimeFormatReturns24hFormat(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array(
            'preferences' => json_encode(array('dateformat' => 'Y-m-d', 'timeformat' => '24')),
        ));

        // Act
        $actual = $u->getDateTimeFormat();

        // Assert
        $this->assertSame('Y-m-d H:i', $actual);
    }

    /**
     * Test Case ID: TC_S1_39
     * Description: UserModel getDateTimeFormat trả về định dạng 12h khi timeformat khác 24
     * Scenario Type: Positive
     * Expected Result: Chuỗi có hậu tố h:i A
     * Related Module: Authentication
     * Link Anchor: #TC_S1_39
     */
    public function testGetDateTimeFormatReturns12hFormat(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array(
            'preferences' => json_encode(array('dateformat' => 'd/m/Y', 'timeformat' => '12')),
        ));

        // Act
        $actual = $u->getDateTimeFormat();

        // Assert
        $this->assertSame('d/m/Y h:i A', $actual);
    }

    /**
     * Test Case ID: TC_S1_40
     * Description: UserModel isEmailVerified trả về true khi không có email_verification_hash
     * Scenario Type: Positive
     * Expected Result: isEmailVerified() trả về true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_40
     */
    public function testIsEmailVerifiedReturnsTrueWhenHashMissing(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array('data' => json_encode(array())));

        // Act
        $actual = $u->isEmailVerified();

        // Assert
        $this->assertTrue($actual);
    }

    /**
     * Test Case ID: TC_S1_41
     * Description: UserModel isEmailVerified trả về false khi có email_verification_hash
     * Scenario Type: Negative
     * Expected Result: isEmailVerified() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_41
     */
    public function testIsEmailVerifiedReturnsFalseWhenHashPresent(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array(
            'data' => json_encode(array('email_verification_hash' => 'abc123')),
        ));

        // Act
        $actual = $u->isEmailVerified();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_42
     * Description: isAdmin thất bại khi role chứa khoảng trắng không hợp lệ
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_42
     */
    public function testIsAdminReturnsFalseWhenRoleContainsExtraWhitespace()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('role', ' admin ');
        $auth->markAsAvailable();

        // Act
        $actual = $auth->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_43
     * Description: canEdit thất bại khi editor chưa available
     * Scenario Type: Negative
     * Expected Result: canEdit() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_43
     */
    public function testCanEditReturnsFalseWhenEditorNotAvailable(): void
    {
        // Arrange
        $editor = new UserModel(0);
        $editor->set('account_type', 'admin');

        $target = $this->makeAvailableUser(array(
            'account_type' => 'member',
            'id' => 2
        ));

        // Act
        $actual = $editor->canEdit($target);

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_44
     * Description: isAdmin thất bại khi role viết hoa khác chuẩn
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_44
     */
    public function testIsAdminReturnsFalseWhenRoleHasDifferentCase()
    {
        // Arrange
        $auth = new Auth();
        $auth->set('role', 'Admin');
        $auth->markAsAvailable();

        // Act
        $actual = $auth->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_45
     * Description: UserModel isAdmin thất bại khi account_type chứa khoảng trắng
     * Scenario Type: Negative
     * Expected Result: isAdmin() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_45
     */
    public function testIsAdminReturnsFalseWhenAccountTypeContainsWhitespace(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array(
            'account_type' => ' admin '
        ));

        // Act
        $actual = $u->isAdmin();

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_46
     * Description: canEdit thất bại khi admin cố chỉnh sửa developer
     * Scenario Type: Negative
     * Expected Result: canEdit() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_46
     */
    public function testCanEditReturnsFalseWhenAdminEditsDeveloper(): void
    {
        // Arrange
        $admin = $this->makeAvailableUser(array(
            'account_type' => 'admin',
            'id' => 1
        ));

        $developer = $this->makeAvailableUser(array(
            'account_type' => 'developer',
            'id' => 2
        ));

        // Act
        $actual = $admin->canEdit($developer);

        // Assert
        $this->assertFalse($actual);
    }

    /**
     * Test Case ID: TC_S1_47
     * Description: isEmailVerified thất bại khi email_verification_hash là empty string
     * Scenario Type: Negative
     * Expected Result: isEmailVerified() trả về false
     * Related Module: Authentication
     * Link Anchor: #TC_S1_47
     */
    public function testIsEmailVerifiedReturnsFalseWhenHashIsEmptyString(): void
    {
        // Arrange
        $u = $this->makeAvailableUser(array(
            'data' => json_encode(array(
                'email_verification_hash' => ''
            ))
        ));

        // Act
        $actual = $u->isEmailVerified();

        // Assert
        $this->assertFalse($actual);
    }

}

