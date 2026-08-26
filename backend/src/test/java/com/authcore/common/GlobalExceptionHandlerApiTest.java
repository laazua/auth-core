package com.authcore.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理三条路径的 Web 层判定用例（infra/003 AC2/AC3/AC4）。
 *
 * <p>探针 controller 仅存在于测试夹具内，禁止为测试向主代码添加业务端点。
 */
class GlobalExceptionHandlerApiTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .build();

    @RestController
    static class ProbeController {

        /**
         * 探针端点：按 name 分支触发三类异常路径。
         *
         * @param request 校验入参
         * @return 正常路径不会到达
         */
        @PostMapping("/probe/users")
        Result<Void> create(@Valid @RequestBody CreateUserRequest request) {
            if ("existing".equals(request.name())) {
                throw new BusinessException(1001, "用户已存在");
            }
            throw new RuntimeException("内部细节 jdbc:mysql://secret 不应外泄");
        }
    }

    record CreateUserRequest(@NotBlank(message = "名称不能为空") String name) {
    }

    /**
     * Given 业务异常 When 翻译 Then HTTP 400 且业务码与提示原样保留。
     *
     * @throws Exception MockMvc 调用异常
     */
    @Test
    @DisplayName("业务异常翻译为 400 与原样业务码")
    void test_业务异常_翻译为400与原样业务码() throws Exception {
        mockMvc.perform(post("/probe/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"existing\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("用户已存在"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * Given @NotBlank 校验失败 When 翻译 Then HTTP 400 且 code=400、message 含字段级提示。
     *
     * @throws Exception MockMvc 调用异常
     */
    @Test
    @DisplayName("参数校验失败返回 400 与 code=400 及字段级提示")
    void test_参数校验失败_返回400与code400() throws Exception {
        mockMvc.perform(post("/probe/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("名称不能为空")))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * Given 未捕获 RuntimeException When 兜底 Then HTTP 500、code=500、固定文案不泄露细节，
     * 且以 error 级日志记录并携带堆栈（规范第 5 节）。
     *
     * @throws Exception MockMvc 调用异常
     */
    @Test
    @DisplayName("未捕获异常返回 500 固定文案不泄露细节且 error 日志带堆栈")
    void test_未捕获异常_返回500固定文案不泄露细节() throws Exception {
        Logger handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        handlerLogger.addAppender(appender);
        try {
            MvcResult result = mockMvc.perform(post("/probe/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"trigger\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("jdbc").contains("系统繁忙");

            assertThat(appender.list)
                    .anySatisfy(event -> {
                        assertEquals(Level.ERROR, event.getLevel());
                        assertThat(event.getThrowableProxy()).as("error 日志必须带堆栈").isNotNull();
                    });
        } finally {
            handlerLogger.detachAppender(appender);
        }
    }
}
