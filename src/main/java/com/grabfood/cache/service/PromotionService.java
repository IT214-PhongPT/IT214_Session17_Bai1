package com.grabfood.cache.service;

import com.grabfood.cache.model.Promotion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý mã khuyến mãi GrabFood với Distributed Cache (Redis JSON Serialization).
 */
@Slf4j
@Service
public class PromotionService {

    private final Map<String, Promotion> promoDatabase = new ConcurrentHashMap<>();

    public PromotionService() {
        initSamplePromos();
    }

    private void initSamplePromos() {
        promoDatabase.put("GRABFOOD50", Promotion.builder()
                .promoCode("GRABFOOD50")
                .description("Giảm 50% tối đa 50.000 VNĐ cho đơn hàng từ 100k")
                .discountPercentage(new BigDecimal("50"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .validUntil(LocalDateTime.now().plusDays(7))
                .active(true)
                .build());

        promoDatabase.put("FREESHIP", Promotion.builder()
                .promoCode("FREESHIP")
                .description("Miễn phí giao hàng tối đa 15.000 VNĐ")
                .discountPercentage(new BigDecimal("100"))
                .maxDiscountAmount(new BigDecimal("15000"))
                .validUntil(LocalDateTime.now().plusDays(30))
                .active(true)
                .build());
    }

    @Cacheable(
            value = "promotions",
            key = "#promoCode.toUpperCase()",
            condition = "#promoCode != null && !#promoCode.trim().isEmpty()",
            unless = "#result == null || !#result.active"
    )
    public Promotion getPromotion(String promoCode) {
        log.warn("===> [PROMO DB QUERY] Truy vấn CSDL khuyến mãi cho mã: {}...", promoCode);
        try {
            Thread.sleep(150); // Mô phỏng độ trễ CSDL
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return promoDatabase.get(promoCode.toUpperCase());
    }

    @CacheEvict(value = "promotions", key = "#promotion.promoCode.toUpperCase()")
    public Promotion savePromotion(Promotion promotion) {
        if (promotion == null || promotion.getPromoCode() == null) {
            throw new IllegalArgumentException("Khuyến mãi không hợp lệ!");
        }
        log.info("===> [PROMO DB SAVE] Cập nhật khuyến mãi CSDL và xóa cache cho mã: {}", promotion.getPromoCode());
        promoDatabase.put(promotion.getPromoCode().toUpperCase(), promotion);
        return promotion;
    }
}
