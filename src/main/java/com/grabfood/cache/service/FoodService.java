package com.grabfood.cache.service;

import com.grabfood.cache.model.FoodItem;
import com.grabfood.cache.model.RestaurantMenuDTO;
import com.grabfood.cache.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service quản lý thực đơn và món ăn của GrabFood với Spring Cache.
 * Sử dụng Distributed Cache (Redis) qua @Cacheable, @CacheEvict.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;

    /**
     * Lấy toàn bộ thực đơn của nhà hàng theo restaurantId.
     * Sử dụng @Cacheable với cache name là 'restaurant_menus' và key là restaurantId.
     * 
     * - Lần đầu (Cache Miss): Thực thi method, gọi CSDL (200ms), kết quả được serialize sang JSON lưu vào Redis.
     * - Lần sau (Cache Hit): Bỏ qua method, lấy trực tiếp JSON từ Redis và deserialize về RestaurantMenuDTO (<5ms).
     * - condition: Chỉ cache khi restaurantId hợp lệ (> 0).
     * - unless: Không cache kết quả null.
     */
    @Cacheable(
            value = "restaurant_menus",
            key = "#restaurantId",
            condition = "#restaurantId != null && #restaurantId > 0",
            unless = "#result == null"
    )
    public RestaurantMenuDTO getRestaurantMenu(Long restaurantId) {
        log.info(">>> [SERVICE] Đang gọi CSDL lấy thực đơn cho Nhà hàng ID: {}", restaurantId);
        return foodRepository.findMenuByRestaurantId(restaurantId)
                .orElse(null);
    }

    /**
     * Lấy danh sách các món ăn của nhà hàng theo restaurantId.
     */
    @Cacheable(
            value = "restaurant_food_items",
            key = "#restaurantId",
            condition = "#restaurantId != null && #restaurantId > 0",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<FoodItem> getFoodItemsByRestaurant(Long restaurantId) {
        log.info(">>> [SERVICE] Đang gọi CSDL lấy danh sách món ăn cho Nhà hàng ID: {}", restaurantId);
        return foodRepository.findMenuByRestaurantId(restaurantId)
                .map(RestaurantMenuDTO::getItems)
                .orElse(Collections.emptyList());
    }

    /**
     * Cập nhật thông tin thực đơn của nhà hàng.
     * Xóa cache cũ bằng @CacheEvict (Cache-Aside Pattern) để đảm bảo tính nhất quán dữ liệu phân tán.
     */
    @CacheEvict(
            value = {"restaurant_menus", "restaurant_food_items"},
            key = "#menu.restaurantId"
    )
    public RestaurantMenuDTO updateMenu(RestaurantMenuDTO menu) {
        if (menu == null || menu.getRestaurantId() == null) {
            throw new IllegalArgumentException("Dữ liệu thực đơn và ID nhà hàng không được để trống!");
        }
        log.info(">>> [SERVICE] Cập nhật thực đơn CSDL và vô hiệu hóa cache cho Nhà hàng ID: {}", menu.getRestaurantId());
        return foodRepository.saveOrUpdateMenu(menu);
    }

    /**
     * Xóa thực đơn của nhà hàng và loại bỏ key tương ứng khỏi Redis cache.
     */
    @CacheEvict(
            value = {"restaurant_menus", "restaurant_food_items"},
            key = "#restaurantId"
    )
    public boolean deleteMenu(Long restaurantId) {
        log.info(">>> [SERVICE] Xóa thực đơn CSDL và xóa cache cho Nhà hàng ID: {}", restaurantId);
        return foodRepository.deleteMenu(restaurantId);
    }

    /**
     * Xóa sạch toàn bộ cache thực đơn (Ví dụ: khi bảo trì hệ thống hoặc cập nhật toàn sàn).
     */
    @CacheEvict(value = {"restaurant_menus", "restaurant_food_items"}, allEntries = true)
    public void evictAllMenuCache() {
        log.info(">>> [SERVICE] Đã xóa toàn bộ bộ nhớ đệm thực đơn trên Redis!");
    }
}
