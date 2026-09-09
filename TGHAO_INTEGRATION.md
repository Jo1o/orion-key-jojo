# Tghao API 对接使用指南

## 功能说明

本系统已集成 Tghao API，实现**代理下单**功能：
- 用户在你的商城下单并支付成功后
- 系统自动调用 Tghao API 下单
- 获取 Tghao 返回的卡密
- 自动发货给用户

## 配置步骤

### 1. 获取 Tghao API 凭证

登录 https://tghao.uk/user/index/api 获取：
- **app_id**（商户 ID）
- **app_key**（商户密钥）

### 2. 配置环境变量

在启动后端时设置以下环境变量：

```bash
# 启用 Tghao 对接
TGHAO_ENABLED=true

# Tghao API 地址（默认可不设置）
TGHAO_BASE_URL=https://tghao.uk

# 你的商户 ID
TGHAO_APP_ID=你的app_id

# 你的商户密钥
TGHAO_APP_KEY=你的app_key
```

**示例（PowerShell）：**
```powershell
$env:TGHAO_ENABLED = "true"
$env:TGHAO_APP_ID = "12345"
$env:TGHAO_APP_KEY = "your-secret-key"
```

**或在 application.yml 中配置：**
```yaml
tghao:
  enabled: true
  app-id: 你的app_id
  app-key: 你的app_key
```

### 3. 配置商品映射

使用管理后台 API 创建商品映射关系：

#### 测试连接
```bash
POST /api/admin/tghao/test-connection
```

#### 获取 Tghao 商品列表
```bash
GET /api/admin/tghao/products
```

#### 创建商品映射
```bash
POST /api/admin/tghao/mappings
Content-Type: application/json

{
  "product_id": "本地商品UUID",
  "tghao_code": "tghao商品编码",
  "tghao_race": "商品种类（可选）",
  "remark": "备注说明"
}
```

#### 查看所有映射
```bash
GET /api/admin/tghao/mappings
```

## 工作流程

### 用户下单流程

```
1. 用户选择商品并下单
   ↓
2. 用户完成支付（订单状态：PAID）
   ↓
3. 用户点击"查看卡密"
   ↓
4. 系统检查商品是否有 Tghao 映射
   ├─ 有映射 → 调用 Tghao API 下单
   │            ├─ 成功：获取卡密并发货
   │            └─ 失败：使用本地库存
   └─ 无映射 → 使用本地库存发货
   ↓
5. 订单状态变为 DELIVERED，用户收到卡密
```

### 订单数据

启用 Tghao 代理的订单会记录：
- `tghao_trade_no`: Tghao 订单号
- `tghao_proxy`: 是否使用代理（true/false）

## API 接口说明

### 管理后台接口

所有接口需要管理员权限（`ROLE_ADMIN`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/tghao/test-connection` | POST | 测试 Tghao 连接 |
| `/admin/tghao/products` | GET | 获取 Tghao 商品列表 |
| `/admin/tghao/products/{code}` | GET | 获取商品详情 |
| `/admin/tghao/mappings` | GET | 获取所有映射 |
| `/admin/tghao/mappings` | POST | 创建映射 |
| `/admin/tghao/mappings/{id}` | PUT | 更新映射 |
| `/admin/tghao/mappings/{id}` | DELETE | 删除映射 |

## 数据库表

### product_mappings（商品映射表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| product_id | UUID | 本地商品 ID |
| tghao_code | VARCHAR(100) | Tghao 商品编码 |
| tghao_race | VARCHAR(100) | Tghao 商品种类 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### orders 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| tghao_trade_no | VARCHAR(100) | Tghao 订单号 |
| tghao_proxy | BOOLEAN | 是否使用 Tghao 代理 |

## 注意事项

1. **余额支付**：Tghao API 使用余额支付，请确保你的 Tghao 账户有足够余额

2. **库存管理**：
   - 配置了 Tghao 映射的商品，系统会优先调用 Tghao API
   - 如果 Tghao API 调用失败，会回退到本地库存
   - 建议本地也保留少量库存作为备用

3. **价格管理**：
   - 本地商品价格可以高于 Tghao 拿货价，差价即为你的利润
   - 系统不会自动同步 Tghao 价格，需要手动调整

4. **商品映射**：
   - 一个本地商品只能映射一个 Tghao 商品
   - 如果商品有多个规格，需要在 `tghao_race` 字段指定

5. **错误处理**：
   - Tghao API 调用失败会记录到日志
   - 订单会尝试使用本地库存发货
   - 如果本地也无库存，订单保持 PAID 状态，提示"缺货补货中"

## 测试建议

1. 先使用测试商品验证整个流程
2. 确认 Tghao 余额充足
3. 小批量测试后再大规模使用
4. 监控日志文件，及时发现问题

## 常见问题

### Q: 如何禁用 Tghao 对接？
A: 设置环境变量 `TGHAO_ENABLED=false` 或删除商品映射

### Q: Tghao 订单失败怎么办？
A: 系统会自动回退到本地库存，如果本地也无库存，需要手动处理订单

### Q: 如何查看 Tghao 订单号？
A: 在数据库 `orders` 表中查看 `tghao_trade_no` 字段

### Q: 支持批量下单吗？
A: 支持，系统会根据购买数量自动调用 Tghao API

## 技术支持

如有问题，请查看：
1. 后端日志：`apps/api/logs/`
2. Tghao API 文档：https://tghao.uk/user/index/api
3. 系统错误日志中的详细错误信息
