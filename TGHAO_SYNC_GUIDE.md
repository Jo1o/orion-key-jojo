# Tghao 商品批量同步使用指南

## 快速开始

### 第一步：配置 Tghao 凭证

1. 访问 https://tghao.uk/user/index/api 获取你的凭证
2. 编辑 `restart-backend.ps1` 文件，找到 Tghao 配置区域：

```powershell
$env:TGHAO_ENABLED = "true"
$env:TGHAO_APP_ID = "你的app_id"
$env:TGHAO_APP_KEY = "你的app_key"
$env:TGHAO_BASE_URL = "https://tghao.uk/api.html"
```

3. 运行重启脚本：
```powershell
.\restart-backend.ps1
```

### 第二步：运行同步工具

在项目根目录下运行：

```powershell
.\sync-products.ps1
```

## 三种同步方式详解

### 方式一：全量同步（推荐首次使用）

**适用场景：**
- 第一次对接 Tghao，需要导入所有商品
- 需要完整更新商品库

**特点：**
- ✓ 自动同步所有商品
- ✓ 自动创建分类
- ✓ 自动计算售价（Tghao进价 × 1.2）
- ⚠️ 耗时较长（取决于商品数量）

**操作步骤：**
1. 运行 `.\sync-products.ps1`
2. 选择选项 `1`
3. 确认同步
4. 等待完成

**返回结果示例：**
```
✓ 同步完成！
  - 成功: 156 个
  - 失败: 0 个

各分类同步统计：
  游戏点卡: 45 个
  视频会员: 38 个
  加速器: 28 个
  ...
```

---

### 方式二：按分类同步

**适用场景：**
- 只需要某个分类的商品（如只卖游戏点卡）
- 增量添加新分类

**特点：**
- ✓ 精确控制同步范围
- ✓ 速度快
- ✓ 可以指定本地分类

**操作步骤：**
1. 运行 `.\sync-products.ps1`
2. 选择选项 `2`
3. 查看可用分类列表
4. 输入要同步的分类 ID（如 `1`）
5. 等待完成

**示例：**
```
可用分类：
  [1] 游戏点卡 - 45 个商品
  [2] 视频会员 - 38 个商品
  [3] 加速器 - 28 个商品

请输入要同步的分类ID: 1
```

**手动指定本地分类（高级）：**

如果要同步到已有分类，可以用 curl 或 Postman：

```bash
curl -X POST http://localhost:8083/api/admin/tghao/sync/category \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tghao_category_id": 1,
    "local_category_id": 5
  }'
```

---

### 方式三：精选同步

**适用场景：**
- 只需要几个特定商品
- 测试对接功能
- 手动选品

**特点：**
- ✓ 完全自定义
- ✓ 适合小批量操作
- ✓ 可以从 Tghao 商品列表中选择

**操作步骤：**
1. 运行 `.\sync-products.ps1`
2. 选择选项 `3`
3. 查看示例商品编码
4. 输入要同步的商品编码（用逗号分隔）
5. 等待完成

**示例：**
```
请输入商品编码（多个用逗号分隔）: steam100,psn200,xbox50

正在同步 3 个商品...

✓ 同步完成！
  ✓ steam100 - Steam 100元点卡
  ✓ psn200 - PSN 200港币充值卡
  ✗ xbox50 - 商品不存在

成功: 2 个，失败: 1 个
```

---

## 价格和库存管理

### 价格策略

**默认加价规则：**
- 售价 = Tghao进价 × 1.2（加价20%）

**修改加价比例：**

编辑 `apps/api/src/main/java/com/orionkey/service/impl/TghaoSyncServiceImpl.java`：

```java
// 找到这一行（约第168行）
BigDecimal markup = new BigDecimal("1.2");  // 1.2 = 加价20%

// 修改为你想要的比例
BigDecimal markup = new BigDecimal("1.5");  // 1.5 = 加价50%
BigDecimal markup = new BigDecimal("1.1");  // 1.1 = 加价10%
```

修改后重新编译：
```powershell
.\restart-backend.ps1
```

### 库存同步

**自动更新所有已同步商品的价格和库存：**

```powershell
# PowerShell 方式
$token = "你的管理员Token"
Invoke-RestMethod -Uri "http://localhost:8083/api/admin/tghao/sync/update-prices" `
  -Method POST `
  -Headers @{ "Authorization" = "Bearer $token" }
```

**建议：**
- 每天定时执行一次价格库存更新
- 使用 Windows 任务计划程序自动执行

---

## 常见问题

### Q1: 同步失败：连接 Tghao 失败

**可能原因：**
- TGHAO_ENABLED 未设置为 true
- TGHAO_APP_ID 或 TGHAO_APP_KEY 错误
- 网络问题

**解决方法：**
1. 检查 `restart-backend.ps1` 中的配置
2. 确认凭证正确（在 Tghao 后台查看）
3. 重启后端：`.\restart-backend.ps1`

---

### Q2: 同步成功但看不到商品

**可能原因：**
- 商品未上架
- 分类未启用

**解决方法：**
1. 登录管理后台 http://localhost:3000/admin
2. 进入商品管理，批量上架
3. 检查分类是否启用

---

### Q3: 同步的商品价格不对

**解决方法：**
1. 修改加价比例（见上文"价格策略"）
2. 或者同步后手动调整单个商品价格

---

### Q4: 想要删除同步的商品重新来

**解决方法：**

```sql
-- 删除所有商品映射
DELETE FROM product_mappings;

-- 删除所有商品（可选）
DELETE FROM products WHERE id > 1;

-- 重置商品ID自增（可选）
ALTER SEQUENCE products_id_seq RESTART WITH 2;
```

然后重新同步。

---

## 使用 Postman/Curl 手动调用

### 获取 Token

```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

返回：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {...}
  }
}
```

### 测试连接

```bash
curl -X POST http://localhost:8083/api/admin/tghao/test-connection \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 全量同步

```bash
curl -X POST http://localhost:8083/api/admin/tghao/sync/all \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"auto_create_category": true}'
```

### 分类同步

```bash
curl -X POST http://localhost:8083/api/admin/tghao/sync/category \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tghao_category_id": 1,
    "local_category_id": null
  }'
```

### 精选同步

```bash
curl -X POST http://localhost:8083/api/admin/tghao/sync/products \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tghao_codes": ["steam100", "psn200"],
    "category_id": null
  }'
```

---

## 工作流程总结

```
1. 配置 Tghao 凭证
   ↓
2. 重启后端
   ↓
3. 运行 sync-products.ps1
   ↓
4. 选择同步方式（全量/分类/精选）
   ↓
5. 等待同步完成
   ↓
6. 访问前台查看商品
   ↓
7. 在管理后台上架商品
   ↓
8. 用户下单 → 自动对接 Tghao 发货
```

---

## 技术说明

### 同步原理

1. **获取 Tghao 商品列表** - 调用 Tghao API 获取商品信息
2. **创建本地商品** - 在本地数据库创建商品记录
3. **创建映射关系** - 在 `product_mappings` 表建立关联
4. **价格计算** - 自动加价生成售价

### 订单流程

```
用户下单（本地商城）
   ↓
支付成功
   ↓
系统检查是否有 Tghao 映射
   ↓
有映射 → 自动在 Tghao 下单 → 获取卡密 → 发货给用户
   ↓
无映射 → 使用本地库存发货
```

### 数据库表

- `products` - 商品表
- `product_mappings` - Tghao映射表
- `orders` - 订单表（含 tghao_trade_no 字段）
- `card_keys` - 卡密表

---

## 下一步

同步完成后：

1. ✓ 访问前台：http://localhost:3000
2. ✓ 查看商品列表
3. ✓ 测试下单流程
4. ✓ 检查 Tghao 自动发货

有问题随时询问！
