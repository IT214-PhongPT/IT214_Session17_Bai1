package com.grabfood.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Đối tượng biểu diễn món ăn trong hệ thống GrabFood.
 * Hỗ trợ Serializable và Jackson serialization/deserialization dạng JSON.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private String description;
    private boolean available;
    private Double rating;
}
