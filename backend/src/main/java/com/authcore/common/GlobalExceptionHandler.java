package com.authcore.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常翻译器：controller 未消化的异常统一翻译为 Result 结构（架构 §4）。
 *
 * <p>通用错误码口径（Planner 定夺登记于工作单）：参数校验失败 400、系统兜底 500，不侵占资源段。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String INTERNAL_ERROR_MESSAGE = "系统繁忙，请稍后重试";

    /**
     * 业务异常：14xx 认证类错误返回 401，其余返回 400。
     *
     * @param e 业务异常
     * @return HTTP 401/400 + Result.error(code, message)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = (e.getCode() >= 1400 && e.getCode() < 1500) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验失败：code=400，message 取首条字段级提示。
     *
     * @param e 校验失败异常
     * @return HTTP 400 + Result.error(400, 字段级提示)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.error(400, message));
    }

    /**
     * 系统兜底：固定文案避免泄露内部细节，error 日志记录完整堆栈（规范第 5 节）。
     *
     * @param e 未捕获异常
     * @return HTTP 500 + Result.error(500, 固定文案)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未捕获系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, INTERNAL_ERROR_MESSAGE));
    }
}
