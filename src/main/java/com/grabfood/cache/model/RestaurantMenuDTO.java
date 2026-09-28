package com.grabfood.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa thông tin thực đơn của nhà hàng trên GrabFood.
 * Được lưu trữ dưới dạng JSON trên Distributed Cache (Redis).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantMenuDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long restaurantId;
    private String restaurantName;
    private String address;
    private String cuisineType;
    private List<FoodItem> items;
    private LocalDateTime lastUpdated;
}
