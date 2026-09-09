package com.orionkey.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 商品映射表 - 映射本地商品到 Tghao 商品
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_mappings")
public class ProductMapping extends BaseEntity {

    /**
     * 本地商品 ID
     */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * Tghao 商品编码
     */
    @Column(name = "tghao_code", nullable = false, length = 100)
    private String tghaoCode;

    /**
     * Tghao 商品种类（race）
     */
    @Column(name = "tghao_race", length = 100)
    private String tghaoRace;

    /**
     * 是否启用
     */
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
}
