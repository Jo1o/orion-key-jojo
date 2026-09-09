package com.orionkey.service;

import com.orionkey.entity.Order;

import java.util.UUID;

/**
 * Tghao 订单处理服务
 */
public interface TghaoOrderService {

    /**
     * 处理订单 - 调用 Tghao API 下单并发货
     * @param order 本地订单
     * @param productId 商品ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean processOrder(Order order, UUID productId, int quantity);

    /**
     * 查询 Tghao 订单状态
     * @param tghaoTradeNo Tghao 订单号
     * @return 订单信息
     */
    String queryOrderSecret(String tghaoTradeNo);
}
