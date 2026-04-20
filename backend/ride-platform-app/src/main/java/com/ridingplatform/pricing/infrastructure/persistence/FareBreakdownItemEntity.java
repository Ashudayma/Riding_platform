package com.ridingplatform.pricing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fare_breakdown_item", schema = "pricing")
public class FareBreakdownItemEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fare_quote_id", nullable = false)
    private FareQuoteEntity fareQuote;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 32)
    private FareLineType lineType;

    @Column(name = "line_code", nullable = false, length = 64)
    private String lineCode;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
