package com.grabfood.cache.controller;

import com.grabfood.cache.model.FoodItem;
import com.grabfood.cache.model.Promotion;
import com.grabfood.cache.model.RestaurantMenuDTO;
import com.grabfood.cache.service.FoodService;
import com.grabfood.cache.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller cho hệ thống GrabFood Cache Demo.
 */
@RestController
@RequestMapping("/api/v1/grabfood")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final PromotionService promotionService;

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<Map<String, Object>> getRestaurantMenu(@PathVariable Long restaurantId) {
        long startTime = System.currentTimeMillis();
        RestaurantMenuDTO menu = foodService.getRestaurantMenu(restaurantId);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("restaurantId", restaurantId);
        response.put("executionTimeMs", duration);
        response.put("source", duration < 50 ? "REDIS_CACHE (Distributed)" : "DATABASE_FETCH (Cache Miss)");
        response.put("data", menu);

        if (menu == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurants/{restaurantId}/foods")
    public ResponseEntity<Map<String, Object>> getFoodItems(@PathVariable Long restaurantId) {
        long startTime = System.currentTimeMillis();
        List<FoodItem> items = foodService.getFoodItemsByRestaurant(restaurantId);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("restaurantId", restaurantId);
        response.put("executionTimeMs", duration);
        response.put("itemsCount", items.size());
        response.put("data", items);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<RestaurantMenuDTO> updateMenu(
            @PathVariable Long restaurantId,
            @RequestBody RestaurantMenuDTO menu) {
        menu.setRestaurantId(restaurantId);
        RestaurantMenuDTO updated = foodService.updateMenu(menu);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/promotions/{promoCode}")
    public ResponseEntity<Map<String, Object>> getPromotion(@PathVariable String promoCode) {
        long startTime = System.currentTimeMillis();
        Promotion promo = promotionService.getPromotion(promoCode);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("promoCode", promoCode);
        response.put("executionTimeMs", duration);
        response.put("source", duration < 50 ? "REDIS_CACHE (Distributed)" : "DATABASE_FETCH (Cache Miss)");
        response.put("data", promo);

        if (promo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cache/all")
    public ResponseEntity<Map<String, String>> evictAllCache() {
        foodService.evictAllMenuCache();
        Map<String, String> res = new HashMap<>();
        res.put("message", "Đã xóa toàn bộ cache thực đơn trên Redis thành công!");
        return ResponseEntity.ok(res);
    }
}
