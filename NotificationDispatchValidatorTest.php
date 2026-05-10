<?php

use PHPUnit\Framework\TestCase;

class NotificationDispatchValidatorTest extends TestCase
{
    private function makeValidator(
        $codec = null,
        $clock = null,
        $senders = null,
        $dedupe = null
    ): NotificationDispatchValidator {
        if ($codec === null) {
            $codec = $this->createMock(NotificationTokenCodec::class);
        }
        if ($clock === null) {
            $clock = $this->createMock(NotificationClock::class);
        }
        if ($senders === null) {
            $senders = $this->createMock(NotificationAccountRepository::class);
        }
        if ($dedupe === null) {
            $dedupe = $this->createMock(NotificationDedupeStore::class);
        }

        return new NotificationDispatchValidator($codec, $clock, $senders, $dedupe);
    }

    /**
     * Test Case ID: NOTIF-RCPT-001
     * Description: Người nhận có email hợp lệ được chấp nhận
     * Scenario Type: Positive
     * Expected Result: ok=true và recipient được chuẩn hoá
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-RCPT-001
     */
    public function testRecipientEmailValidPasses(): void
    {
        // Arrange
        $v = $this->makeValidator();

        // Act
        $result = $v->validateRecipientIdentity(' staff@cu.net ');

        // Assert
        $this->assertTrue($result->ok);
        $this->assertSame('staff@cu.net', $result->data['recipient']);
    }

    /**
     * Test Case ID: NOTIF-RCPT-002
     * Description: Email người nhận rỗng bị từ chối (tương đương empty credential nghiệp vụ)
     * Scenario Type: Negative
     * Expected Result: EMPTY_CREDENTIALS
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-RCPT-002
     */
    public function testRecipientFailsWhenBlank(): void
    {
        // Arrange
        $v = $this->makeValidator();

        // Act
        $result = $v->validateRecipientIdentity("\t ");

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('EMPTY_CREDENTIALS', $result->code);
    }

    /**
     * Test Case ID: NOTIF-RCPT-003
     * Description: Email người nhận sai định dạng bị từ chối
     * Scenario Type: Negative
     * Expected Result: INVALID_EMAIL
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-RCPT-003
     */
    public function testRecipientFailsWhenInvalidEmail(): void
    {
        // Arrange
        $v = $this->makeValidator();

        // Act
        $result = $v->validateRecipientIdentity('khong-phai-email');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_EMAIL', $result->code);
    }

    /**
     * Test Case ID: NOTIF-DDP-004
     * Description: Mã gửi trùng (dedupe) chưa tồn tại được chấp nhận
     * Scenario Type: Positive
     * Expected Result: ok=true
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-DDP-004
     */
    public function testDedupeKeyUnusedPasses(): void
    {
        // Arrange
        $dedupe = $this->createMock(NotificationDedupeStore::class);
        $dedupe->expects($this->once())->method('exists')->with('evt-xyz')->willReturn(false);
        $v = $this->makeValidator(null, null, null, $dedupe);

        // Act
        $result = $v->assertDedupeKeyUnused('evt-xyz');

        // Assert
        $this->assertTrue($result->ok);
    }

    /**
     * Test Case ID: NOTIF-DDP-005
     * Description: Mã dedupe đã được sử dụng bị từ chối (tránh gửi trùng một sự kiện)
     * Scenario Type: Negative
     * Expected Result: DUPLICATED_EMAIL
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-DDP-005
     */
    public function testDedupeFailsWhenKeyAlreadySent(): void
    {
        // Arrange
        $dedupe = $this->createMock(NotificationDedupeStore::class);
        $dedupe->method('exists')->willReturn(true);
        $v = $this->makeValidator(null, null, null, $dedupe);

        // Act
        $result = $v->assertDedupeKeyUnused('booking-123');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('DUPLICATED_EMAIL', $result->code);
    }

    /**
     * Test Case ID: NOTIF-DDP-006
     * Description: Dedupe key rỗng là validation failure nghiệp vụ
     * Scenario Type: Negative
     * Expected Result: VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-DDP-006
     */
    public function testDedupeFailsWhenKeyBlank(): void
    {
        // Arrange
        $dedupe = $this->createMock(NotificationDedupeStore::class);
        $dedupe->expects($this->never())->method('exists');
        $v = $this->makeValidator(null, null, null, $dedupe);

        // Act
        $result = $v->assertDedupeKeyUnused('');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }

    /**
     * Test Case ID: NOTIF-JWT-007
     * Description: Sender có token và claims hợp lệ, role được phép, tài khoản không inactive
     * Scenario Type: Positive
     * Expected Result: Authorize succeeds
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-007
     */
    public function testSenderTokenAuthorizedPasses(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $clock = $this->createMock(NotificationClock::class);
        $senders = $this->createMock(NotificationAccountRepository::class);
        $clock->method('now')->willReturn(100);
        $codec->method('decodeClaims')->willReturn(array(
            'email' => 'doc@hos.org',
            'role' => 'admin',
            'exp' => 200,
        ));
        $senders->method('findSenderByEmail')->with('doc@hos.org')->willReturn(array('is_active' => 1));
        $v = $this->makeValidator($codec, $clock, $senders);

        // Act
        $result = $v->authorizeSenderToken('tok', array('admin', 'doctor'));

        // Assert
        $this->assertTrue($result->ok);
        $this->assertSame('doc@hos.org', $result->data['sender_email']);
    }

    /**
     * Test Case ID: NOTIF-JWT-008
     * Description: Token rỗng bị INVALID_TOKEN (tương đương không có JWT)
     * Scenario Type: Negative
     * Expected Result: INVALID_TOKEN
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-008
     */
    public function testSenderFailsWhenTokenEmpty(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $codec->expects($this->never())->method('decodeClaims');
        $v = $this->makeValidator($codec);

        // Act
        $result = $v->authorizeSenderToken('  ', array('admin'));

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: NOTIF-JWT-009
     * Description: Token không đọc được claims bị INVALID_TOKEN
     * Scenario Type: Negative
     * Expected Result: INVALID_TOKEN
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-009
     */
    public function testSenderFailsWhenTokenCannotBeDecoded(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $codec->method('decodeClaims')->willReturn(null);
        $v = $this->makeValidator($codec);

        // Act
        $result = $v->authorizeSenderToken('bad', array());

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: NOTIF-JWT-010
     * Description: Token đã quá hạn theo clock bị EXPIRED_TOKEN
     * Scenario Type: Negative
     * Expected Result: EXPIRED_TOKEN
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-010
     */
    public function testSenderFailsWhenTokenExpired(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $clock = $this->createMock(NotificationClock::class);
        $clock->method('now')->willReturn(500);
        $codec->method('decodeClaims')->willReturn(array(
            'email' => 'doc@hos.org',
            'role' => 'admin',
            'exp' => 400,
        ));
        $v = $this->makeValidator($codec, $clock);

        // Act
        $result = $v->authorizeSenderToken('expired', array('admin'));

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('EXPIRED_TOKEN', $result->code);
    }

    /**
     * Test Case ID: NOTIF-JWT-011
     * Description: Role trong token không nằm trong danh sách được phép
     * Scenario Type: Negative
     * Expected Result: UNAUTHORIZED_ROLE
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-011
     */
    public function testSenderFailsWhenRoleNotAllowed(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $clock = $this->createMock(NotificationClock::class);
        $clock->method('now')->willReturn(1);
        $codec->method('decodeClaims')->willReturn(array(
            'email' => 'guest@hos.org',
            'role' => 'member',
            'exp' => 999,
        ));
        $v = $this->makeValidator($codec, $clock);

        // Act
        $result = $v->authorizeSenderToken('jwt', array('admin'));

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('UNAUTHORIZED_ROLE', $result->code);
    }

    /**
     * Test Case ID: NOTIF-JWT-012
     * Description: Sender inactive trong catalog bị INACTIVE_ACCOUNT
     * Scenario Type: Negative
     * Expected Result: INACTIVE_ACCOUNT
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-012
     */
    public function testSenderFailsWhenAccountInactive(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $clock = $this->createMock(NotificationClock::class);
        $senders = $this->createMock(NotificationAccountRepository::class);
        $clock->method('now')->willReturn(1);
        $codec->method('decodeClaims')->willReturn(array(
            'email' => 'off@hos.org',
            'role' => 'admin',
            'exp' => 99,
        ));
        $senders->method('findSenderByEmail')->willReturn(array('is_active' => 0));
        $v = $this->makeValidator($codec, $clock, $senders);

        // Act
        $result = $v->authorizeSenderToken('tok', array('admin'));

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INACTIVE_ACCOUNT', $result->code);
    }

    /**
     * Test Case ID: NOTIF-JWT-013
     * Description: Email trong claims token sai định dạng
     * Scenario Type: Negative
     * Expected Result: INVALID_EMAIL
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-JWT-013
     */
    public function testSenderFailsWhenEmailInTokenMalformed(): void
    {
        // Arrange
        $codec = $this->createMock(NotificationTokenCodec::class);
        $clock = $this->createMock(NotificationClock::class);
        $clock->method('now')->willReturn(1);
        $codec->method('decodeClaims')->willReturn(array(
            'email' => 'not-email',
            'role' => 'admin',
            'exp' => 99,
        ));
        $senders = $this->createMock(NotificationAccountRepository::class);
        $senders->expects($this->never())->method('findSenderByEmail');
        $v = $this->makeValidator($codec, $clock, $senders);

        // Act
        $result = $v->authorizeSenderToken('tok', array());

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_EMAIL', $result->code);
    }

    /**
     * Test Case ID: NOTIF-BODY-014
     * Description: Nội dung thông báo đủ độ dài tối thiểu UTF-8 hợp lệ
     * Scenario Type: Positive
     * Expected Result: ok=true
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-BODY-014
     */
    public function testBodyPolicyPassesAtMinimumLength(): void
    {
        // Arrange
        $v = $this->makeValidator();

        $plain = 'Xin chào bệnh nhân';
        $expectedLen = function_exists('mb_strlen')
            ? mb_strlen($plain, 'UTF-8')
            : strlen($plain);

        // Act
        $result = $v->validateMessageBodyPolicy($plain);

        // Assert
        $this->assertTrue($result->ok);
        $this->assertSame($expectedLen, $result->data['length']);
    }

    /**
     * Test Case ID: NOTIF-BODY-015
     * Description: Nội dung rỗng bị VALIDATION_FAILURE
     * Scenario Type: Negative
     * Expected Result: VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-BODY-015
     */
    public function testBodyPolicyFailsEmpty(): void
    {
        // Arrange
        $v = $this->makeValidator();

        // Act
        $result = $v->validateMessageBodyPolicy('  ');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }

    /**
     * Test Case ID: NOTIF-BODY-016
     * Description: Nội dung quá ngắn bị PASSWORD_POLICY_VIOLATION (chính sách nội dung)
     * Scenario Type: Negative
     * Expected Result: PASSWORD_POLICY_VIOLATION
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-BODY-016
     */
    public function testBodyPolicyFailsTooShort(): void
    {
        // Arrange
        $v = $this->makeValidator();

        // Act
        $result = $v->validateMessageBodyPolicy('Hi:(');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('PASSWORD_POLICY_VIOLATION', $result->code);
    }

    /**
     * Test Case ID: NOTIF-WH-017
     * Description: Sai secret webhook (tương đương wrong password) bị INVALID_CREDENTIALS
     * Scenario Type: Negative
     * Expected Result: INVALID_CREDENTIALS
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-WH-017
     */
    public function testWebhookSecretWrongFails(): void
    {
        // Arrange
        $v = $this->makeValidator();

        // Act
        $result = $v->assertWebhookSecretMatches('client-wrong', 'server-ok-xx');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_CREDENTIALS', $result->code);
    }

    /**
     * Test Case ID: NOTIF-WH-018
     * Description: Secret webhook khớp hash_equals thì thành công
     * Scenario Type: Positive
     * Expected Result: ok=true
     * Related Module: Drug & Notification
     * Link Anchor: #NOTIF-WH-018
     */
    public function testWebhookSecretMatchPasses(): void
    {
        // Arrange
        $v = $this->makeValidator();
        $secret = str_repeat('a', 24);

        // Act
        $result = $v->assertWebhookSecretMatches($secret, $secret);

        // Assert
        $this->assertTrue($result->ok);
    }
}
