package br.com.fiap.phase4.commons.aot;

import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Email;
import br.com.fiap.phase4.commons.domain.vo.LicensePlate;
import br.com.fiap.phase4.commons.domain.vo.Money;
import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.event.EventMetadata;
import br.com.fiap.phase4.commons.web.dto.ErrorResponseDto;
import br.com.fiap.phase4.commons.web.dto.GenericResponseDto;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.io.Serializable;
import java.util.List;

public class CommonsRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        List<Class<? extends Serializable>> serializableClasses = List.of(
                CpfOrCnpj.class,
                LicensePlate.class,
                Email.class,
                Money.class,
                EventEnvelope.class,
                EventMetadata.class
        );

        List<Class<?>> otherBindingClasses = List.of(
                ErrorResponseDto.class,
                GenericResponseDto.class
        );

        for (Class<? extends Serializable> clazz : serializableClasses) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS
            );
        }

        for (Class<?> clazz : otherBindingClasses) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS
            );
        }
    }
}
