package br.com.fiap.phase4.commons.domain.vo;

import br.com.fiap.phase4.commons.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Objects Domain Tests")
class ValueObjectsTest {

    @Nested
    @DisplayName("CpfOrCnpj Tests")
    class CpfOrCnpjTests {

        @Test
        @DisplayName("Should create valid CPF and format correctly")
        void shouldCreateValidCpf() {
            // Standard valid CPF algorithm test
            var doc = new CpfOrCnpj("52998224725");
            assertThat(doc.isCpf()).isTrue();
            assertThat(doc.isCnpj()).isFalse();
            assertThat(doc.formatted()).isEqualTo("529.982.247-25");
        }

        @Test
        @DisplayName("Should create valid CNPJ and format correctly")
        void shouldCreateValidCnpj() {
            var doc = new CpfOrCnpj("11.222.333/0001-81");
            assertThat(doc.isCnpj()).isTrue();
            assertThat(doc.isCpf()).isFalse();
            assertThat(doc.formatted()).isEqualTo("11.222.333/0001-81");
        }

        @ParameterizedTest
        @ValueSource(strings = {"11111111111", "12345678900", "00000000000", "invalid"})
        @DisplayName("Should throw ValidationException for invalid documents")
        void shouldRejectInvalidDocuments(String invalid) {
            assertThatThrownBy(() -> new CpfOrCnpj(invalid))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("LicensePlate Tests")
    class LicensePlateTests {

        @Test
        @DisplayName("Should accept valid Mercosul plate")
        void shouldAcceptMercosulPlate() {
            var plate = new LicensePlate("BRA2E19");
            assertThat(plate.isMercosul()).isTrue();
            assertThat(plate.formatted()).isEqualTo("BRA2E19");
        }

        @Test
        @DisplayName("Should accept valid classic plate")
        void shouldAcceptClassicPlate() {
            var plate = new LicensePlate("ABC-1234");
            assertThat(plate.isMercosul()).isFalse();
            assertThat(plate.formatted()).isEqualTo("ABC-1234");
        }

        @ParameterizedTest
        @ValueSource(strings = {"1234567", "ABCD123", "AB12345", "INVALID"})
        @DisplayName("Should reject invalid license plates")
        void shouldRejectInvalidLicensePlate(String invalid) {
            assertThatThrownBy(() -> new LicensePlate(invalid))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Email Tests")
    class EmailTests {

        @Test
        @DisplayName("Should accept valid email and normalize to lowercase")
        void shouldAcceptValidEmail() {
            var email = new Email("Test.User@example.com");
            assertThat(email.value()).isEqualTo("test.user@example.com");
        }

        @ParameterizedTest
        @ValueSource(strings = {"plainaddress", "@missingusername.com", "missing.domain@.com"})
        @DisplayName("Should reject invalid emails")
        void shouldRejectInvalidEmail(String invalid) {
            assertThatThrownBy(() -> new Email(invalid))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Money Tests")
    class MoneyTests {

        @Test
        @DisplayName("Should perform arithmetic operations correctly with scale 2")
        void shouldPerformMoneyOperations() {
            var m1 = Money.of(100.50);
            var m2 = Money.of("49.50");

            assertThat(m1.add(m2)).isEqualTo(Money.of("150.00"));
            assertThat(m1.subtract(m2)).isEqualTo(Money.of("51.00"));
            assertThat(m2.multiply(2)).isEqualTo(Money.of("99.00"));
            assertThat(m1.isPositive()).isTrue();
        }
    }
}
