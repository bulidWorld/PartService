package com.partapp.partservice.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.partapp.partservice.web.generated.api.TopicsApiController;
import com.partapp.partservice.web.generated.api.TopicsApiDelegate;
import com.partapp.partservice.web.generated.model.ApiResponseTopics;

@WebMvcTest(TopicsApiController.class)
class TopicParameterValidationTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    TopicsApiDelegate delegate;

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void invalidLimitReturnsUnifiedParameterError(int limit) throws Exception {
        mvc.perform(get("/api/v1/topics").param("limit", Integer.toString(limit))
                .header("Authorization", "Bearer test-token").header("X-Request-Id", "validation-test"))
            .andExpect(status().isBadRequest())
            .andExpect(header().string("X-Request-Id", "validation-test"))
            .andExpect(content().json("""
                {"code":40001,"message":"请求参数不合法","data":null,"requestId":"validation-test"}
                """, true));

        verifyNoInteractions(delegate);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void validLimitReachesDelegate(int limit) throws Exception {
        when(delegate.listTopics(null, null, limit))
            .thenReturn(ResponseEntity.ok(new ApiResponseTopics().code(0).message("ok")));

        mvc.perform(get("/api/v1/topics").param("limit", Integer.toString(limit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(delegate).listTopics(null, null, limit);
    }
}
