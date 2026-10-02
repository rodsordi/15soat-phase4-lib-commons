package br.com.fiap.phase4.commons.domain.util;

import br.com.fiap.phase4.commons.domain.exception.ValidationException;

import java.util.Collection;
import java.util.Objects;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new ValidationException(message);
        }
    }

    public static void notBlank(String string, String message) {
        if (string == null || string.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException(message);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}
