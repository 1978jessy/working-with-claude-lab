package com.marlowefinch.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** GET /api/summary through MockMvc: default range and an empty range. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SummaryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void summaryDefaultsToTheLast30DaysAndNamesTheWorstCarrierAndBusiestCategory() throws Exception {
        mvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-22"))
                .andExpect(jsonPath("$.to").value("2026-09-21"))
                .andExpect(jsonPath("$.onTimeRate").value(0.937))
                .andExpect(jsonPath("$.openTickets").value(114))
                .andExpect(jsonPath("$.revenue").value(360095.5))
                .andExpect(jsonPath("$.orders").value(624))
                .andExpect(jsonPath("$.worstCarrier").value("Kessler Logistics"))
                .andExpect(jsonPath("$.busiestTicketCategory").value("Delivery delay"));
    }

    @Test
    void summaryOfAnEmptyRangeHasNullNames() throws Exception {
        mvc.perform(get("/api/summary").param("from", "2020-01-01").param("to", "2020-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2020-01-01"))
                .andExpect(jsonPath("$.to").value("2020-01-31"))
                .andExpect(jsonPath("$.orders").value(0))
                .andExpect(jsonPath("$.worstCarrier").isEmpty())
                .andExpect(jsonPath("$.busiestTicketCategory").isEmpty());
    }
}
