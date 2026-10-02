package br.com.fiap.phase4.commons.domain.vo;

import br.com.fiap.phase4.commons.domain.exception.ValidationException;

import java.io.Serializable;
import java.util.Objects;

public record CpfOrCnpj(String value) implements Serializable {

    public CpfOrCnpj {
        Objects.requireNonNull(value, "Document value must not be null");
        String sanitized = value.replaceAll("\\D", "");
        if (sanitized.length() == 11) {
            if (!isValidCpf(sanitized)) {
                throw new ValidationException("Invalid CPF: " + value);
            }
        } else if (sanitized.length() == 14) {
            if (!isValidCnpj(sanitized)) {
                throw new ValidationException("Invalid CNPJ: " + value);
            }
        } else {
            throw new ValidationException("Document must have 11 (CPF) or 14 (CNPJ) digits: " + value);
        }
        value = sanitized;
    }

    public boolean isCpf() {
        return value.length() == 11;
    }

    public boolean isCnpj() {
        return value.length() == 14;
    }

    public String formatted() {
        if (isCpf()) {
            return value.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return value.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) return false;
        try {
            int d1 = 0, d2 = 0;
            for (int i = 0; i < 9; i++) {
                int digit = cpf.charAt(i) - '0';
                d1 += digit * (10 - i);
                d2 += digit * (11 - i);
            }
            int r1 = 11 - (d1 % 11);
            int check1 = (r1 >= 10) ? 0 : r1;
            d2 += check1 * 2;
            int r2 = 11 - (d2 % 11);
            int check2 = (r2 >= 10) ? 0 : r2;
            return check1 == (cpf.charAt(9) - '0') && check2 == (cpf.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidCnpj(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        try {
            int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int sum1 = 0, sum2 = 0;
            for (int i = 0; i < 12; i++) {
                int digit = cnpj.charAt(i) - '0';
                sum1 += digit * weights1[i];
                sum2 += digit * weights2[i];
            }
            int r1 = sum1 % 11;
            int check1 = (r1 < 2) ? 0 : 11 - r1;
            sum2 += check1 * weights2[12];
            int r2 = sum2 % 11;
            int check2 = (r2 < 2) ? 0 : 11 - r2;
            return check1 == (cnpj.charAt(12) - '0') && check2 == (cnpj.charAt(13) - '0');
        } catch (Exception e) {
            return false;
        }
    }
}
