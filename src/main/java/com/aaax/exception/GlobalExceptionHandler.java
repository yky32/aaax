package com.aaax.exception;

import com.aaax.core.exception.BaseGlobalExceptionHandler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

/**
 * qs/uaa style: in-service GlobalExceptionHandler extends core BaseGlobalExceptionHandler.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestController
@ControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {}
