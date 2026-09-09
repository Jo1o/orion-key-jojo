# 后端重启脚本
# 使用方法：在 PowerShell 中运行 .\restart-backend.ps1

Write-Host "正在重启后端..." -ForegroundColor Green

# 配置环境变量
$env:JAVA_HOME = "$env:USERPROFILE\Java\jdk-22.0.2+9"
$env:PATH = "$env:JAVA_HOME\bin;C:\Users\51080\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin;$env:PATH"

# 数据库配置
$env:DB_URL = "jdbc:postgresql://localhost:5432/db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "123456"

# Tghao 配置（根据需要修改）
$env:TGHAO_ENABLED = "true"  # 改为 "true" 启用 Tghao
$env:TGHAO_APP_ID = "2853"        # 填写你的 app_id
$env:TGHAO_APP_KEY = "87690A94DD4EC45B"       # 填写你的 app_key

# 进入项目目录
Set-Location "D:\workspace\orion-key-jojo\apps\api"

# 停止现有进程（如果有）
Write-Host "停止现有后端进程..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*spring-boot*"
} | Stop-Process -Force

Start-Sleep -Seconds 2

# 启动后端
Write-Host "启动后端服务..." -ForegroundColor Green
mvn spring-boot:run
