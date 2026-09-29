package com.sepe.mvp.controller;

import com.sepe.mvp.model.PortalStatus;
import com.sepe.mvp.model.SearchResult;
import com.sepe.mvp.service.SepeStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SepeStatusController.class)
class SepeStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SepeStatusService service;

    @Test
    void returnsProvincesForValidPostalCode() throws Exception {
        when(service.findProvincesForPostalCode("28001")).thenReturn(List.of(
            new SearchResult("28", "Madrid", false, PortalStatus.OK, "https://example.test")));

        mockMvc.perform(post("/api/find-appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postalCode\":\"28001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].provinceName").value("Madrid"))
            .andExpect(jsonPath("$[0].portalStatus").value("OK"));
    }

    @Test
    void returns400ForInvalidPostalCode() throws Exception {
        when(service.findProvincesForPostalCode("abc"))
            .thenThrow(new IllegalArgumentException("El código postal debe tener 5 dígitos"));

        mockMvc.perform(post("/api/find-appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postalCode\":\"abc\"}"))
            .andExpect(status().isBadRequest());
    }
}
