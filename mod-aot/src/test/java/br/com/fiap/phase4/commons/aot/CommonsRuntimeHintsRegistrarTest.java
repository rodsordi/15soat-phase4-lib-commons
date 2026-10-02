package br.com.fiap.phase4.commons.aot;

import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommonsRuntimeHintsRegistrar Tests")
class CommonsRuntimeHintsRegistrarTest {

    @Test
    @DisplayName("Should register reflection hints for domain Value Objects without external JSON")
    void shouldRegisterHints() {
        var registrar = new CommonsRuntimeHintsRegistrar();
        var hints = new RuntimeHints();

        registrar.registerHints(hints, getClass().getClassLoader());

        assertThat(hints.reflection().typeHints())
                .anyMatch(hint -> hint.getType().equals(org.springframework.aot.hint.TypeReference.of(CpfOrCnpj.class)));
    }
}
