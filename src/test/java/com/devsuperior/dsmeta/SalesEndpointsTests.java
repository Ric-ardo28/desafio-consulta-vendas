package com.devsuperior.dsmeta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SalesEndpointsTests {
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test
    void summaryMatchesPdf() throws Exception {
        mvc.perform(get("/sales/summary").param("minDate", "2022-01-01").param("maxDate", "2022-06-30"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [{"sellerName":"Anakin","total":110571.0},
                 {"sellerName":"Logan","total":83587.0},
                 {"sellerName":"Loki Odinson","total":150597.0},
                 {"sellerName":"Padme","total":135902.0},
                 {"sellerName":"Thor Odinson","total":144896.0}]
                """));
    }

    @Test
    void reportMatchesPdf() throws Exception {
        mvc.perform(get("/sales/report").param("minDate", "2022-05-01")
                .param("maxDate", "2022-05-31").param("name", "odinson"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(content().json("""
                {"content":[
                  {"id":9,"date":"2022-05-22","amount":19476.0,"sellerName":"Loki Odinson"},
                  {"id":10,"date":"2022-05-18","amount":20530.0,"sellerName":"Thor Odinson"},
                  {"id":12,"date":"2022-05-06","amount":21753.0,"sellerName":"Loki Odinson"}]}
                """));
    }

    @Test
    void reportSupportsPaginationSortingAndCaseInsensitiveName() throws Exception {
        mvc.perform(get("/sales/report").param("minDate", "2022-05-01")
                .param("maxDate", "2022-05-31").param("name", "ODINSON")
                .param("size", "1").param("page", "1").param("sort", "id,desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(3)).andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void datesAreInclusiveAndNameIsOptional() throws Exception {
        mvc.perform(get("/sales/report").param("minDate", "2022-05-22").param("maxDate", "2022-05-22"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(9));
        mvc.perform(get("/sales/summary").param("minDate", "2022-05-22").param("maxDate", "2022-05-22"))
            .andExpect(status().isOk()).andExpect(content().json("""
                [{"sellerName":"Loki Odinson","total":19476.0}]
                """));
    }

    @Test
    void bothEndpointsAcceptMissingAndEmptyParameters() throws Exception {
        mvc.perform(get("/sales/report")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/sales/summary")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/sales/report").param("minDate", "").param("maxDate", "").param("name", ""))
            .andExpect(status().isOk());
        mvc.perform(get("/sales/summary").param("minDate", "").param("maxDate", ""))
            .andExpect(status().isOk());
    }

    @Test
    void absentStartUsesOneYearBeforeProvidedEnd() throws Exception {
        mvc.perform(get("/sales/report").param("maxDate", "2022-05-22").param("name", "not-a-seller"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/sales/summary").param("maxDate", "2020-01-01"))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
