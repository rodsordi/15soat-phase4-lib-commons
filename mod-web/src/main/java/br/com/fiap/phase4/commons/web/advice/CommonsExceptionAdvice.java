package br.com.fiap.phase4.commons.web.advice;

import br.com.fiap.phase4.commons.domain.exception.AlreadyExistsException;
import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.commons.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CommonsExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(CommonsExceptionAdvice.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://api.fiap.com.br/errors/not-found"));
        enrichProblem(problem, "RESOURCE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(AlreadyExistsException ex) {
        log.warn("Resource already exists: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("https://api.fiap.com.br/errors/conflict"));
        enrichProblem(problem, "RESOURCE_ALREADY_EXISTS");
        return problem;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        log.warn("Domain validation error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("https://api.fiap.com.br/errors/validation-error"));
        problem.setProperty("errors", ex.getErrors());
        enrichProblem(problem, ex.getErrorCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Bean validation error: {}", fieldErrors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request payload attributes");
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://api.fiap.com.br/errors/bad-request"));
        problem.setProperty("fieldErrors", fieldErrors);
        enrichProblem(problem, "INVALID_ARGUMENT");
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("Domain error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setType(URI.create("https://api.fiap.com.br/errors/domain-error"));
        enrichProblem(problem, ex.getErrorCode());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.fiap.com.br/errors/internal-error"));
        enrichProblem(problem, "INTERNAL_ERROR");
        return problem;
    }

    private void enrichProblem(ProblemDetail problem, String errorCode) {
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now().toString());
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }
}
