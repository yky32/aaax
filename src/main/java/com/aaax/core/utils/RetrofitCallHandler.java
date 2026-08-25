package com.aaax.core.utils;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class RetrofitCallHandler {


    public static <T> T executeLite(Call<Result<T>> api) {
        try {
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler.execute()
                    =====================================================
                    Calling ==> {}
                    url ==> {}
                    api ==> {}
                    =====================================================
                    """,
                    api,
                    api.request().url(),
                    api.request().url().encodedPath());
            Response<Result<T>> execute = api.execute();
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler.execute() @@@ response
                    =====================================================
                    api.execute() ==> {}
                    =====================================================
                    """,
                    execute);

            if (ObjectUtils.isNotEmpty(execute.body())){
                Result<T> result = Objects.requireNonNull(execute.body());
                return result.getData();
            }

            if (ObjectUtils.isNotEmpty(execute.errorBody())) {
                // unhappy-case, special handle go to check errorBody
                Map map = JSONUtil.readValue(execute.errorBody().string(), Map.class);
                throw new BizException(SystemResponse.SYM9400, map);
            }

            throw new BizException(SystemResponse.SYM9400, "Error in execute() => ".concat(RetrofitCallHandler.class.getSimpleName()));

        } catch (Exception exception) {
            printLog(exception, api.request().url().encodedPath());
            if (exception instanceof BizException) {
                throw new BizException(((BizException) exception).getResponse(), ((BizException) exception).getData());
            }

            // other exception
            throw new BizException(SystemResponse.SYS9400, exception.getMessage());
        }
    }

    public static <T> T execute(Call<Result<T>> api) {
        try {
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler.execute()
                    =====================================================
                    Calling ==> {}
                    Request ==> {}
                    url ==> {}
                    api ==> {}
                    =====================================================
                    """,
                    api,
                    api.request(),
                    api.request().url(),
                    api.request().url().encodedPath());
            Response<Result<T>> execute = api.execute();
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler.execute() @@@ response
                    =====================================================
                    api.execute() ==> {}
                    =====================================================
                    """,
                    execute);

            if (ObjectUtils.isNotEmpty(execute.body())){
                Result<T> result = Objects.requireNonNull(execute.body());
                return result.getData();
            }

            if (ObjectUtils.isNotEmpty(execute.errorBody())) {
                // unhappy-case, special handle go to check errorBody
                Map map = JSONUtil.readValue(execute.errorBody().string(), Map.class);
                throw new BizException(SystemResponse.SYM9400, map);
            }

            throw new BizException(SystemResponse.SYM9400, "Error in execute() => ".concat(RetrofitCallHandler.class.getSimpleName()));

        } catch (Exception exception) {
            printLog(exception, api.request().url().encodedPath());
            if (exception instanceof BizException) {
                throw new BizException(((BizException) exception).getResponse(), ((BizException) exception).getData());
            }

            // other exception
            throw new BizException(SystemResponse.SYS9400, exception.getMessage());
        }
    }

    public static void _void_execute(Call<? extends Void> api) {
        try {
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler._void_execute()
                    =====================================================
                    Calling ==> {}
                    Request ==> {}
                    url ==> {}
                    api ==> {}
                    =====================================================
                    """,
                    api,
                    api.request(),
                    api.request().url(),
                    api.request().url().encodedPath());

            Response<? extends Void> execute = api.execute();

            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler._void_execute() @@@ response
                    =====================================================
                    api.void ==> {}
                    =====================================================
                    """,
                    api);

        } catch (Exception exception) {
            printLog(exception, api.request().url().encodedPath());
            if (exception instanceof BizException) {
                throw new BizException(((BizException) exception).getResponse(), ((BizException) exception).getData());
            }
            // other exception
            throw new BizException(SystemResponse.SYS9400, exception.getMessage());
        }
    }

    public static <T> T _execute(Call<T> api) {
        try {
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler._execute()
                    =====================================================
                    Calling ==> {}
                    Request ==> {}
                    url ==> {}
                    api ==> {}
                    =====================================================
                    """,
                    api,
                    api.request(),
                    api.request().url(),
                    api.request().url().encodedPath());
            log.info("-- RetrofitCallHandler _execute: {}", api);
            Response<T> execute = api.execute();

            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler._execute() @@@ response
                    =====================================================
                    api.execute() ==> {}
                    =====================================================
                    """,
                    execute);

            if (ObjectUtils.isNotEmpty(execute.body())){
                return execute.body();
            }
            if (ObjectUtils.isNotEmpty(execute.errorBody())) {
                // unhappy-case, special handle go to check errorBody
                Map map = JSONUtil.readValue(execute.errorBody().string(), Map.class);
                throw new BizException(SystemResponse.SYM9400, map);
            }
            throw new BizException(SystemResponse.SYM9400, "Error in _execute() => ".concat(RetrofitCallHandler.class.getSimpleName()));

        } catch (Exception exception) {
            printLog(exception, api.request().url().encodedPath());
            if (exception instanceof BizException) {
                throw new BizException(((BizException) exception).getResponse(), ((BizException) exception).getData());
            }
            // other exception
            throw new BizException(SystemResponse.SYS9400, exception.getMessage());
        }
    }

    public static <T> Result<T> _execute_with_result(Call<T> api) {
        try {
            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler._execute_with_result()
                    =====================================================
                    Calling ==> {}
                    Request ==> {}
                    url ==> {}
                    api ==> {}
                    =====================================================
                    """,
                    api,
                    api.request(),
                    api.request().url(),
                    api.request().url().encodedPath());


            Response<T> execute = api.execute();

            log.info("""
                    
                    /////////////////////////////////////////////////////
                    =====================================================
                    RetrofitCallHandler._execute_with_result() @@@ response
                    =====================================================
                    api.execute() ==> {}
                    =====================================================
                    """,
                    execute);

            if (ObjectUtils.isNotEmpty(execute.body())){
                return R.success(execute.body());
            }

            if (ObjectUtils.isNotEmpty(execute.errorBody())) {
                Map map = JSONUtil.readValue(execute.errorBody().string(), Map.class);
                return R.fail((T) map);
            }

            throw new BizException(SystemResponse.SYM9400, "Error in _execute_with_result() => ".concat(RetrofitCallHandler.class.getSimpleName()));

        } catch (Exception exception) {
            printLog(exception, api.request().url().encodedPath());
            if (exception instanceof BizException) {
                throw new BizException(((BizException) exception).getResponse(), ((BizException) exception).getData());
            }
            // other exception
            throw new BizException(SystemResponse.SYS9400, exception);
        }
    }



    // === common
    private static void printLog(Exception exception, String url) {
        // ERROR so SigNoz / log-based alerts can fire (was INFO — silent in prod triage).
        log.error("""
                    
                    @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                    =====================================================
                    Error in RetrofitCallHandler  @@@ catch-block
                    =====================================================
                    exception.message ==> {}
                    exception.type ==> {}
                    url ==> {}
                    =====================================================
                    """,
                exception.getMessage(),
                exception.getClass().getName(),
                url);
    }
}
