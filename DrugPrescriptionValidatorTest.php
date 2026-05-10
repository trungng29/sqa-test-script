<?php

use PHPUnit\Framework\TestCase;

class DrugPrescriptionValidatorTest extends TestCase
{
    /** @var DrugCatalogRepository|\PHPUnit\Framework\MockObject\MockObject */
    private $catalog;

    /** @var DrugPrescriptionValidator */
    private $validator;

    protected function setUp(): void
    {
        parent::setUp();
        $this->catalog = $this->createMock(DrugCatalogRepository::class);
        $this->validator = new DrugPrescriptionValidator($this->catalog);
    }

    /**
     * Test Case ID: DRUG-RXLINE-001
     * Description: Dòng đơn hợp lệ được chấp nhận khi thuốc có trong danh mục và đang hoạt động
     * Scenario Type: Positive
     * Expected Result: Trả kết quả ok=true và chứa drug_id, quantity
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-001
     */
    public function testValidateLineAcceptsWellFormedActiveDrug(): void
    {
        // Arrange
        $this->catalog->method('findById')->willReturnMap(array(
            array('Par1', array('id' => 'Par1', 'name' => 'Paracetamol', 'is_active' => true)),
        ));
        $item = array('drug_id' => 'Par1', 'quantity' => 2, 'dosage' => '500 mg x 2 lần/ngày');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertTrue($result->ok);
        $this->assertSame('Par1', $result->data['drug_id']);
        $this->assertSame(2, $result->data['quantity']);
    }

    /**
     * Test Case ID: DRUG-RXLINE-002
     * Description: Từ chối khi mã thuốc để trống (credential nghiệp vụ đơn hàng trống)
     * Scenario Type: Negative
     * Expected Result: ok=false và code=EMPTY_CREDENTIALS
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-002
     */
    public function testValidateLineFailsWhenDrugIdMissing(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');
        $item = array('drug_id' => '   ', 'quantity' => 1, 'dosage' => '10 ml trong ngày');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('EMPTY_CREDENTIALS', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-003
     * Description: Từ chối khi liều dùng để trống
     * Scenario Type: Negative
     * Expected Result: code=VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-003
     */
    public function testValidateLineFailsWhenDosageBlank(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');
        $item = array('drug_id' => 'X1', 'quantity' => 1, 'dosage' => '');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-004
     * Description: Từ chối khi liều dùng không đạt chính sách định dạng (thiếu bối cảnh sau đơn vị)
     * Scenario Type: Negative
     * Expected Result: code=PASSWORD_POLICY_VIOLATION
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-004
     */
    public function testValidateLineFailsWhenDosageFailsPolicyTooShortDetail(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');
        $item = array('drug_id' => 'X1', 'quantity' => 1, 'dosage' => '250mg');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('PASSWORD_POLICY_VIOLATION', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-005
     * Description: Từ chối khi số lượng không phải số nguyên
     * Scenario Type: Negative
     * Expected Result: code=VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-005
     */
    public function testValidateLineFailsWhenQuantityNotInteger(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');
        $item = array('drug_id' => 'X1', 'quantity' => 2.5, 'dosage' => '10 ml trong ngày');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-006
     * Description: Từ chối khi số lượng < 1
     * Scenario Type: Negative
     * Expected Result: code=VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-006
     */
    public function testValidateLineFailsWhenQuantityLessThanOne(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');
        $item = array('drug_id' => 'X1', 'quantity' => 0, 'dosage' => '10 ml trong ngày');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-007
     * Description: Từ chối khi thuốc không có trong danh mục
     * Scenario Type: Negative
     * Expected Result: code=DRUG_UNKNOWN
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-007
     */
    public function testValidateLineFailsWhenDrugNotInCatalog(): void
    {
        // Arrange
        $this->catalog->method('findById')->with('Nope')->willReturn(null);
        $item = array('drug_id' => 'Nope', 'quantity' => 1, 'dosage' => '10 ml trong ngày');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('DRUG_UNKNOWN', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-008
     * Description: Từ chối khi thuốc không còn hoạt động trong danh mục (ngưng sử dụng)
     * Scenario Type: Negative
     * Expected Result: code=INACTIVE_ACCOUNT
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-008
     */
    public function testValidateLineFailsWhenDrugInactiveInCatalog(): void
    {
        // Arrange
        $this->catalog->method('findById')->willReturn(array(
            'id' => 'Old1',
            'name' => 'Thuốc ngưng',
            'is_active' => false,
        ));
        $item = array('drug_id' => 'Old1', 'quantity' => 3, 'dosage' => '2 viên sau ăn');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('INACTIVE_ACCOUNT', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXLINE-009
     * Description: Liều dùng có số thập phân và dấu gạch vẫn hợp lệ khi có mô tả sau đơn vị
     * Scenario Type: Positive
     * Expected Result: ok=true
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXLINE-009
     */
    public function testValidateLineAcceptsDecimalDosageWithFrequency(): void
    {
        // Arrange
        $this->catalog->method('findById')->willReturn(array(
            'id' => 'Amp',
            'is_active' => 1,
        ));
        $item = array('drug_id' => 'Amp', 'quantity' => 1, 'dosage' => '12.5 mg/ngày');

        // Act
        $result = $this->validator->validateLineItem($item);

        // Assert
        $this->assertTrue($result->ok);
    }

    /**
     * Test Case ID: DRUG-RXFULL-010
     * Description: Đơn nhiều dòng hợp lệ được chấp nhận
     * Scenario Type: Positive
     * Expected Result: line_count đúng số dòng
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXFULL-010
     */
    public function testValidatePrescriptionAcceptsDistinctLines(): void
    {
        // Arrange
        $this->catalog->method('findById')->willReturnCallback(function ($id) {
            return array('id' => $id, 'is_active' => true);
        });
        $lines = array(
            array('drug_id' => 'A', 'quantity' => 1, 'dosage' => '500 mg/ngày'),
            array('drug_id' => 'B', 'quantity' => 2, 'dosage' => '10 ml sáng tối'),
        );

        // Act
        $result = $this->validator->validatePrescription($lines);

        // Assert
        $this->assertTrue($result->ok);
        $this->assertSame(2, $result->data['line_count']);
    }

    /**
     * Test Case ID: DRUG-RXFULL-011
     * Description: Đơn rỗng bị từ chối
     * Scenario Type: Negative
     * Expected Result: VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXFULL-011
     */
    public function testValidatePrescriptionFailsWhenEmpty(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');

        // Act
        $result = $this->validator->validatePrescription(array());

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXFULL-012
     * Description: Trùng cùng mã thuốc trong một đơn bị từ chối (quy tắc trùng nghiệp vụ)
     * Scenario Type: Negative
     * Expected Result: code=DUPLICATED_EMAIL
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXFULL-012
     */
    public function testValidatePrescriptionFailsOnDuplicateDrugId(): void
    {
        // Arrange
        $this->catalog->method('findById')->willReturn(array('id' => 'Dup', 'is_active' => true));
        $lines = array(
            array('drug_id' => 'Dup', 'quantity' => 1, 'dosage' => '500 mg/ngày'),
            array('drug_id' => 'Dup', 'quantity' => 2, 'dosage' => '500 mg tối'),
        );

        // Act
        $result = $this->validator->validatePrescription($lines);

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('DUPLICATED_EMAIL', $result->code);
    }

    /**
     * Test Case ID: DRUG-RXFULL-013
     * Description: Phần tử không phải mảng trong danh sách dòng là lỗi validation
     * Scenario Type: Negative
     * Expected Result: VALIDATION_FAILURE
     * Related Module: Drug & Notification
     * Link Anchor: #DRUG-RXFULL-013
     */
    public function testValidatePrescriptionFailsWhenLineNotArray(): void
    {
        // Arrange
        $this->catalog->expects($this->never())->method('findById');

        // Act
        $result = $this->validator->validatePrescription(array(null));

        // Assert
        $this->assertFalse($result->ok);
        $this->assertSame('VALIDATION_FAILURE', $result->code);
    }
}
