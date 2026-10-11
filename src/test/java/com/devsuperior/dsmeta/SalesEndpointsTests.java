package com.devsuperior.dsmeta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SalesEndpointsTests {
    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
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

    @Test
    @Transactional
    void reportWithoutArgumentsReturnsOnlyLastTwelveMonths() throws Exception {
        LocalDate today = prepareRollingYearSales();
        mvc.perform(get("/sales/report"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0].date").value(today.minusYears(1).toString()))
            .andExpect(jsonPath("$.content[0].amount").value(100.0))
            .andExpect(jsonPath("$.content[1].date").value(today.minusMonths(6).toString()))
            .andExpect(jsonPath("$.content[1].amount").value(200.0))
            .andExpect(jsonPath("$.content[2].date").value(today.toString()))
            .andExpect(jsonPath("$.content[2].amount").value(300.0));
    }

    @Test
    @Transactional
    void summaryWithoutArgumentsSumsOnlyLastTwelveMonthsBySeller() throws Exception {
        prepareRollingYearSales();
        mvc.perform(get("/sales/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sellerName").value("Logan"))
            .andExpect(jsonPath("$[0].total").value(300.0))
            .andExpect(jsonPath("$[1].sellerName").value("Loki Odinson"))
            .andExpect(jsonPath("$[1].total").value(300.0));
    }

    // Each test rolls back this isolated fixture, preserving the PDF seed.
    private LocalDate prepareRollingYearSales() {
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        jdbc.update("DELETE FROM tb_sales");
        insertSale(1, 100.0, today.minusYears(1));
        insertSale(1, 200.0, today.minusMonths(6));
        insertSale(2, 300.0, today);
        insertSale(1, 9000.0, today.minusYears(1).minusDays(1));
        insertSale(2, 8000.0, today.plusDays(1));
        insertSale(3, 7000.0, today.minusYears(2));
        return today;
    }

    private void insertSale(long sellerId, double amount, LocalDate date) {
        jdbc.update("INSERT INTO tb_sales(seller_id, visited, deals, amount, date) VALUES (?, 1, 1, ?, ?)",
            sellerId, amount, date);
    }
}
