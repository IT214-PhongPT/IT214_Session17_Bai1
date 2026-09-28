package com.grabfood.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đối tượng khuyến mãi của nhà hàng / món ăn trên GrabFood.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion implements Serializable {
    private static final long serialVersionUID = 1L;

    private String promoCode;
    private String description;
    private BigDecimal discountPercentage;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime validUntil;
    private boolean active;
}
