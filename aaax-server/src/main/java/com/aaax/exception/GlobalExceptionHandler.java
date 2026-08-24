package com.aaax.exception;

import com.aaax.core.config.api_handler.InServiceElkHandler;
import com.aaax.core.exception.BaseGlobalExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;


/**
 * This is [in-service -> GlobalExceptionHandler] exception handler
 * You can override the [BaseGlobalExceptionHandler] method
 * in order to customize your specific return response here
 *
 * @Author wayne.yu
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestController
@ControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    public GlobalExceptionHandler(InServiceElkHandler inServiceElkHandler) {
        super(inServiceElkHandler);
    }
}