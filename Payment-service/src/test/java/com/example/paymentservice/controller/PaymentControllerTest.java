package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPaymentTest() throws Exception {
        var request = new Payment.CreateRequest(123L, new BigDecimal("99.99"));
        var paymentId = 3L;
        var expectedPayment = new Payment();
        expectedPayment.setId(paymentId);

        given(paymentService.createPayment(any(Payment.CreateRequest.class))).willReturn(expectedPayment);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/payments/3"))
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.message").value("Payment accepted for processing"));
    }

    @Test
    void getPaymentStatusTest() throws Exception {
        when(paymentService.getStatus(any())).thenReturn(new PaymentController.PaymentStatusResponse(1L, "PENDING"));
        mockMvc.perform(get("/api/v1/payments/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));

    }
}