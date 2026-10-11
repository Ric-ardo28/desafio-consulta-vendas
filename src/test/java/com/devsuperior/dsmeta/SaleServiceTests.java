package com.devsuperior.dsmeta;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import com.devsuperior.dsmeta.repositories.SaleRepository;
import com.devsuperior.dsmeta.services.SaleService;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTests {
    @Mock private SaleRepository repository;
    @InjectMocks private SaleService service;

    @Test
    void omittedStartIsRelativeToProvidedEndIncludingLeapYear() {
        var page = PageRequest.of(0, 20);
        service.getReport("", "2024-02-29", "", page);
        service.getSummary("", "2024-02-29");
        verify(repository).getReport(LocalDate.of(2023, 2, 28), LocalDate.of(2024, 2, 29), "", page);
        verify(repository).getSummary(LocalDate.of(2023, 2, 28), LocalDate.of(2024, 2, 29));
    }

    @Test
    void omittedEndUsesSystemDateAndOmittedStartUsesLastYear() {
        LocalDate before = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        var page = PageRequest.of(0, 20);
        service.getReport(null, null, null, page);
        service.getSummary("", "");
        LocalDate after = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        verify(repository).getReport(argThat(d -> d.equals(before.minusYears(1)) || d.equals(after.minusYears(1))),
            argThat(d -> d.equals(before) || d.equals(after)), eq(""), eq(page));
        verify(repository).getSummary(argThat(d -> d.equals(before.minusYears(1)) || d.equals(after.minusYears(1))),
            argThat(d -> d.equals(before) || d.equals(after)));
    }

    @Test
    void explicitStartIsPreservedWhenEndIsOmitted() {
        LocalDate before = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        var page = PageRequest.of(0, 20);
        service.getReport("2022-01-01", "", "Logan", page);
        service.getSummary("2022-01-01", "");
        LocalDate after = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        verify(repository).getReport(eq(LocalDate.of(2022, 1, 1)),
            argThat(d -> d.equals(before) || d.equals(after)), eq("Logan"), eq(page));
        verify(repository).getSummary(eq(LocalDate.of(2022, 1, 1)),
            argThat(d -> d.equals(before) || d.equals(after)));
    }
}
