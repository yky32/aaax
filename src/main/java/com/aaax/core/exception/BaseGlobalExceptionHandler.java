package com.aaax.core.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.aaax.core.response.R;
import com.aaax.core.response.Response;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Copied shape from qs app-core {@code BaseGlobalExceptionHandler} (no private ELK client).
 */
public class BaseGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("exception", ex.getClass().getSimpleName());
        String reason = Optional.ofNullable(ex.getMessage())
                .filter(s -> !s.isBlank())
                .orElse(Optional.ofNullable(status.getReasonPhrase()).orElse("Error"));
        data.put("message", reason);
        if (request instanceof ServletWebRequest swr) {
            data.put("path", swr.getRequest().getRequestURI());
        }

        Response envelope = envelopeForSpringMvcStatus(status, reason);
        Result<Object> result = R.error(envelope, data);
        result.setRequestId(UUID.randomUUID().toString());
        log.warn(
                "-- BaseGlobalExceptionHandler.handleExceptionInternal status={} {} path={}",
                status.value(),
                ex.getClass().getSimpleName(),
                data.get("path"));
        return new ResponseEntity<>(result, headers, statusCode);
    }

    private static Response envelopeForSpringMvcStatus(HttpStatus status, String message) {
        if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            return new Response("SYS9405", message, status);
        }
        if (status.is5xxServerError()) {
            return new Response("SYS9999", message, status);
        }
        return new Response("PAM0400", message, status);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = !ife.getPath().isEmpty() ? ife.getPath().get(0).getFieldName() : "unknown";
                String value = String.valueOf(ife.getValue());
                String validValues = Arrays.toString(ife.getTargetType().getEnumConstants());
                String message = String.format(
                        "Field '%s' with value '%s' is invalid! Accepted values are: %s",
                        fieldName, value, validValues);
                return new ResponseEntity<>(R.fail(SystemResponse.PAM0400, message), HttpStatus.BAD_REQUEST);
            }
        }
        log.error("-- BaseGlobalExceptionHandler.handleHttpMessageNotReadable, ex => {}", ex.getMessage());
        Map<String, Object> data = Map.of(
                "exception",
                ex.getClass().getSimpleName(),
                "message",
                Optional.ofNullable(ex.getMessage()).orElse("Malformed JSON / unreadable body"));
        return new ResponseEntity<>(R.error(SystemResponse.PAM0400, data), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> systemError(HttpServletRequest req, Exception exception) {
        Result<String> error = R.error(SystemResponse.SYS9999, exception.getMessage());
        error.setRequestId(UUID.randomUUID().toString());
        log.error("-- BaseGlobalExceptionHandler.systemError", exception);
        return new ResponseEntity<>(error, SystemResponse.SYS9999.getHttpStatus());
    }

    @ExceptionHandler(value = BizException.class)
    public ResponseEntity<Object> error(HttpServletRequest req, BizException bizException) {
        Result<Object> error = R.error(bizException.getResponse(), bizException.getData());
        error.setRequestId(UUID.randomUUID().toString());
        log.error(
                "-- BaseGlobalExceptionHandler.BizException code={} path={}",
                bizException.getResponse() != null ? bizException.getResponse().getCode() : null,
                req.getRequestURI());
        HttpStatus status = bizException.getResponse() != null && bizException.getResponse().getHttpStatus() != null
                ? bizException.getResponse().getHttpStatus()
                : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(error, status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> details = new ArrayList<>();
        for (ObjectError err : ex.getBindingResult().getAllErrors()) {
            details.add(err.getDefaultMessage());
        }
        return new ResponseEntity<>(R.invalid(details), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
                R.fail(SystemResponse.SYS9901, ex.getMessage()), SystemResponse.SYS9901.getHttpStatus());
    }
}
