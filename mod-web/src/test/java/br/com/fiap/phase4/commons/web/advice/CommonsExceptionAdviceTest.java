package br.com.fiap.phase4.commons.web.advice;

import br.com.fiap.phase4.commons.domain.exception.AlreadyExistsException;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.commons.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommonsExceptionAdvice Unit Tests")
class CommonsExceptionAdviceTest {

    private CommonsExceptionAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new CommonsExceptionAdvice();
    }

    @Test
    @DisplayName("Should translate ResourceNotFoundException to 404 ProblemDetail")
    void shouldHandleResourceNotFound() {
        var ex = new ResourceNotFoundException("WorkOrder", "123");
        ProblemDetail problem = advice.handleResourceNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).contains("WorkOrder with identifier '123' was not found");
        assertThat(problem.getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Should translate AlreadyExistsException to 409 ProblemDetail")
    void shouldHandleAlreadyExists() {
        var ex = new AlreadyExistsException("Vehicle", "ABC1234");
        ProblemDetail problem = advice.handleAlreadyExists(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).contains("Vehicle with identifier 'ABC1234' already exists");
        assertThat(problem.getProperties()).containsEntry("errorCode", "RESOURCE_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Should translate ValidationException to 422 ProblemDetail with errors list")
    void shouldHandleValidationException() {
        var ex = new ValidationException("Validation failed", List.of("Invalid CPF", "Invalid Plate"));
        ProblemDetail problem = advice.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getProperties()).containsKey("errors");
    }
}
