package com.aaax.core.utils;

import com.aaax.core.common.AppContextHolder;
import com.aaax.core.common.jsonfield.LogContextMetadata;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingUtil {

    public static LogContextMetadata set_correlationId_req_res(String traceId, String correlationId, Object req, Object res) {
        LogContextMetadata logContext = set_correlationId_req_res(correlationId, req, res);
        if (traceId != null) {
            logContext.setTraceId(traceId);
        }
        log.info("-- setupLoggingId => {}", logContext);
        return logContext;
    }

    public static LogContextMetadata set_correlationId_req_res(String correlationId, Object req, Object res) {
        LogContextMetadata logContext = AppContextHolder.CONTEXT.get().getLogContext();
        if (correlationId != null) {
            logContext.setCorrelationId(IdSplitter.split(correlationId));
        }
        if (req != null) {
            logContext.setRequestBody(req);
        }
        if (res != null) {
            logContext.setResponseBody(res);
        }
        log.info("-- setupLoggingId => {}", logContext);
        return logContext;
    }
}
