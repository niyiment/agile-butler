package com.niyiment.agilebutler.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Controller
@ControllerAdvice
@Slf4j
public class WebSocketErrorHandler {

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, String> handleException(Exception ex) {
        log.error("WebSocket error occurred: {}", ex.getMessage(), ex);

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("type", "ERROR");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return errorResponse;
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("WebSocket validation error: {}", ex.getMessage());

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("type", "VALIDATION_ERROR");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return errorResponse;
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, String> handleSecurityException(SecurityException ex) {
        log.warn("WebSocket security error: {}", ex.getMessage());

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("type", "SECURITY_ERROR");
        errorResponse.put("message", "Access denied");
        errorResponse.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return errorResponse;
    }
}