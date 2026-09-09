package com.orionkey.controller;

import com.orionkey.common.ApiResponse;
import com.orionkey.entity.ProductMapping;
import com.orionkey.repository.ProductMappingRepository;
import com.orionkey.service.TghaoApiService;
import com.orionkey.service.TghaoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理后台 - Tghao 对接管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/tghao")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTghaoController {

    private final TghaoApiService tghaoApiService;
    private final TghaoSyncService tghaoSyncService;
    private final ProductMappingRepository productMappingRepository;

    /**
     * 测试 Tghao 连接
     */
    @PostMapping("/test-connection")
    public ApiResponse<Map<String, Object>> testConnection() {
        boolean connected = tghaoApiService.testConnection();
        return ApiResponse.success(Map.of(
                "connected", connected,
                "message", connected ? "连接成功" : "连接失败"
        ));
    }

    /**
     * 获取 Tghao 商品列表
     */
    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> getProducts() {
        Map<String, Object> result = tghaoApiService.getProductList();
        return ApiResponse.success(result);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/products/{code}")
    public ApiResponse<Map<String, Object>> getProductDetail(@PathVariable String code) {
        Map<String, Object> result = tghaoApiService.getProductDetail(code);
        return ApiResponse.success(result);
    }

    /**
     * 获取所有商品映射
     */
    @GetMapping("/mappings")
    public ApiResponse<List<ProductMapping>> getMappings() {
        List<ProductMapping> mappings = productMappingRepository.findAll();
        return ApiResponse.success(mappings);
    }

    /**
     * 创建商品映射
     */
    @PostMapping("/mappings")
    public ApiResponse<ProductMapping> createMapping(@RequestBody Map<String, Object> request) {
        ProductMapping mapping = new ProductMapping();
        mapping.setProductId(UUID.fromString((String) request.get("product_id")));
        mapping.setTghaoCode((String) request.get("tghao_code"));
        mapping.setTghaoRace((String) request.get("tghao_race"));
        mapping.setEnabled(true);
        mapping.setRemark((String) request.get("remark"));

        productMappingRepository.save(mapping);
        return ApiResponse.success(mapping);
    }

    /**
     * 更新商品映射
     */
    @PutMapping("/mappings/{id}")
    public ApiResponse<ProductMapping> updateMapping(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        ProductMapping mapping = productMappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("映射不存在"));

        if (request.containsKey("tghao_code")) {
            mapping.setTghaoCode((String) request.get("tghao_code"));
        }
        if (request.containsKey("tghao_race")) {
            mapping.setTghaoRace((String) request.get("tghao_race"));
        }
        if (request.containsKey("enabled")) {
            mapping.setEnabled((Boolean) request.get("enabled"));
        }
        if (request.containsKey("remark")) {
            mapping.setRemark((String) request.get("remark"));
        }

        productMappingRepository.save(mapping);
        return ApiResponse.success(mapping);
    }

    /**
     * 删除商品映射
     */
    @DeleteMapping("/mappings/{id}")
    public ApiResponse<Void> deleteMapping(@PathVariable UUID id) {
        productMappingRepository.deleteById(id);
        return ApiResponse.success(null);
    }

    // ========== 商品同步接口 ==========

    /**
     * 同步单个商品
     */
    @PostMapping("/sync/product")
    public ApiResponse<Map<String, Object>> syncProduct(@RequestBody Map<String, Object> request) {
        String tghaoCode = (String) request.get("tghao_code");
        String categoryId = (String) request.get("category_id");

        Map<String, Object> result = tghaoSyncService.syncProduct(tghaoCode, categoryId);
        return ApiResponse.success(result);
    }

    /**
     * 批量同步商品
     */
    @PostMapping("/sync/products")
    public ApiResponse<List<Map<String, Object>>> syncProducts(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> tghaoCodes = (List<String>) request.get("tghao_codes");
        String categoryId = (String) request.get("category_id");

        List<Map<String, Object>> results = tghaoSyncService.syncProducts(tghaoCodes, categoryId);
        return ApiResponse.success(results);
    }

    /**
     * 同步整个分类
     */
    @PostMapping("/sync/category")
    public ApiResponse<Map<String, Object>> syncCategory(@RequestBody Map<String, Object> request) {
        Integer tghaoCategoryId = (Integer) request.get("tghao_category_id");
        String localCategoryId = (String) request.get("local_category_id");

        Map<String, Object> result = tghaoSyncService.syncCategory(tghaoCategoryId, localCategoryId);
        return ApiResponse.success(result);
    }

    /**
     * 同步所有商品
     */
    @PostMapping("/sync/all")
    public ApiResponse<Map<String, Object>> syncAll(@RequestBody(required = false) Map<String, Object> request) {
        boolean autoCreateCategory = request != null &&
                (Boolean) request.getOrDefault("auto_create_category", false);

        Map<String, Object> result = tghaoSyncService.syncAll(autoCreateCategory);
        return ApiResponse.success(result);
    }

    /**
     * 更新已映射商品的价格和库存
     */
    @PostMapping("/sync/update-prices")
    public ApiResponse<Map<String, Object>> updatePrices() {
        Map<String, Object> result = tghaoSyncService.updatePricesAndStock();
        return ApiResponse.success(result);
    }
}
