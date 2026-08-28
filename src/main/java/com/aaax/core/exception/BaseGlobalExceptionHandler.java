package com.aaax.core.exception;


import tools.jackson.databind.exc.InvalidFormatException;
import com.aaax.core.api.ApiClient;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.response.R;
import com.aaax.core.response.Response;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.handler.EndpointHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

@Slf4j
public class BaseGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private ApiClient apiClient;

    public BaseGlobalExceptionHandler(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public BaseGlobalExceptionHandler() {
    }

    /**
     * Normalizes Spring MVC / Boot default error bodies ({@code timestamp}, {@code status}, {@code error}, {@code path})
     * into the standard {@link Result} envelope used elsewhere (same shape as {@link BizException} responses).
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("exception", ex.getClass().getSimpleName());
        String reason = Optional.ofNullable(ex.getMessage()).filter(s -> !s.isBlank()).orElse(Optional.ofNullable(status.getReasonPhrase()).orElse("Error"));
        data.put("message", reason);
        if (request instanceof ServletWebRequest swr) {
            data.put("path", swr.getRequest().getRequestURI());
        }
        if (body instanceof ProblemDetail pd && pd.getDetail() != null) {
            data.put("detail", pd.getDetail());
        }

        Response envelope = envelopeForSpringMvcStatus(status, reason);
        Result<Object> result = R.error(envelope, data);
        try {
            result.setRequestId(AppContextHolder.CONTEXT.get().getRequestContext().getRequestId());
        } catch (Exception ignored) {
            // no request context (e.g. some async paths)
        }

        log.warn("-- BaseGlobalExceptionHandler.handleExceptionInternal status={} {} path={}", status.value(), ex.getClass().getSimpleName(), data.get("path"));
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
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException ife) {

            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = !ife.getPath().isEmpty() ? ife.getPath().get(0).getPropertyName() : "unknown";
                String value = String.valueOf(ife.getValue());
                String validValues = Arrays.toString(ife.getTargetType().getEnumConstants());
                String message = String.format("Field '%s' with value '%s' is invalid! Accepted values are: %s", fieldName, value, validValues);

                return new ResponseEntity<>(R.fail(SystemResponse.PAM0400, message), HttpStatus.BAD_REQUEST);
            }
        }
        log.error("-- BaseGlobalExceptionHandler.handleHttpMessageNotReadable, ex => {}", ex.getMessage());
        Map<String, Object> data = Map.of("exception", ex.getClass().getSimpleName(), "message", Optional.ofNullable(ex.getMessage()).orElse("Malformed JSON / unreadable body"));
        return new ResponseEntity<>(R.error(SystemResponse.PAM0400, data), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> systemError(HttpServletRequest req, Exception exception) {
        Result<String> error = R.error(SystemResponse.SYS9999, exception.getMessage());
        setBackRequestId(error);
        if (apiClient != null) {
            apiClient.executeOnly(error);
        }
        return new ResponseEntity<>(error, SystemResponse.SYS9999.getHttpStatus());
    }

    @ExceptionHandler(value = BizException.class)
    public ResponseEntity<Object> error(HttpServletRequest req, BizException bizException) {
        // Set back the `x-request-id` for bug-tracing
        Result<Object> error = R.error(bizException.getResponse(), bizException.getData());
        setBackRequestId(error);
        try {
            apiClient.executeOnly(error);
        } catch (Exception e) {
            log.error("-- BaseGlobalExceptionHandler.error, ex => {}", e.getMessage());
        }
        return new ResponseEntity<>(error, bizException.getResponse().getHttpStatus());
    }

    private <T> Result<T> setBackRequestId(Result<T> result) {
        String requestId = UUID.randomUUID().toString();
        try {
            requestId = AppContextHolder.CONTEXT.get().getRequestContext().getRequestId();
            log.info("-- request-id is set to be from [AppContext].requestId [{}]", requestId);
        } catch (Exception exception) {
            log.info("-- Error in request-id set from [AppContext], used back the default one [{}]", requestId);
        }
        result.setRequestId(requestId);
        log.error("-- BaseGlobalExceptionHandler, request-id => [{}], result => [{}]", requestId, result);
        return result;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        try {
            apiClient.executeOnly(new BizException(SystemResponse.SYS9405, ex.getMessage()));
        } catch (Exception e) {
            log.error("-- BaseGlobalExceptionHandler.handleMethodArgumentNotValid, ex => {}", e.getMessage());
        }
        List<String> details = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            details.add(error.getDefaultMessage());
        }
        return new ResponseEntity<>(R.invalid(details), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(R.fail(SystemResponse.SYS9901, ex.getMessage()), SystemResponse.SYS9901.getHttpStatus());
    }

    // __ only handle 401 issue with no token
    public void authenticationDenied(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        Map<String, String> detail = Map.of("path", request.getRequestURI(), "error", ex.getMessage());
        log.info("-- BaseGlobalExceptionHandler.authenticationDenied, ex => {}", detail);
        EndpointHandler.out(response, SystemResponse.SAU0403.getHttpStatus().value(), R.fail(SystemResponse.SAU0403, detail));
    }

    public void accessDenied(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        Map<String, String> detail = Map.of("path", request.getRequestURI(), "error", ex.getMessage());
        log.info("-- BaseGlobalExceptionHandler.accessDenied, ex => {}", detail);
        EndpointHandler.out(response, SystemResponse.SAU0401.getHttpStatus().value(), R.fail(SystemResponse.SAU0401, detail));
    }
}
