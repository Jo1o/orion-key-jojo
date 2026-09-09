# 停止后端脚本
# 使用方法：在 PowerShell 中运行 .\stop-backend.ps1

Write-Host "正在停止后端..." -ForegroundColor Yellow

# 查找并停止 Spring Boot 进程
$processes = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*spring-boot*" -or $_.CommandLine -like "*orion-key*"
}

if ($processes) {
    $processes | Stop-Process -Force
    Write-Host "后端已停止" -ForegroundColor Green
} else {
    Write-Host "未找到运行中的后端进程" -ForegroundColor Yellow
}
