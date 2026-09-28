package com.grabfood.cache;

import com.grabfood.cache.model.FoodItem;
import com.grabfood.cache.model.RestaurantMenuDTO;
import com.grabfood.cache.repository.FoodRepository;
import com.grabfood.cache.service.FoodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho FoodService kiểm tra logic nghiệp vụ và tương tác Repository.
 */
@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private FoodService foodService;

    private RestaurantMenuDTO sampleMenu;

    @BeforeEach
    void setUp() {
        FoodItem item = FoodItem.builder()
                .id(1L)
                .name("Phở Bò Tái Nạm")
                .category("Món chính")
                .price(new BigDecimal("55000"))
                .available(true)
                .build();

        sampleMenu = RestaurantMenuDTO.builder()
                .restaurantId(101L)
                .restaurantName("Phở Lý Quốc Sư")
                .items(List.of(item))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Lấy thực đơn nhà hàng hợp lệ trả về đúng DTO")
    void testGetRestaurantMenu_Success() {
        when(foodRepository.findMenuByRestaurantId(101L)).thenReturn(Optional.of(sampleMenu));

        RestaurantMenuDTO result = foodService.getRestaurantMenu(101L);

        assertNotNull(result);
        assertEquals(101L, result.getRestaurantId());
        assertEquals("Phở Lý Quốc Sư", result.getRestaurantName());
        assertEquals(1, result.getItems().size());
        verify(foodRepository, times(1)).findMenuByRestaurantId(101L);
    }

    @Test
    @DisplayName("Lấy danh sách món ăn từ nhà hàng")
    void testGetFoodItemsByRestaurant() {
        when(foodRepository.findMenuByRestaurantId(101L)).thenReturn(Optional.of(sampleMenu));

        List<FoodItem> items = foodService.getFoodItemsByRestaurant(101L);

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("Phở Bò Tái Nạm", items.get(0).getName());
    }

    @Test
    @DisplayName("Cập nhật thực đơn thành công")
    void testUpdateMenu_Success() {
        when(foodRepository.saveOrUpdateMenu(any(RestaurantMenuDTO.class))).thenReturn(sampleMenu);

        RestaurantMenuDTO updated = foodService.updateMenu(sampleMenu);

        assertNotNull(updated);
        assertEquals(101L, updated.getRestaurantId());
        verify(foodRepository, times(1)).saveOrUpdateMenu(sampleMenu);
    }

    @Test
    @DisplayName("Cập nhật thực đơn null ném ngoại lệ IllegalArgumentException (Fail-Fast)")
    void testUpdateMenu_NullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> foodService.updateMenu(null));
    }
}
