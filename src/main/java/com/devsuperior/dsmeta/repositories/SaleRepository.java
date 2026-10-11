package com.devsuperior.dsmeta.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.devsuperior.dsmeta.dto.SaleReportDTO;
import com.devsuperior.dsmeta.dto.SaleSummaryDTO;
import com.devsuperior.dsmeta.entities.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Query("""
        SELECT new com.devsuperior.dsmeta.dto.SaleReportDTO(s.id, s.date, s.amount, seller.name)
        FROM Sale s JOIN s.seller seller
        WHERE s.date BETWEEN :minDate AND :maxDate
        AND LOWER(seller.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """)
    Page<SaleReportDTO> getReport(@Param("minDate") LocalDate minDate,
            @Param("maxDate") LocalDate maxDate, @Param("name") String name, Pageable pageable);

    @Query("""
        SELECT new com.devsuperior.dsmeta.dto.SaleSummaryDTO(seller.name, SUM(s.amount))
        FROM Sale s JOIN s.seller seller
        WHERE s.date BETWEEN :minDate AND :maxDate
        GROUP BY seller.id, seller.name
        ORDER BY seller.name
        """)
    List<SaleSummaryDTO> getSummary(@Param("minDate") LocalDate minDate,
            @Param("maxDate") LocalDate maxDate);
}
