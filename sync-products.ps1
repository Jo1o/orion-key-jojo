# Tghao 商品批量同步测试脚本
# 使用方法：在 PowerShell 中运行此脚本

# ========== 配置区域 ==========
$baseUrl = "http://localhost:8083/api"
$adminUsername = "admin"
$adminPassword = "123456#"

Write-Host ("=" * 60) -ForegroundColor Cyan
Write-Host "Tghao 商品批量同步工具" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Cyan
Write-Host ""

# ========== 步骤1：登录获取 Token ==========
Write-Host "[1/5] 登录管理员账户..." -ForegroundColor Yellow

$loginBody = @{
    account = $adminUsername
    password = $adminPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    $token = $loginResponse.data.token
    Write-Host "成功登录！Token: $($token.Substring(0, 20))..." -ForegroundColor Green
}
catch {
    Write-Host "登录失败: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host ""

# ========== 步骤2：测试 Tghao 连接 ==========
Write-Host "[2/5] 测试 Tghao API 连接..." -ForegroundColor Yellow

try {
    $testResponse = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/test-connection" -Method POST -Headers @{ "Authorization" = "Bearer $token" }
    if ($testResponse.data.connected) {
        Write-Host "Tghao 连接成功！" -ForegroundColor Green
    }
    else {
        Write-Host "Tghao 连接失败！请检查配置" -ForegroundColor Red
        Write-Host "提示：请确保在 restart-backend.ps1 中配置了正确的 TGHAO_APP_ID 和 TGHAO_APP_KEY" -ForegroundColor Yellow
        exit
    }
}
catch {
    Write-Host "测试连接失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "可能原因：" -ForegroundColor Yellow
    Write-Host "  1. TGHAO_ENABLED 未设置为 true" -ForegroundColor Yellow
    Write-Host "  2. TGHAO_APP_ID 或 TGHAO_APP_KEY 配置错误" -ForegroundColor Yellow
    Write-Host "  3. 后端未重启" -ForegroundColor Yellow
    exit
}

Write-Host ""

# ========== 步骤3：获取 Tghao 商品列表 ==========
Write-Host "[3/5] 获取 Tghao 商品列表..." -ForegroundColor Yellow

try {
    $productsResponse = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/products" -Method GET -Headers @{ "Authorization" = "Bearer $token" }
    $categories = $productsResponse.data
    $totalProducts = 0

    Write-Host "获取成功！Tghao 商品分类：" -ForegroundColor Green
    Write-Host ""

    foreach ($category in $categories) {
        $childCount = 0
        if ($category.children) {
            $childCount = $category.children.Count
            $totalProducts += $childCount
        }
        Write-Host "  [$($category.id)] $($category.name) - $childCount 个商品" -ForegroundColor Cyan
    }

    Write-Host ""
    Write-Host "总计：$totalProducts 个商品可同步" -ForegroundColor Green
}
catch {
    Write-Host "获取商品列表失败: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host ""

# ========== 步骤4：选择同步方式 ==========
Write-Host "[4/5] 选择同步方式" -ForegroundColor Yellow
Write-Host ""
Write-Host "请选择同步方式：" -ForegroundColor White
Write-Host "  [1] 全量同步（同步所有商品，自动创建分类）" -ForegroundColor Cyan
Write-Host "  [2] 按分类同步（选择特定分类同步）" -ForegroundColor Cyan
Write-Host "  [3] 精选同步（输入商品编码同步）" -ForegroundColor Cyan
Write-Host "  [0] 退出" -ForegroundColor Gray
Write-Host ""

$choice = Read-Host "请输入选项 (0-3)"

switch ($choice) {
    "1" {
        # ========== 全量同步 ==========
        Write-Host ""
        Write-Host "[5/5] 开始全量同步..." -ForegroundColor Yellow
        Write-Host "警告：这将同步所有 $totalProducts 个商品，可能需要几分钟" -ForegroundColor Red
        $confirm = Read-Host "确认继续？(y/n)"

        if ($confirm -eq "y" -or $confirm -eq "Y") {
            try {
                $syncBody = @{
                    auto_create_category = $true
                } | ConvertTo-Json

                Write-Host "正在同步，请稍候..." -ForegroundColor Yellow

                $syncResponse = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/sync/all" -Method POST -ContentType "application/json" -Headers @{ "Authorization" = "Bearer $token" } -Body $syncBody -TimeoutSec 600

                Write-Host ""
                Write-Host "同步完成！" -ForegroundColor Green
                Write-Host "  - 成功: $($syncResponse.data.total_success) 个" -ForegroundColor Green
                Write-Host "  - 失败: $($syncResponse.data.total_fail) 个" -ForegroundColor Red
                Write-Host ""
                Write-Host "各分类同步统计：" -ForegroundColor Cyan
                foreach ($cat in $syncResponse.data.category_counts.PSObject.Properties) {
                    Write-Host "  $($cat.Name): $($cat.Value) 个" -ForegroundColor White
                }
            }
            catch {
                Write-Host "同步失败: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
        else {
            Write-Host "已取消" -ForegroundColor Yellow
        }
    }

    "2" {
        # ========== 按分类同步 ==========
        Write-Host ""
        Write-Host "[5/5] 按分类同步" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "可用分类：" -ForegroundColor Cyan
        foreach ($category in $categories) {
            if ($category.children -and $category.children.Count -gt 0) {
                Write-Host "  [$($category.id)] $($category.name) - $($category.children.Count) 个商品" -ForegroundColor White
            }
        }
        Write-Host ""

        $categoryId = Read-Host "请输入要同步的分类ID"

        try {
            $syncBody = @{
                tghao_category_id = [int]$categoryId
                local_category_id = $null
            } | ConvertTo-Json

            Write-Host "正在同步分类 $categoryId ..." -ForegroundColor Yellow

            $syncResponse = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/sync/category" -Method POST -ContentType "application/json" -Headers @{ "Authorization" = "Bearer $token" } -Body $syncBody -TimeoutSec 300

            Write-Host ""
            Write-Host "同步完成！" -ForegroundColor Green
            Write-Host "  - 总计: $($syncResponse.data.total) 个" -ForegroundColor White
            Write-Host "  - 成功: $($syncResponse.data.success_count) 个" -ForegroundColor Green
            Write-Host "  - 失败: $($syncResponse.data.fail_count) 个" -ForegroundColor Red
        }
        catch {
            Write-Host "同步失败: $($_.Exception.Message)" -ForegroundColor Red
        }
    }

    "3" {
        # ========== 精选同步 ==========
        Write-Host ""
        Write-Host "[5/5] 精选商品同步" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "示例商品编码：" -ForegroundColor Cyan

        $exampleCount = 0
        foreach ($category in $categories) {
            if ($category.children -and $exampleCount -lt 3) {
                Write-Host "  $($category.name)：" -ForegroundColor White
                $itemCount = 0
                foreach ($item in $category.children) {
                    if ($itemCount -lt 2) {
                        Write-Host "    - $($item.code) ($($item.name))" -ForegroundColor Gray
                        $itemCount++
                    }
                }
                $exampleCount++
            }
        }

        Write-Host ""
        $codes = Read-Host "请输入商品编码（多个用逗号分隔，如：code1,code2,code3）"

        if ($codes) {
            $codeArray = $codes -split "," | ForEach-Object { $_.Trim() }

            try {
                $syncBody = @{
                    tghao_codes = $codeArray
                    category_id = $null
                } | ConvertTo-Json

                Write-Host "正在同步 $($codeArray.Count) 个商品..." -ForegroundColor Yellow

                $syncResponse = Invoke-RestMethod -Uri "$baseUrl/admin/tghao/sync/products" -Method POST -ContentType "application/json" -Headers @{ "Authorization" = "Bearer $token" } -Body $syncBody -TimeoutSec 300

                Write-Host ""
                Write-Host "同步完成！" -ForegroundColor Green

                $successCount = 0
                $failCount = 0
                foreach ($result in $syncResponse.data) {
                    if ($result.success) {
                        $successCount++
                        Write-Host "  成功: $($result.tghao_code) - $($result.product_title)" -ForegroundColor Green
                    }
                    else {
                        $failCount++
                        Write-Host "  失败: $($result.tghao_code) - $($result.message)" -ForegroundColor Red
                    }
                }

                Write-Host ""
                Write-Host "成功: $successCount 个，失败: $failCount 个" -ForegroundColor Cyan
            }
            catch {
                Write-Host "同步失败: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }

    "0" {
        Write-Host "已退出" -ForegroundColor Yellow
        exit
    }

    default {
        Write-Host "无效选项" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ("=" * 60) -ForegroundColor Cyan
Write-Host "完成！现在可以访问前台查看商品：http://localhost:3000" -ForegroundColor Green
Write-Host ("=" * 60) -ForegroundColor Cyan