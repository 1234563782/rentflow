package com.rentflow.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.web.ApiErrorResponse;
import com.rentflow.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                code,
                message,
                correlationId == null ? Ulid.next() : correlationId,
                Map.of()
        ));
    }
}
