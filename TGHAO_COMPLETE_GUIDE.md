# Tghao 对接完整功能说明

## 🎉 已实现功能总览

你的自动发卡商城现在已经完整集成了 Tghao API，支持两种模式：

### 模式1：代理下单模式（自动发货）
用户下单 → 支付成功 → 自动调用 Tghao API 下单 → 获取卡密 → 发货给用户

### 模式2：商品自动同步模式（快速上架）
从 Tghao 批量拉取商品 → 自动创建到本地商城 → 无需手动录入

---

## 📋 功能清单

### ✅ 基础功能
- [x] Tghao API 签名算法（MD5）
- [x] API 连接测试
- [x] 获取商品列表
- [x] 获取商品详情
- [x] 检查库存
- [x] 询价
- [x] 下单
- [x] 查询订单

### ✅ 商品映射管理
- [x] 手动创建商品映射
- [x] 查看所有映射
- [x] 更新映射
- [x] 删除映射
- [x] 启用/禁用映射

### ✅ 自动同步功能（新增）
- [x] 同步单个商品
- [x] 批量同步商品
- [x] 按分类同步
- [x] 全量同步所有商品
- [x] 自动创建分类
- [x] 自动计算价格（加价20%）
- [x] 定期更新价格

### ✅ 订单处理
- [x] 支付成功自动调用 Tghao 下单
- [x] 获取 Tghao 卡密并发货
- [x] 失败自动回退到本地库存
- [x] 记录 Tghao 订单号
- [x] 支持查询 Tghao 订单状态

### ✅ 数据库支持
- [x] product_mappings 表（商品映射）
- [x] orders 表扩展（Tghao 订单号、代理标记）
- [x] 自动迁移脚本

---

## 🚀 快速开始

### 第一步：配置 Tghao 凭证

```powershell
# 设置环境变量
$env:TGHAO_ENABLED = "true"
$env:TGHAO_APP_ID = "你的app_id"
$env:TGHAO_APP_KEY = "你的app_key"

# 重启后端
cd D:\workspace\orion-key-jojo\apps\api
mvn spring-boot:run
```

### 第二步：测试连接

```bash
POST http://localhost:8083/api/admin/tghao/test-connection
Authorization: Bearer <管理员Token>
```

### 第三步：选择使用方式

#### 方式A：全自动同步（推荐新站）

```bash
# 一键同步所有 Tghao 商品（自动创建分类）
POST http://localhost:8083/api/admin/tghao/sync/all
Content-Type: application/json

{
  "auto_create_category": true
}
```

**结果：**
- 自动创建所有商品
- 自动创建分类
- 自动建立映射关系
- 价格自动加价 20%

#### 方式B：精选商品同步

```bash
# 1. 获取 Tghao 商品列表
GET http://localhost:8083/api/admin/tghao/products

# 2. 同步指定商品
POST http://localhost:8083/api/admin/tghao/sync/products
Content-Type: application/json

{
  "tghao_codes": ["steam50", "office365", "netflix"],
  "category_id": "分类UUID（可选）"
}
```

#### 方式C：手动映射

```bash
# 1. 手动创建本地商品（在管理后台）
# 2. 创建映射关系
POST http://localhost:8083/api/admin/tghao/mappings
Content-Type: application/json

{
  "product_id": "本地商品UUID",
  "tghao_code": "tghao商品编码",
  "tghao_race": "商品种类（可选）"
}
```

---

## 📊 完整 API 接口列表

### 基础接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/tghao/test-connection` | POST | 测试连接 |
| `/admin/tghao/products` | GET | 获取商品列表 |
| `/admin/tghao/products/{code}` | GET | 获取商品详情 |

### 映射管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/tghao/mappings` | GET | 查看所有映射 |
| `/admin/tghao/mappings` | POST | 创建映射 |
| `/admin/tghao/mappings/{id}` | PUT | 更新映射 |
| `/admin/tghao/mappings/{id}` | DELETE | 删除映射 |

### 自动同步（新增）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/tghao/sync/product` | POST | 同步单个商品 |
| `/admin/tghao/sync/products` | POST | 批量同步商品 |
| `/admin/tghao/sync/category` | POST | 同步整个分类 |
| `/admin/tghao/sync/all` | POST | 同步所有商品 |
| `/admin/tghao/sync/update-prices` | POST | 更新价格 |

---

## 💰 价格策略

### 自动同步时的价格计算

```
本地售价 = Tghao user_price × 1.2
```

**示例：**
- Tghao 拿货价：50 元
- 本地售价：60 元
- 你的利润：10 元

### 修改加价比例

编辑文件：`apps/api/src/main/java/com/orionkey/service/impl/TghaoSyncServiceImpl.java`

```java
// 第 71 行
BigDecimal markup = new BigDecimal("1.2"); // 改为 1.3 即加价 30%
```

---

## 🔄 工作流程

### 用户购买流程

```
1. 用户浏览商品（本地商城）
   ↓
2. 用户下单并支付
   ↓
3. 支付成功，订单状态变为 PAID
   ↓
4. 用户点击"查看卡密"
   ↓
5. 系统检查商品是否有 Tghao 映射
   ├─ 有映射 → 调用 Tghao API 下单
   │            ├─ 成功：获取卡密并发货
   │            └─ 失败：使用本地库存
   └─ 无映射 → 直接使用本地库存
   ↓
6. 订单状态变为 DELIVERED
   ↓
7. 用户收到卡密
```

### 自动同步流程

```
1. 调用同步接口
   ↓
2. 从 Tghao 获取商品信息
   ↓
3. 检查是否已存在映射
   ├─ 存在 → 跳过
   └─ 不存在 → 继续
   ↓
4. 创建本地商品
   ├─ 标题、描述
   ├─ 价格（加价20%）
   └─ 分类（可选）
   ↓
5. 创建商品映射关系
   ↓
6. 完成
```

---

## 📁 相关文档

1. **TGHAO_INTEGRATION.md** - 基础对接文档
   - 配置说明
   - 手动映射
   - 代理下单
   - 故障排查

2. **TGHAO_AUTO_SYNC.md** - 自动同步文档
   - 同步模式详解
   - API 接口文档
   - 使用步骤
   - 最佳实践

3. **C:\Users\51080\Desktop\api对接文档.md** - Tghao 官方文档
   - API 规范
   - 签名算法
   - 接口说明

---

## ⚙️ 配置文件

### application.yml

```yaml
tghao:
  enabled: ${TGHAO_ENABLED:false}
  base-url: ${TGHAO_BASE_URL:https://tghao.uk}
  app-id: ${TGHAO_APP_ID:}
  app-key: ${TGHAO_APP_KEY:}
  connect-timeout: 10000
  read-timeout: 30000
```

### 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| TGHAO_ENABLED | 是否启用 | true |
| TGHAO_APP_ID | 商户ID | 12345 |
| TGHAO_APP_KEY | 商户密钥 | your-secret-key |

---

## 🗄️ 数据库表结构

### product_mappings（商品映射表）

```sql
CREATE TABLE product_mappings (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    tghao_code VARCHAR(100) NOT NULL,
    tghao_race VARCHAR(100),
    is_enabled BOOLEAN DEFAULT true,
    remark VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### orders 表新增字段

```sql
ALTER TABLE orders ADD COLUMN tghao_trade_no VARCHAR(100);
ALTER TABLE orders ADD COLUMN tghao_proxy BOOLEAN DEFAULT false;
```

---

## 🎯 使用建议

### 新建商城

1. ✅ 使用"全量同步"快速上架所有商品
2. ✅ 自动创建分类
3. ✅ 同步后调整分类排序和商品排序
4. ✅ 设置定时任务每天更新价格

### 已有商城

1. ✅ 使用"精选同步"添加部分商品
2. ✅ 手动映射现有商品到 Tghao
3. ✅ 逐步替换本地库存为 Tghao 代理

### 混合模式

1. ✅ 热门商品：使用 Tghao 代理（无需囤货）
2. ✅ 特色商品：使用本地库存（独家资源）
3. ✅ 灵活切换：启用/禁用映射即可

---

## 📈 优势

### 使用 Tghao 代理的好处

1. **零库存风险** - 无需提前囤货
2. **自动发货** - 支付后立即发货
3. **实时库存** - 不会出现缺货情况
4. **价格同步** - 可定时更新价格
5. **品类丰富** - 一键同步大量商品

### 自动同步的好处

1. **快速上架** - 几分钟上架数百商品
2. **自动分类** - 无需手动创建分类
3. **批量管理** - 统一更新价格
4. **降低人工** - 减少手动录入

---

## ⚠️ 注意事项

1. **Tghao 余额** - 确保账户有足够余额
2. **价格策略** - 根据市场调整加价比例
3. **定期更新** - 建议每天更新价格
4. **备用库存** - 保留少量本地库存作为备份
5. **监控日志** - 定期查看后端日志

---

## 🔧 故障排查

### 同步失败

**检查项：**
1. API 配置是否正确
2. 网络连接是否正常
3. Tghao 商品是否存在
4. 查看后端日志详细错误

### 下单失败

**检查项：**
1. Tghao 余额是否充足
2. 商品映射是否正确
3. 商品库存是否充足
4. 查看订单表 tghao_trade_no 字段

### 价格异常

**检查项：**
1. 加价比例设置
2. Tghao 价格是否变动
3. 手动更新价格

---

## 📞 技术支持

**查看日志：**
```bash
# 后端日志
D:\workspace\orion-key-jojo\apps\api\logs\

# 实时日志
tail -f spring.log
```

**数据库查询：**
```sql
-- 查看所有映射
SELECT * FROM product_mappings;

-- 查看 Tghao 订单
SELECT * FROM orders WHERE tghao_proxy = true;
```

---

## 🎊 总结

你现在拥有一个完整的 Tghao 对接方案，包括：

✅ **手动映射** - 精确控制每个商品  
✅ **自动同步** - 快速批量上架  
✅ **代理下单** - 自动发货无需囤货  
✅ **价格管理** - 定时更新保持同步  
✅ **故障回退** - Tghao 失败自动使用本地库存  

**开始使用：**
1. 配置 API 凭证
2. 测试连接
3. 选择同步方式（全量/精选/手动）
4. 开始营业！

祝生意兴隆！🎉
