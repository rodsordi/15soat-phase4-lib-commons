package br.com.fiap.phase4.commons.domain.vo;

import br.com.fiap.phase4.commons.domain.exception.ValidationException;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) implements Serializable {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public Email {
        Objects.requireNonNull(value, "Email must not be null");
        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Invalid email address: " + value);
        }
        value = trimmed;
    }
}
