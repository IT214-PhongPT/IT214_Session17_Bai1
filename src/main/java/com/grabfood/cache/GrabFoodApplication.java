package com.grabfood.cache;

import com.grabfood.cache.model.FoodItem;
import com.grabfood.cache.model.Promotion;
import com.grabfood.cache.model.RestaurantMenuDTO;
import com.grabfood.cache.service.FoodService;
import com.grabfood.cache.service.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * Lớp khởi chạy ứng dụng GrabFood Distributed Cache Service.
 * Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
 */
@Slf4j
@SpringBootApplication
public class GrabFoodApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrabFoodApplication.class, args);
    }

    /**
     * Demo tự động kiểm thử cơ chế Distributed Cache trên Redis khi ứng dụng khởi động.
     */
    @Bean
    public CommandLineRunner demo(FoodService foodService,
                                  PromotionService promotionService,
                                  RedisTemplate<String, Object> redisTemplate) {
        return args -> {
            log.info("=========================================================================");
            log.info(" BẮT ĐẦU CHẠY THỰC NGHIỆM DISTRIBUTED CACHE - GRABFOOD ECOSYSTEM");
            log.info(" Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120");
            log.info("=========================================================================");

            Long restaurantId = 101L;

            // BƯỚC 1: Lần gọi thứ nhất -> Cache Miss -> Đọc CSDL
            log.info("\n--- [BƯỚC 1] Lần gọi đầu tiên (Restaurant ID = {}): Mong đợi CACHE MISS ---", restaurantId);
            long start1 = System.currentTimeMillis();
            RestaurantMenuDTO menu1 = foodService.getRestaurantMenu(restaurantId);
            long time1 = System.currentTimeMillis() - start1;
            log.info("Kết quả lần 1: [Thời gian: {} ms] - Nhà hàng: {}", time1, menu1 != null ? menu1.getRestaurantName() : "NULL");

            // BƯỚC 2: Lần gọi thứ hai -> Cache Hit -> Đọc Redis
            log.info("\n--- [BƯỚC 2] Lần gọi thứ hai (Restaurant ID = {}): Mong đợi CACHE HIT (<5ms) ---", restaurantId);
            long start2 = System.currentTimeMillis();
            RestaurantMenuDTO menu2 = foodService.getRestaurantMenu(restaurantId);
            long time2 = System.currentTimeMillis() - start2;
            log.info("Kết quả lần 2: [Thời gian: {} ms] - Nhà hàng: {}", time2, menu2 != null ? menu2.getRestaurantName() : "NULL");

            // BƯỚC 3: Lấy danh sách món ăn
            log.info("\n--- [BƯỚC 3] Lấy danh sách món ăn của Nhà hàng 101 ---");
            List<FoodItem> items = foodService.getFoodItemsByRestaurant(restaurantId);
            log.info("Số lượng món ăn tìm thấy: {}", items.size());

            // BƯỚC 4: Kiểm tra Khuyến mãi
            log.info("\n--- [BƯỚC 4] Lấy thông tin mã khuyến mãi 'GRABFOOD50' ---");
            Promotion promo = promotionService.getPromotion("GRABFOOD50");
            log.info("Khuyến mãi: {} - Giảm: {}%", promo.getPromoCode(), promo.getDiscountPercentage());

            log.info("\n=========================================================================");
            log.info(" THỰC NGHIỆM HOÀN TẤT THÀNH CÔNG RỰC RỠ!");
            log.info("=========================================================================\n");
        };
    }
}
