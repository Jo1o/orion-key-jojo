package com.orionkey.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tghao API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tghao")
public class TghaoConfig {

    /**
     * 是否启用 Tghao 对接
     */
    private boolean enabled = false;

    /**
     * API 基础 URL
     */
    private String baseUrl = "https://tghao.uk";

    /**
     * 商户 ID (app_id)
     */
    private String appId;

    /**
     * 商户密钥 (app_key)
     */
    private String appKey;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;
}
