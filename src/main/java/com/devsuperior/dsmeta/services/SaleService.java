package com.devsuperior.dsmeta.services;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.devsuperior.dsmeta.dto.SaleReportDTO;
import com.devsuperior.dsmeta.dto.SaleSummaryDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devsuperior.dsmeta.dto.SaleMinDTO;
import com.devsuperior.dsmeta.entities.Sale;
import com.devsuperior.dsmeta.repositories.SaleRepository;

@Service
public class SaleService {

	@Autowired
	private SaleRepository repository;
	
	public SaleMinDTO findById(Long id) {
		Optional<Sale> result = repository.findById(id);
		Sale entity = result.get();
		return new SaleMinDTO(entity);
	}

    @Transactional(readOnly = true)
    public Page<SaleReportDTO> getReport(String minDate, String maxDate, String name, Pageable pageable) {
        LocalDate end = resolveMaxDate(maxDate);
        LocalDate start = resolveMinDate(minDate, end);
        return repository.getReport(start, end, name == null ? "" : name, pageable);
    }

    @Transactional(readOnly = true)
    public List<SaleSummaryDTO> getSummary(String minDate, String maxDate) {
        LocalDate end = resolveMaxDate(maxDate);
        LocalDate start = resolveMinDate(minDate, end);
        return repository.getSummary(start, end);
    }

    private LocalDate resolveMaxDate(String maxDate) {
        if (maxDate == null || maxDate.isBlank()) {
            return LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        }
        return LocalDate.parse(maxDate);
    }

    private LocalDate resolveMinDate(String minDate, LocalDate maxDate) {
        return minDate == null || minDate.isBlank() ? maxDate.minusYears(1L) : LocalDate.parse(minDate);
    }
}
