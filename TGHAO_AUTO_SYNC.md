# Tghao 商品自动同步功能

## 功能说明

系统支持从 Tghao 自动拉取商品并创建到本地商城，无需手动创建商品和映射关系。

## 同步模式

### 1. 同步单个商品
拉取指定 Tghao 商品并创建到本地

### 2. 批量同步商品
一次同步多个 Tghao 商品

### 3. 同步整个分类
同步 Tghao 某个分类下的所有商品

### 4. 全量同步
同步 Tghao 所有商品（支持自动创建分类）

### 5. 更新价格
更新已映射商品的价格和库存

## API 接口

所有接口需要管理员权限，请求头需要带上 `Authorization: Bearer <token>`

### 1. 同步单个商品

```bash
POST /api/admin/tghao/sync/product
Content-Type: application/json

{
  "tghao_code": "商品编码",
  "category_id": "本地分类UUID（可选）"
}
```

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "product_id": "uuid",
    "product_title": "Steam 50元充值卡",
    "tghao_code": "steam50",
    "price": 60.00
  }
}
```

### 2. 批量同步商品

```bash
POST /api/admin/tghao/sync/products
Content-Type: application/json

{
  "tghao_codes": ["code1", "code2", "code3"],
  "category_id": "本地分类UUID（可选）"
}
```

### 3. 同步整个分类

```bash
POST /api/admin/tghao/sync/category
Content-Type: application/json

{
  "tghao_category_id": 1,
  "local_category_id": "本地分类UUID"
}
```

**说明：**
- `tghao_category_id`: 从 `/api/admin/tghao/products` 接口获取的 Tghao 分类 ID
- `local_category_id`: 本地分类 UUID，商品会创建到这个分类下

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "total": 10,
    "success_count": 9,
    "fail_count": 1,
    "synced_products": ["code1", "code2", ...]
  }
}
```

### 4. 全量同步所有商品

```bash
POST /api/admin/tghao/sync/all
Content-Type: application/json

{
  "auto_create_category": true
}
```

**参数说明：**
- `auto_create_category`: 是否自动创建分类
  - `true`: 自动根据 Tghao 分类名称创建本地分类
  - `false`: 所有商品不分配分类

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "total_success": 150,
    "total_fail": 5,
    "category_counts": {
      "游戏充值": 50,
      "软件激活": 30,
      "会员订阅": 70
    }
  }
}
```

### 5. 更新价格和库存

```bash
POST /api/admin/tghao/sync/update-prices
```

**说明：**
- 遍历所有已映射的商品
- 从 Tghao 获取最新价格
- 自动更新本地商品价格（加价 20%）

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "updated_count": 100,
    "error_count": 2
  }
}
```

## 使用步骤

### 方式一：全量自动同步（推荐新站）

**适合场景：** 新建商城，想快速上架所有 Tghao 商品

```bash
# 1. 测试连接
POST /api/admin/tghao/test-connection

# 2. 全量同步（自动创建分类）
POST /api/admin/tghao/sync/all
{
  "auto_create_category": true
}

# 3. 等待同步完成（可能需要几分钟）
# 4. 访问前台查看商品
```

### 方式二：按分类同步（推荐）

**适合场景：** 只想上架部分分类的商品

```bash
# 1. 获取 Tghao 分类列表
GET /api/admin/tghao/products

# 2. 创建本地分类（在管理后台操作）
POST /api/admin/categories
{
  "name": "游戏充值"
}

# 3. 同步指定分类
POST /api/admin/tghao/sync/category
{
  "tghao_category_id": 1,
  "local_category_id": "本地分类UUID"
}
```

### 方式三：精选商品同步

**适合场景：** 只想上架部分精选商品

```bash
# 1. 浏览 Tghao 商品列表
GET /api/admin/tghao/products

# 2. 查看商品详情
GET /api/admin/tghao/products/{code}

# 3. 批量同步选中的商品
POST /api/admin/tghao/sync/products
{
  "tghao_codes": ["steam50", "office365", "netflix"],
  "category_id": "分类UUID"
}
```

## 定时更新价格

建议每天自动更新一次价格，确保价格与 Tghao 保持同步。

**方式1：手动触发**
```bash
POST /api/admin/tghao/sync/update-prices
```

**方式2：计划任务（推荐）**
- Windows: 任务计划程序
- Linux: Crontab

```bash
# 每天凌晨2点更新价格
0 2 * * * curl -X POST http://localhost:8083/api/admin/tghao/sync/update-prices \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 价格策略

同步时的价格计算：
```
本地售价 = Tghao user_price × 1.2
```

**说明：**
- `user_price`: Tghao 的会员价/代理价（你的拿货价）
- 默认加价 20%
- 可以在代码中修改加价比例：`TghaoSyncServiceImpl.java` 第 71 行

**示例：**
- Tghao 拿货价：50 元
- 本地售价：60 元
- 利润：10 元

## 商品映射关系

同步后自动创建商品映射关系：
- `product_id`: 本地商品 ID
- `tghao_code`: Tghao 商品编码
- `tghao_race`: 商品种类（如果有）
- `enabled`: 启用状态

## 同步规则

1. **去重处理：** 已存在映射的商品不会重复创建
2. **自动启用：** 同步的商品默认启用状态
3. **价格加成：** 自动加价 20%（可配置）
4. **分类处理：** 
   - 手动指定分类：商品创建到指定分类
   - 自动创建分类：根据 Tghao 分类名称创建本地分类
   - 不指定分类：商品不分配分类

## 注意事项

1. **大量同步时间较长**
   - 全量同步可能需要几分钟
   - 建议在非营业时间进行大批量同步

2. **价格更新频率**
   - 建议每天更新一次
   - 避免过于频繁更新（增加 API 调用成本）

3. **分类管理**
   - 自动创建的分类可以后续在管理后台修改
   - 建议先规划好分类结构

4. **商品编辑**
   - 同步后可以在管理后台编辑商品
   - 手动修改的价格不会被自动更新覆盖（除非调用更新价格接口）

5. **已售商品**
   - 不要删除有订单的商品映射关系
   - 禁用即可：`enabled: false`

## 工作流程图

```
获取 Tghao 商品
      ↓
检查是否已存在映射
      ↓
   (存在) → 跳过
      ↓
   (不存在)
      ↓
创建本地商品
      ↓
设置价格（加价20%）
      ↓
创建商品映射
      ↓
   完成
```

## 故障排查

### 问题1：同步失败
**可能原因：**
- Tghao API 配置错误
- 网络连接问题
- Tghao 商品不存在

**解决方法：**
1. 检查 API 配置：`TGHAO_APP_ID` 和 `TGHAO_APP_KEY`
2. 测试连接：`POST /api/admin/tghao/test-connection`
3. 查看后端日志

### 问题2：价格不合理
**可能原因：**
- 加价比例设置不当

**解决方法：**
- 修改 `TghaoSyncServiceImpl.java` 中的加价比例
- 或同步后手动调整价格

### 问题3：商品重复
**可能原因：**
- 映射关系被删除后重新同步

**解决方法：**
- 检查 `product_mappings` 表
- 禁用映射而不是删除

## 最佳实践

1. **首次使用**
   - 先测试连接
   - 同步1-2个商品测试完整流程
   - 确认无误后批量同步

2. **日常维护**
   - 每天自动更新价格
   - 定期检查 Tghao 新品
   - 及时下架停售商品

3. **分类管理**
   - 使用自动创建分类
   - 同步后调整分类排序
   - 合并相似分类

4. **价格策略**
   - 根据市场调整加价比例
   - 热门商品可降低加价
   - 冷门商品可提高加价

## 示例脚本

### PowerShell 批量同步脚本

```powershell
# 配置
$baseUrl = "http://localhost:8083/api"
$token = "your_admin_token"

# 测试连接
$response = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/test-connection" `
    -Method POST `
    -Headers @{ "Authorization" = "Bearer $token" }

if ($response.data.connected) {
    Write-Host "连接成功，开始同步..."
    
    # 全量同步
    $syncResult = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/sync/all" `
        -Method POST `
        -Headers @{ 
            "Authorization" = "Bearer $token"
            "Content-Type" = "application/json"
        } `
        -Body '{"auto_create_category": true}'
    
    Write-Host "同步完成："
    Write-Host "成功: $($syncResult.data.total_success)"
    Write-Host "失败: $($syncResult.data.total_fail)"
} else {
    Write-Host "连接失败，请检查配置"
}
```

## 技术支持

如有问题，请查看：
1. 后端日志：`apps/api/logs/`
2. API 响应中的错误信息
3. Tghao API 文档
