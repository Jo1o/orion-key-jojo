package com.orionkey.service;

import java.util.List;
import java.util.Map;

/**
 * Tghao 商品同步服务
 */
public interface TghaoSyncService {

    /**
     * 同步单个商品
     * @param tghaoCode Tghao 商品编码
     * @param categoryId 本地分类 ID（可选）
     * @return 同步结果
     */
    Map<String, Object> syncProduct(String tghaoCode, String categoryId);

    /**
     * 批量同步商品
     * @param tghaoCodes Tghao 商品编码列表
     * @param categoryId 本地分类 ID（可选）
     * @return 同步结果列表
     */
    List<Map<String, Object>> syncProducts(List<String> tghaoCodes, String categoryId);

    /**
     * 同步整个分类下的所有商品
     * @param tghaoCategoryId Tghao 分类 ID
     * @param localCategoryId 本地分类 ID
     * @return 同步结果
     */
    Map<String, Object> syncCategory(Integer tghaoCategoryId, String localCategoryId);

    /**
     * 同步所有 Tghao 商品
     * @param autoCreateCategory 是否自动创建分类
     * @return 同步结果统计
     */
    Map<String, Object> syncAll(boolean autoCreateCategory);

    /**
     * 更新已映射商品的价格和库存
     * @return 更新结果统计
     */
    Map<String, Object> updatePricesAndStock();
}
