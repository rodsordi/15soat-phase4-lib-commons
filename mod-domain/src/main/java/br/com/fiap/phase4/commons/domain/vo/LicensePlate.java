package br.com.fiap.phase4.commons.domain.vo;

import br.com.fiap.phase4.commons.domain.exception.ValidationException;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public record LicensePlate(String value) implements Serializable {

    private static final Pattern MERCOSUL_PATTERN = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");
    private static final Pattern CLASSIC_PATTERN = Pattern.compile("^[A-Z]{3}[0-9]{4}$");

    public LicensePlate {
        Objects.requireNonNull(value, "License plate must not be null");
        String sanitized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!MERCOSUL_PATTERN.matcher(sanitized).matches() && !CLASSIC_PATTERN.matcher(sanitized).matches()) {
            throw new ValidationException("Invalid license plate format (must be ABC1234 or ABC1D23): " + value);
        }
        value = sanitized;
    }

    public boolean isMercosul() {
        return MERCOSUL_PATTERN.matcher(value).matches();
    }

    public String formatted() {
        if (isMercosul()) {
            return value;
        }
        return value.substring(0, 3) + "-" + value.substring(3);
    }
}
