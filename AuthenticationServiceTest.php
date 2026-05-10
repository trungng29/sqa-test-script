<?php

use PHPUnit\Framework\TestCase;

class AuthenticationServiceTest extends TestCase
{
    private function makeService($http = null, $codec = null, $clock = null, $users = null)
    {
        if (!$http) {
            $http = $this->createMock(AuthenticationHttpClient::class);
        }
        if (!$codec) {
            $codec = $this->createMock(AuthenticationTokenCodec::class);
        }
        if (!$clock) {
            $clock = $this->createMock(AuthenticationClock::class);
        }
        if (!$users) {
            $users = $this->createMock(AuthenticationUserRepository::class);
        }

        return new AuthenticationService($http, $codec, $clock, $users);
    }

    private function makeAvailableUser(array $fields)
    {
        $user = new DataEntry();
        foreach ($fields as $k => $v) {
            $user->set($k, $v, true);
        }
        $user->markAsAvailable();
        return $user;
    }

    /**
     * Test Case ID: TC_S1_10
     * Description: User có thể login với email/mật khẩu hợp lệ và nhận accessToken
     * Scenario Type: Positive
     * Expected Result: Kết quả ok=true và data là accessToken
     * Related Module: Authentication
     * Link Anchor: #TC_S1_10
     */
    public function testLoginSucceedsWithValidCredentials()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $clock = $this->createMock(AuthenticationClock::class);
        $users = $this->createMock(AuthenticationUserRepository::class);

        $users->expects($this->once())
            ->method('findByEmail')
            ->with('user@example.com')
            ->willReturn(null);

        $http->expects($this->once())
            ->method('postLogin')
            ->with('user@example.com', 'Password1')
            ->willReturn(array('result' => 1, 'accessToken' => 'token-123'));

        $service = $this->makeService($http, $codec, $clock, $users);

        // Act
        $result = $service->login('user@example.com', 'Password1');

        // Assert
        $this->assertTrue($result->ok);
        $this->assertSame('token-123', $result->data);
        $this->assertNull($result->code);
    }

    /**
     * Test Case ID: TC_S1_11
     * Description: Login thất bại khi email trống
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi EMPTY_CREDENTIALS
     * Related Module: Authentication
     * Link Anchor: #TC_S1_11
     */
    public function testLoginFailsWhenEmailIsEmpty()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $http->expects($this->never())->method('postLogin');

        $service = $this->makeService($http);

        // Act
        $result = $service->login('', 'Password1');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('EMPTY_CREDENTIALS', $result->code);
    }

    /**
     * Test Case ID: TC_S1_12
     * Description: Login thất bại khi mật khẩu trống
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi EMPTY_CREDENTIALS
     * Related Module: Authentication
     * Link Anchor: #TC_S1_12
     */
    public function testLoginFailsWhenPasswordIsEmpty()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $http->expects($this->never())->method('postLogin');

        $service = $this->makeService($http);

        // Act
        $result = $service->login('user@example.com', '');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('EMPTY_CREDENTIALS', $result->code);
    }

    /**
     * Test Case ID: TC_S1_13
     * Description: Login thất bại khi email sai định dạng
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_EMAIL
     * Related Module: Authentication
     * Link Anchor: #TC_S1_13
     */
    public function testLoginFailsWhenEmailFormatIsInvalid()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $http->expects($this->never())->method('postLogin');

        $service = $this->makeService($http);

        // Act
        $result = $service->login('not-an-email', 'Password1');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_EMAIL', $result->code);
    }

    /**
     * Test Case ID: TC_S1_14
     * Description: Login thất bại khi vi phạm password policy
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi PASSWORD_POLICY_VIOLATION
     * Related Module: Authentication
     * Link Anchor: #TC_S1_14
     */
    public function testLoginFailsWhenPasswordPolicyIsViolated()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $http->expects($this->never())->method('postLogin');

        $service = $this->makeService($http);

        // Act
        $result = $service->login('user@example.com', 'short');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('PASSWORD_POLICY_VIOLATION', $result->code);
    }

    /**
     * Test Case ID: TC_S1_15
     * Description: Login thất bại khi tài khoản inactive
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INACTIVE_ACCOUNT
     * Related Module: Authentication
     * Link Anchor: #TC_S1_15
     */
    public function testLoginFailsWhenAccountIsInactive()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $http->expects($this->never())->method('postLogin');

        $users = $this->createMock(AuthenticationUserRepository::class);
        $inactive = $this->makeAvailableUser(array('is_active' => 0));
        $users->expects($this->once())
            ->method('findByEmail')
            ->with('user@example.com')
            ->willReturn($inactive);

        $service = $this->makeService($http, null, null, $users);

        // Act
        $result = $service->login('user@example.com', 'Password1');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INACTIVE_ACCOUNT', $result->code);
    }

    /**
     * Test Case ID: TC_S1_16
     * Description: Login thất bại khi mật khẩu sai (API trả result=0)
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_CREDENTIALS
     * Related Module: Authentication
     * Link Anchor: #TC_S1_16
     */
    public function testLoginFailsWhenPasswordIsWrong()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $users = $this->createMock(AuthenticationUserRepository::class);
        $users->method('findByEmail')->willReturn(null);

        $http->expects($this->once())
            ->method('postLogin')
            ->with('user@example.com', 'Password1')
            ->willReturn(array('result' => 0));

        $service = $this->makeService($http, null, null, $users);

        // Act
        $result = $service->login('user@example.com', 'Password1');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_CREDENTIALS', $result->code);
    }

    /**
     * Test Case ID: TC_S1_17
     * Description: Login thất bại khi accessToken rỗng dù result=1
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_17
     */
    public function testLoginFailsWhenAccessTokenIsMissing()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $users = $this->createMock(AuthenticationUserRepository::class);
        $users->method('findByEmail')->willReturn(null);

        $http->expects($this->once())
            ->method('postLogin')
            ->willReturn(array('result' => 1, 'accessToken' => ''));

        $service = $this->makeService($http, null, null, $users);

        // Act
        $result = $service->login('user@example.com', 'Password1');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: TC_S1_18
     * Description: Xác thực token hợp lệ trả về claims
     * Scenario Type: Positive
     * Expected Result: ok=true và data chứa claims
     * Related Module: Authentication
     * Link Anchor: #TC_S1_18
     */
    public function testAuthenticateFromTokenSucceedsWithValidToken()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $clock = $this->createMock(AuthenticationClock::class);

        $clock->method('now')->willReturn(1000);
        $codec->expects($this->once())
            ->method('decode')
            ->with('token-abc')
            ->willReturn(array('email' => 'user@example.com', 'role' => 'doctor', 'exp' => 2000));

        $service = $this->makeService(null, $codec, $clock);

        // Act
        $result = $service->authenticateFromToken('token-abc');

        // Assert
        $this->assertTrue($result->ok);
        $this->assertIsArray($result->data);
        $this->assertSame('doctor', $result->data['role']);
    }

    /**
     * Test Case ID: TC_S1_19
     * Description: Xác thực thất bại khi token rỗng
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_19
     */
    public function testAuthenticateFromTokenFailsWhenTokenIsEmpty()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $codec->expects($this->never())->method('decode');

        $service = $this->makeService(null, $codec);

        // Act
        $result = $service->authenticateFromToken('');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: TC_S1_20
     * Description: Xác thực thất bại khi token không decode được
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_20
     */
    public function testAuthenticateFromTokenFailsWhenTokenIsInvalid()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $codec->expects($this->once())
            ->method('decode')
            ->with('bad-token')
            ->willReturn(null);

        $service = $this->makeService(null, $codec);

        // Act
        $result = $service->authenticateFromToken('bad-token');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: TC_S1_21
     * Description: Xác thực thất bại khi token hết hạn
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi EXPIRED_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_21
     */
    public function testAuthenticateFromTokenFailsWhenTokenIsExpired()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $clock = $this->createMock(AuthenticationClock::class);

        $clock->method('now')->willReturn(2000);
        $codec->method('decode')->willReturn(array('email' => 'user@example.com', 'role' => 'doctor', 'exp' => 1500));

        $service = $this->makeService(null, $codec, $clock);

        // Act
        $result = $service->authenticateFromToken('token-expired');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('EXPIRED_TOKEN', $result->code);
    }

    /**
     * Test Case ID: TC_S1_22
     * Description: Xác thực thất bại khi role không được phép
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi UNAUTHORIZED_ROLE
     * Related Module: Authentication
     * Link Anchor: #TC_S1_22
     */
    public function testAuthenticateFromTokenFailsWhenRoleIsUnauthorized()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $clock = $this->createMock(AuthenticationClock::class);
        $clock->method('now')->willReturn(1000);

        $codec->method('decode')->willReturn(array('email' => 'user@example.com', 'role' => 'doctor', 'exp' => 2000));

        $service = $this->makeService(null, $codec, $clock);

        // Act
        $result = $service->authenticateFromToken('token-role', array('admin'));

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('UNAUTHORIZED_ROLE', $result->code);
    }

    /**
     * Test Case ID: TC_S1_23
     * Description: Kiểm tra email không trùng khi chưa tồn tại
     * Scenario Type: Positive
     * Expected Result: ok=true
     * Related Module: Authentication
     * Link Anchor: #TC_S1_23
     */
    public function testAssertEmailNotDuplicatedSucceedsWhenEmailNotExists()
    {
        // Arrange
        $users = $this->createMock(AuthenticationUserRepository::class);
        $users->expects($this->once())
            ->method('findByEmail')
            ->with('new@example.com')
            ->willReturn(null);

        $service = $this->makeService(null, null, null, $users);

        // Act
        $result = $service->assertEmailNotDuplicated('new@example.com');

        // Assert
        $this->assertTrue($result->ok);
    }

    /**
     * Test Case ID: TC_S1_24
     * Description: Kiểm tra email trùng trả về lỗi DUPLICATED_EMAIL
     * Scenario Type: Negative
     * Expected Result: ok=false và code=DUPLICATED_EMAIL
     * Related Module: Authentication
     * Link Anchor: #TC_S1_24
     */
    public function testAssertEmailNotDuplicatedFailsWhenEmailIsDuplicated()
    {
        // Arrange
        $users = $this->createMock(AuthenticationUserRepository::class);
        $users->expects($this->once())
            ->method('findByEmail')
            ->with('dup@example.com')
            ->willReturn($this->makeAvailableUser(array('email' => 'dup@example.com')));

        $service = $this->makeService(null, null, null, $users);

        // Act
        $result = $service->assertEmailNotDuplicated('dup@example.com');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('DUPLICATED_EMAIL', $result->code);
    }

    /**
     * Test Case ID: TC_S1_25
     * Description: Kiểm tra email trùng thất bại khi email sai định dạng
     * Scenario Type: Negative
     * Expected Result: ok=false và code=INVALID_EMAIL
     * Related Module: Authentication
     * Link Anchor: #TC_S1_25
     */
    public function testAssertEmailNotDuplicatedFailsWhenEmailFormatIsInvalid()
    {
        // Arrange
        $users = $this->createMock(AuthenticationUserRepository::class);
        $users->expects($this->never())->method('findByEmail');

        $service = $this->makeService(null, null, null, $users);

        // Act
        $result = $service->assertEmailNotDuplicated('bad-email');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_EMAIL', $result->code);
    }

    /**
     * Test Case ID: TC_S1_26
     * Description: authenticateFromToken thất bại khi token thiếu role
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_26
     */
    public function testAuthenticateFailsWhenTokenRoleMissing()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $clock = $this->createMock(AuthenticationClock::class);

        $clock->method('now')->willReturn(1000);

        $codec->method('decode')->willReturn(array(
            'email' => 'user@example.com',
            'exp' => 2000
        ));

        $service = $this->makeService(null, $codec, $clock);

        // Act
        $result = $service->authenticateFromToken('bad-token');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: TC_S1_27
     * Description: authenticateFromToken thất bại khi exp không phải numeric
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_27
     */
    public function testAuthenticateFailsWhenTokenExpiryIsString()
    {
        // Arrange
        $codec = $this->createMock(AuthenticationTokenCodec::class);
        $clock = $this->createMock(AuthenticationClock::class);

        $clock->method('now')->willReturn(1000);

        $codec->method('decode')->willReturn(array(
            'email' => 'user@example.com',
            'role' => 'admin',
            'exp' => 'tomorrow'
        ));

        $service = $this->makeService(null, $codec, $clock);

        // Act
        $result = $service->authenticateFromToken('bad-token');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

    /**
     * Test Case ID: TC_S1_28
     * Description: Login thất bại khi accessToken chỉ chứa whitespace
     * Scenario Type: Negative
     * Expected Result: Trả về lỗi INVALID_TOKEN
     * Related Module: Authentication
     * Link Anchor: #TC_S1_28
     */
    public function testLoginFailsWhenAccessTokenContainsOnlyWhitespace()
    {
        // Arrange
        $http = $this->createMock(AuthenticationHttpClient::class);
        $users = $this->createMock(AuthenticationUserRepository::class);

        $users->method('findByEmail')->willReturn(null);

        $http->method('postLogin')->willReturn(array(
            'result' => 1,
            'accessToken' => '   '
        ));

        $service = $this->makeService($http, null, null, $users);

        // Act
        $result = $service->login('user@example.com', 'Password1');

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INVALID_TOKEN', $result->code);
    }

}

