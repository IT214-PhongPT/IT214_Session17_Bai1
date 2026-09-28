package com.grabfood.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grabfood.cache.config.CacheConfig;
import com.grabfood.cache.model.FoodItem;
import com.grabfood.cache.model.RestaurantMenuDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tính năng Serialization sang JSON với GenericJackson2JsonRedisSerializer.
 * Đảm bảo dữ liệu lưu trong Redis ở dạng JSON đọc được (human-readable) và có type metadata.
 */
class JsonSerializationTest {

    @Test
    @DisplayName("Kiểm tra serialize RestaurantMenuDTO sang chuỗi JSON và deserialize ngược lại")
    void testJsonSerializationAndDeserialization() {
        CacheConfig config = new CacheConfig();
        ObjectMapper mapper = config.redisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        FoodItem item1 = FoodItem.builder()
                .id(1L)
                .name("Phở Bò Tái Nạm")
                .category("Món chính")
                .price(new BigDecimal("55000"))
                .description("Phở bò tái nạm gia truyền")
                .available(true)
                .rating(4.9)
                .build();

        RestaurantMenuDTO menu = RestaurantMenuDTO.builder()
                .restaurantId(101L)
                .restaurantName("Phở Gia Truyền 10 Lý Quốc Sư")
                .address("10 Lý Quốc Sư, Hà Nội")
                .cuisineType("Món Việt")
                .items(List.of(item1))
                .lastUpdated(LocalDateTime.now())
                .build();

        // 1. Serialize đối tượng thành byte[] (JSON)
        byte[] bytes = serializer.serialize(menu);
        assertNotNull(bytes);

        String jsonString = new String(bytes);
        System.out.println("JSON Serialized Output in Redis:\n" + jsonString);

        // 2. Xác thực chuỗi JSON đọc được, chứa các trường thuộc tính rõ ràng
        assertTrue(jsonString.contains("\"restaurantId\":101"));
        assertTrue(jsonString.contains("\"restaurantName\":\"Phở Gia Truyền 10 Lý Quốc Sư\""));
        assertTrue(jsonString.contains("\"name\":\"Phở Bò Tái Nạm\""));
        assertTrue(jsonString.contains("\"price\":55000"));

        // 3. Deserialize byte[] ngược lại thành đối tượng Java
        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof RestaurantMenuDTO);

        RestaurantMenuDTO deserializedMenu = (RestaurantMenuDTO) deserialized;
        assertEquals(101L, deserializedMenu.getRestaurantId());
        assertEquals("Phở Gia Truyền 10 Lý Quốc Sư", deserializedMenu.getRestaurantName());
        assertEquals(1, deserializedMenu.getItems().size());
        assertEquals("Phở Bò Tái Nạm", deserializedMenu.getItems().get(0).getName());
    }
}
