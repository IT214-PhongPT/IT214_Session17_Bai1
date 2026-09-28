# IT214 - Session 17 - Bài Tập 1: Cấu hình Hạ tầng Distributed Cache chuyên nghiệp

- **Học phần:** Kiến trúc Microservices (IT214)
- **Sinh viên:** Phạm Thanh Phong
- **MSSV:** N24DTCN120
- **Mã bài tập:** `IT214_Session17_BT1`

---

## 1. Giới thiệu dự án
Dự án triển khai giải pháp **Distributed Cache (Redis)** cho hệ sinh thái **GrabFood**, thay thế bộ nhớ đệm cục bộ (Local Heap Cache) để giải quyết triệt để sự cố dữ liệu không nhất quán giữa các instance khi mở rộng hệ thống (Horizontal Scaling).

### Các điểm cải tiến trọng tâm:
1. **Lettuce Driver (Non-blocking I/O):** Thay thế Jedis truyền thống bằng Lettuce dựa trên Netty, hỗ trợ chia sẻ kết nối an toàn (thread-safe connection sharing) và kết hợp `commons-pool2`.
2. **JSON Serialization:** Chuyển đổi từ Java Serialization sang `GenericJackson2JsonRedisSerializer` với `ObjectMapper` tùy biến, giúp dữ liệu lưu trong Redis ở định dạng JSON con người có thể đọc hiểu trực tiếp qua `redis-cli`.
3. **Cấu hình động qua YAML:** Toàn bộ thông số kết nối (`host`, `port`, `timeout`, `pool.max-active`) được nạp linh hoạt từ `application.yml`, không hard-code.
4. **Khả năng chịu lỗi cao (Fault Tolerance):** Tích hợp `CustomCacheErrorHandler` để tự động Fallback xuống Database khi Redis gặp sự cố, ngăn ngừa lỗi 500 gián đoạn dịch vụ.

---

## 2. Hướng dẫn chạy dự án

### Yêu cầu môi trường:
- Java 17+ (hoặc Java 21, Java 23)
- Redis Server (Docker hoặc cài đặt trực tiếp trên cổng `6379`)

### Bước 1: Khởi động Redis Server bằng Docker
```bash
docker run -d --name grabfood-redis -p 6379:6379 redis:alpine
```

Kiểm tra kết nối:
```bash
docker exec -it grabfood-redis redis-cli ping
# Output: PONG
```

### Bước 2: Build và chạy ứng dụng Spring Boot
```bash
gradle bootRun
```

### Bước 3: Kiểm tra dữ liệu JSON trong Redis
```bash
# Xem danh sách các keys được cache
docker exec -it grabfood-redis redis-cli keys "*"

# Xem chi tiết JSON của thực đơn nhà hàng 101
docker exec -it grabfood-redis redis-cli get "restaurant_menus::101"
```

---

## 3. Cấu trúc thư mục dự án
```
IT214_Session17_BT1_PhamThanhPhong_N24DTCN120/
├── build.gradle
├── settings.gradle
├── README.md
├── IT214_Session17_BT1_PhamThanhPhong_N24DTCN120.md  <-- Báo cáo chi tiết đầy đủ
└── src/
    ├── main/
    │   ├── java/com/grabfood/cache/
    │   │   ├── GrabFoodApplication.java
    │   │   ├── config/
    │   │   │   └── CacheConfig.java             <-- File cấu hình chính
    │   │   ├── controller/
    │   │   │   └── FoodController.java
    │   │   ├── model/
    │   │   │   ├── FoodItem.java
    │   │   │   ├── Promotion.java
    │   │   │   └── RestaurantMenuDTO.java
    │   │   ├── repository/
    │   │   │   └── FoodRepository.java
    │   │   └── service/
    │   │       ├── FoodService.java
    │   │       └── PromotionService.java
    │   └── resources/
    │       └── application.yml                  <-- Cấu hình tham số Redis
    └── test/
        └── java/com/grabfood/cache/
            ├── FoodServiceTest.java
            └── JsonSerializationTest.java
```
