package br.com.fiap.phase4.commons.domain.vo;

import br.com.fiap.phase4.commons.domain.exception.ValidationException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) implements Serializable, Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String value) {
        try {
            return new Money(new BigDecimal(value));
        } catch (Exception e) {
            throw new ValidationException("Invalid monetary value: " + value);
        }
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor must not be null");
        return new Money(this.amount.multiply(factor));
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public int compareTo(Money o) {
        return this.amount.compareTo(o.amount);
    }
}
