package com.orionkey.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tghao API 服务接口
 */
public interface TghaoApiService {

    /**
     * 测试连接
     */
    boolean testConnection();

    /**
     * 获取商品列表
     */
    Map<String, Object> getProductList();

    /**
     * 获取商品详情
     * @param code 商品编码
     */
    Map<String, Object> getProductDetail(String code);

    /**
     * 检查库存
     * @param code 商品编码
     * @param num 购买数量
     * @param race 商品种类
     */
    boolean checkInventory(String code, int num, String race);

    /**
     * 询价
     * @param code 商品编码
     * @param num 购买数量
     * @param race 商品种类
     */
    BigDecimal getPrice(String code, int num, String race);

    /**
     * 下单
     * @param code 商品编码
     * @param num 购买数量
     * @param race 商品种类
     * @param contact 联系方式
     * @param requestNo 幂等号
     * @return 订单结果（包含 tradeNo 和 secret）
     */
    Map<String, Object> createOrder(String code, int num, String race, String contact, String requestNo);

    /**
     * 查询订单
     * @param tradeNo 订单号
     */
    Map<String, Object> queryOrder(String tradeNo);
}
