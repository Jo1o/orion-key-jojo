package com.orionkey.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orionkey.config.TghaoConfig;
import com.orionkey.exception.BusinessException;
import com.orionkey.service.TghaoApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tghao API 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TghaoApiServiceImpl implements TghaoApiService {

    private final TghaoConfig tghaoConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成签名
     * 算法：将所有参数按key排序，去除空值，拼接成 key1=value1&key2=value2&key=appKey，然后MD5
     */
    private String generateSign(Map<String, Object> params) {
        // 移除 sign 字段
        params.remove("sign");

        // 按 key 排序
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);

        // 去除空值并构建查询字符串
        String query = sortedParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().toString().isEmpty())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        // 拼接 appKey
        String signStr = query + "&key=" + tghaoConfig.getAppKey();

        log.debug("Sign string: {}", signStr);

        // MD5
        return md5(signStr);
    }

    /**
     * MD5 加密
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 encryption failed", e);
        }
    }

    /**
     * 发送 POST 请求
     */
    private Map<String, Object> post(String endpoint, Map<String, Object> params) {
        if (!tghaoConfig.isEnabled()) {
            throw new BusinessException(0, "Tghao 对接未启用");
        }

        // 添加公共参数
        params.put("app_id", tghaoConfig.getAppId());

        // 生成签名
        String sign = generateSign(new HashMap<>(params));
        params.put("sign", sign);

        String url = tghaoConfig.getBaseUrl() + endpoint;

        log.info("Tghao API request: {} with params: {}", url, params);

        try {
            // 构建 form-urlencoded 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            params.forEach((key, value) -> {
                if (value != null) {
                    formData.add(key, value.toString());
                }
            });

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("Tghao API response: {}", response.getBody());

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);

            Integer code = (Integer) result.get("code");
            if (code == null || code != 200) {
                String msg = (String) result.getOrDefault("msg", "请求失败");
                log.error("Tghao API error: code={}, msg={}", code, msg);
                throw new BusinessException(0, "Tghao API 错误: " + msg);
            }

            return result;
        } catch (Exception e) {
            log.error("Tghao API request failed", e);
            throw new BusinessException(0, "调用 Tghao API 失败: " + e.getMessage());
        }
    }

    @Override
    public boolean testConnection() {
        try {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> result = post("/shared/authentication/connect", params);
            return result.get("code").equals(200);
        } catch (Exception e) {
            log.error("Tghao connection test failed", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getProductList() {
        Map<String, Object> params = new HashMap<>();
        return post("/shared/commodity/items", params);
    }

    @Override
    public Map<String, Object> getProductDetail(String code) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        return post("/shared/commodity/item", params);
    }

    @Override
    public boolean checkInventory(String code, int num, String race) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("shared_code", code);
            params.put("num", num);
            if (race != null && !race.isEmpty()) {
                params.put("race", race);
            }
            Map<String, Object> result = post("/shared/commodity/inventoryState", params);
            return result.get("code").equals(200);
        } catch (Exception e) {
            log.warn("Inventory check failed for code={}, num={}", code, num, e);
            return false;
        }
    }

    @Override
    public BigDecimal getPrice(String code, int num, String race) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("num", num);
        if (race != null && !race.isEmpty()) {
            params.put("race", race);
        }

        Map<String, Object> result = post("/shared/commodity/valuation", params);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        String priceStr = data.get("price").toString();
        return new BigDecimal(priceStr);
    }

    @Override
    public Map<String, Object> createOrder(String code, int num, String race, String contact, String requestNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("shared_code", code);
        params.put("num", num);

        if (race != null && !race.isEmpty()) {
            params.put("race", race);
        }
        if (contact != null && !contact.isEmpty()) {
            params.put("contact", contact);
        }
        if (requestNo != null && !requestNo.isEmpty()) {
            params.put("request_no", requestNo);
        }

        params.put("device", 0);

        Map<String, Object> result = post("/shared/commodity/trade", params);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        log.info("Tghao order created: tradeNo={}, amount={}",
                data.get("tradeNo"), data.get("amount"));

        return data;
    }

    @Override
    public Map<String, Object> queryOrder(String tradeNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("tradeNo", tradeNo);

        Map<String, Object> result = post("/shared/commodity/query", params);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        return data;
    }
}
