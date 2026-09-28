package com.grabfood.cache.repository;

import com.grabfood.cache.model.FoodItem;
import com.grabfood.cache.model.RestaurantMenuDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository mô phỏng tầng lưu trữ CSDL vật lý (PostgreSQL / MySQL) của GrabFood.
 * Có giả lập độ trễ truy vấn (I/O Latency 200ms) để đối chứng hiệu năng với Redis Cache.
 */
@Slf4j
@Repository
public class FoodRepository {

    private final Map<Long, RestaurantMenuDTO> databaseStore = new ConcurrentHashMap<>();

    public FoodRepository() {
        initSampleData();
    }

    private void initSampleData() {
        // Nhà hàng 1: Phở Gia Truyền Hà Nội
        List<FoodItem> phoItems = new ArrayList<>();
        phoItems.add(FoodItem.builder().id(1L).name("Phở Bò Tái Nạm").category("Món chính").price(new BigDecimal("55000")).description("Phở bò tái nạm truyền thống nước dùng thanh ngọt").available(true).rating(4.9).build());
        phoItems.add(FoodItem.builder().id(2L).name("Phở Gà Đồi").category("Món chính").price(new BigDecimal("50000")).description("Phở gà ta thịt chắc thơm ngon").available(true).rating(4.8).build());
        phoItems.add(FoodItem.builder().id(3L).name("Quẩy Giòn").category("Kèm thêm").price(new BigDecimal("10000")).description("Quẩy vàng giòn rụm").available(true).rating(4.7).build());
        phoItems.add(FoodItem.builder().id(4L).name("Trà Đá").category("Đồ uống").price(new BigDecimal("5000")).description("Trà đá hoa nhài mát lạnh").available(true).rating(4.6).build());

        RestaurantMenuDTO phoMenu = RestaurantMenuDTO.builder()
                .restaurantId(101L)
                .restaurantName("Phở Gia Truyền Hà Nội - 10 Lý Quốc Sư")
                .address("10 Lý Quốc Sư, Hoàn Kiếm, Hà Nội")
                .cuisineType("Món Việt")
                .items(phoItems)
                .lastUpdated(LocalDateTime.now())
                .build();
        databaseStore.put(101L, phoMenu);

        // Nhà hàng 2: Bún Chả Hương Liên
        List<FoodItem> bunChaItems = new ArrayList<>();
        bunChaItems.add(FoodItem.builder().id(5L).name("Bún Chả Đặc Biệt").category("Món chính").price(new BigDecimal("65000")).description("Bún chả Obama nướng than hoa thơm lừng").available(true).rating(4.9).build());
        bunChaItems.add(FoodItem.builder().id(6L).name("Nem Hải Sản").category("Món phụ").price(new BigDecimal("35000")).description("Nem rán hải sản giòn rụm kèm sốt").available(true).rating(4.8).build());

        RestaurantMenuDTO bunChaMenu = RestaurantMenuDTO.builder()
                .restaurantId(102L)
                .restaurantName("Bún Chả Hương Liên (Obama)")
                .address("24 Lê Văn Hưu, Hai Bà Trưng, Hà Nội")
                .cuisineType("Món Việt")
                .items(bunChaItems)
                .lastUpdated(LocalDateTime.now())
                .build();
        databaseStore.put(102L, bunChaMenu);
    }

    /**
     * Giả lập truy vấn CSDL vật lý với độ trễ 200ms.
     */
    public Optional<RestaurantMenuDTO> findMenuByRestaurantId(Long restaurantId) {
        log.warn("===> [DATABASE QUERY] Truy vấn CSDL vật lý cho Restaurant ID: {} (I/O Delay ~200ms)...", restaurantId);
        try {
            Thread.sleep(200); // Mô phỏng độ trễ I/O mạng và disk
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.ofNullable(databaseStore.get(restaurantId));
    }

    public RestaurantMenuDTO saveOrUpdateMenu(RestaurantMenuDTO menu) {
        log.info("===> [DATABASE WRITE] Lưu/cập nhật dữ liệu vào CSDL cho Restaurant ID: {}", menu.getRestaurantId());
        menu.setLastUpdated(LocalDateTime.now());
        databaseStore.put(menu.getRestaurantId(), menu);
        return menu;
    }

    public boolean deleteMenu(Long restaurantId) {
        log.info("===> [DATABASE DELETE] Xóa thực đơn khỏi CSDL cho Restaurant ID: {}", restaurantId);
        return databaseStore.remove(restaurantId) != null;
    }
}
