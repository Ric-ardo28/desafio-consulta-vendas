package com.devsuperior.dsmeta.dto;

public class SaleSummaryDTO {
    private final String sellerName;
    private final Double total;

    public SaleSummaryDTO(String sellerName, Double total) {
        this.sellerName = sellerName;
        this.total = total;
    }

    public String getSellerName() { return sellerName; }

    public Double getTotal() { return total; }
}
