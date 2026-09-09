package com.orionkey.service.impl;

import com.orionkey.entity.Order;
import com.orionkey.entity.ProductMapping;
import com.orionkey.repository.ProductMappingRepository;
import com.orionkey.service.TghaoApiService;
import com.orionkey.service.TghaoOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Tghao 订单处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TghaoOrderServiceImpl implements TghaoOrderService {

    private final TghaoApiService tghaoApiService;
    private final ProductMappingRepository productMappingRepository;

    @Override
    public boolean processOrder(Order order, UUID productId, int quantity) {
        try {
            // 查找商品映射
            ProductMapping mapping = productMappingRepository.findByProductIdAndEnabledTrue(productId)
                    .orElse(null);

            if (mapping == null) {
                log.warn("Product {} has no Tghao mapping, skipping proxy order", productId);
                return false;
            }

            String tghaoCode = mapping.getTghaoCode();
            String tghaoRace = mapping.getTghaoRace();

            log.info("Creating Tghao order: code={}, race={}, num={}", tghaoCode, tghaoRace, quantity);

            // 先检查库存
            boolean hasStock = tghaoApiService.checkInventory(tghaoCode, quantity, tghaoRace);
            if (!hasStock) {
                log.error("Tghao inventory check failed for code={}, num={}", tghaoCode, quantity);
                return false;
            }

            // 调用 Tghao API 下单
            String requestNo = "ORK-" + order.getId().toString();
            String contact = order.getEmail() != null ? order.getEmail() : "noreply@orionkey.com";

            Map<String, Object> result = tghaoApiService.createOrder(
                    tghaoCode,
                    quantity,
                    tghaoRace,
                    contact,
                    requestNo
            );

            // 保存 Tghao 订单号
            String tghaoTradeNo = result.get("tradeNo").toString();
            order.setTghaoTradeNo(tghaoTradeNo);
            order.setTghaoProxy(true);

            log.info("Tghao order created successfully: local={}, tghao={}, secret={}",
                    order.getId(), tghaoTradeNo,
                    result.get("secret") != null ? "YES" : "PENDING");

            return true;

        } catch (Exception e) {
            log.error("Failed to create Tghao order for local order {}", order.getId(), e);
            return false;
        }
    }

    @Override
    public String queryOrderSecret(String tghaoTradeNo) {
        try {
            Map<String, Object> result = tghaoApiService.queryOrder(tghaoTradeNo);
            return result.get("secret") != null ? result.get("secret").toString() : null;
        } catch (Exception e) {
            log.error("Failed to query Tghao order {}", tghaoTradeNo, e);
            return null;
        }
    }
}
