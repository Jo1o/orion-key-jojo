package com.orionkey.service.impl;

import com.orionkey.entity.Product;
import com.orionkey.entity.ProductCategory;
import com.orionkey.entity.ProductMapping;
import com.orionkey.repository.ProductCategoryRepository;
import com.orionkey.repository.ProductMappingRepository;
import com.orionkey.repository.ProductRepository;
import com.orionkey.service.TghaoApiService;
import com.orionkey.service.TghaoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Tghao 商品同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TghaoSyncServiceImpl implements TghaoSyncService {

    private final TghaoApiService tghaoApiService;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductMappingRepository productMappingRepository;

    @Override
    @Transactional
    public Map<String, Object> syncProduct(String tghaoCode, String categoryId) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 获取 Tghao 商品详情
            Map<String, Object> response = tghaoApiService.getProductDetail(tghaoCode);
            @SuppressWarnings("unchecked")
            Map<String, Object> tghaoProduct = (Map<String, Object>) response.get("data");

            if (tghaoProduct == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }

            // 检查是否已存在映射
            Optional<ProductMapping> existingMapping = productMappingRepository.findByTghaoCode(tghaoCode);
            if (existingMapping.isPresent()) {
                result.put("success", false);
                result.put("message", "商品已存在映射");
                result.put("product_id", existingMapping.get().getProductId());
                return result;
            }

            // 创建本地商品
            Product product = new Product();
            product.setTitle((String) tghaoProduct.get("name"));
            product.setDescription((String) tghaoProduct.get("description"));

            // 价格处理：Tghao 的 user_price 作为成本价，加价 20% 作为售价
            BigDecimal tghaoPrice = new BigDecimal(tghaoProduct.get("user_price").toString());
            BigDecimal markup = new BigDecimal("1.2"); // 加价 20%
            product.setBasePrice(tghaoPrice.multiply(markup));

            // 设置分类
            if (categoryId != null && !categoryId.isEmpty()) {
                product.setCategoryId(UUID.fromString(categoryId));
            }

            product.setLowStockThreshold(5);
            product.setWholesaleEnabled(false);
            product.setEnabled(true);
            product.setSortOrder(0);
            product.setIsDeleted(0);

            productRepository.save(product);

            // 创建映射关系
            ProductMapping mapping = new ProductMapping();
            mapping.setProductId(product.getId());
            mapping.setTghaoCode(tghaoCode);

            // 如果有 category，设置 race
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) tghaoProduct.get("config");
            if (config != null && config.containsKey("category")) {
                // 获取第一个 category 作为默认 race
                @SuppressWarnings("unchecked")
                Map<String, Object> categories = (Map<String, Object>) config.get("category");
                if (!categories.isEmpty()) {
                    String firstRace = (String) categories.keySet().iterator().next();
                    mapping.setTghaoRace(firstRace);
                }
            }

            mapping.setEnabled(true);
            mapping.setRemark("自动同步创建");

            productMappingRepository.save(mapping);

            result.put("success", true);
            result.put("product_id", product.getId());
            result.put("product_title", product.getTitle());
            result.put("tghao_code", tghaoCode);
            result.put("price", product.getBasePrice());

            log.info("Successfully synced product: {} -> {}", tghaoCode, product.getId());

        } catch (Exception e) {
            log.error("Failed to sync product: {}", tghaoCode, e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> syncProducts(List<String> tghaoCodes, String categoryId) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String code : tghaoCodes) {
            Map<String, Object> result = syncProduct(code, categoryId);
            results.add(result);
        }

        return results;
    }

    @Override
    @Transactional
    public Map<String, Object> syncCategory(Integer tghaoCategoryId, String localCategoryId) {
        Map<String, Object> result = new LinkedHashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> syncedProducts = new ArrayList<>();

        try {
            // 获取 Tghao 商品列表
            Map<String, Object> response = tghaoApiService.getProductList();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) response.get("data");

            // 查找指定分类
            Map<String, Object> targetCategory = null;
            for (Map<String, Object> cat : categories) {
                if (tghaoCategoryId.equals(cat.get("id"))) {
                    targetCategory = cat;
                    break;
                }
            }

            if (targetCategory == null) {
                result.put("success", false);
                result.put("message", "Tghao 分类不存在");
                return result;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) targetCategory.get("children");

            if (children != null) {
                for (Map<String, Object> item : children) {
                    String code = (String) item.get("code");
                    Map<String, Object> syncResult = syncProduct(code, localCategoryId);

                    if ((Boolean) syncResult.getOrDefault("success", false)) {
                        successCount++;
                        syncedProducts.add(code);
                    } else {
                        failCount++;
                    }
                }
            }

            result.put("success", true);
            result.put("total", successCount + failCount);
            result.put("success_count", successCount);
            result.put("fail_count", failCount);
            result.put("synced_products", syncedProducts);

        } catch (Exception e) {
            log.error("Failed to sync category: {}", tghaoCategoryId, e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> syncAll(boolean autoCreateCategory) {
        Map<String, Object> result = new LinkedHashMap<>();
        int totalSuccess = 0;
        int totalFail = 0;
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        try {
            // 获取 Tghao 商品列表
            Map<String, Object> response = tghaoApiService.getProductList();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) response.get("data");

            for (Map<String, Object> category : categories) {
                String categoryName = (String) category.get("name");
                Integer categoryId = (Integer) category.get("id");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) category.get("children");

                if (children == null || children.isEmpty()) {
                    continue;
                }

                // 创建或查找本地分类
                UUID localCategoryId = null;
                if (autoCreateCategory) {
                    ProductCategory localCategory = productCategoryRepository
                            .findByName(categoryName)
                            .orElseGet(() -> {
                                ProductCategory newCat = new ProductCategory();
                                newCat.setName(categoryName);
                                newCat.setSortOrder(0);
                                newCat.setIsDeleted(0);
                                return productCategoryRepository.save(newCat);
                            });
                    localCategoryId = localCategory.getId();
                }

                int categorySuccess = 0;
                int categoryFail = 0;

                for (Map<String, Object> item : children) {
                    String code = (String) item.get("code");
                    Map<String, Object> syncResult = syncProduct(code,
                            localCategoryId != null ? localCategoryId.toString() : null);

                    if ((Boolean) syncResult.getOrDefault("success", false)) {
                        categorySuccess++;
                        totalSuccess++;
                    } else {
                        categoryFail++;
                        totalFail++;
                    }
                }

                categoryCounts.put(categoryName, categorySuccess);
                log.info("Synced category {}: {} success, {} failed",
                        categoryName, categorySuccess, categoryFail);
            }

            result.put("success", true);
            result.put("total_success", totalSuccess);
            result.put("total_fail", totalFail);
            result.put("category_counts", categoryCounts);

        } catch (Exception e) {
            log.error("Failed to sync all products", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updatePricesAndStock() {
        Map<String, Object> result = new LinkedHashMap<>();
        int updateCount = 0;
        int errorCount = 0;

        try {
            List<ProductMapping> mappings = productMappingRepository.findAll();

            for (ProductMapping mapping : mappings) {
                if (!mapping.isEnabled()) {
                    continue;
                }

                try {
                    // 获取 Tghao 商品详情
                    Map<String, Object> response = tghaoApiService.getProductDetail(mapping.getTghaoCode());
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tghaoProduct = (Map<String, Object>) response.get("data");

                    if (tghaoProduct == null) {
                        errorCount++;
                        continue;
                    }

                    // 更新价格
                    Product product = productRepository.findById(mapping.getProductId()).orElse(null);
                    if (product != null) {
                        BigDecimal tghaoPrice = new BigDecimal(tghaoProduct.get("user_price").toString());
                        BigDecimal markup = new BigDecimal("1.2"); // 加价 20%
                        product.setBasePrice(tghaoPrice.multiply(markup));
                        productRepository.save(product);
                        updateCount++;
                    }

                } catch (Exception e) {
                    log.warn("Failed to update product {}: {}", mapping.getTghaoCode(), e.getMessage());
                    errorCount++;
                }
            }

            result.put("success", true);
            result.put("updated_count", updateCount);
            result.put("error_count", errorCount);

        } catch (Exception e) {
            log.error("Failed to update prices and stock", e);
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }

        return result;
    }
}
