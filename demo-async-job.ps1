# WorkHub - Async Job Creation and Completion Demo Script (PowerShell Version)
# Make sure the application and Kafka are running before executing this script.

$BaseUrl = "http://localhost:8080"
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "WorkHub Async Job Demo" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Read Tenant ID from the seeder file
if (-not (Test-Path ".tenant_ids.txt")) {
    Write-Host "Error: .tenant_ids.txt not found. Ensure the application has started and seeded the data." -ForegroundColor Red
    exit 1
}
$TenantId = Get-Content ".tenant_ids.txt" -Raw
$TenantId = $TenantId.Trim()
Write-Host "1. Using Seeded Tenant ID: $TenantId"

# 2. Register/Login as a TENANT_ADMIN user
Write-Host "2. Authenticating as Admin User..."
$RegisterBody = @{
    email = "admin_demo@workhub.local"
    password = "password123"
    tenantId = $TenantId
    tenantRole = "TENANT_ADMIN"
} | ConvertTo-Json

try {
    # Try to register
    $Token = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $RegisterBody -ContentType "application/json"
} catch {
    # If registration fails (e.g. user exists), try to login
    $LoginBody = @{
        email = "admin_demo@workhub.local"
        password = "password123"
    } | ConvertTo-Json
    
    # Note: AuthController uses GET for login with request body in this implementation
    $Token = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Get -Body $LoginBody -ContentType "application/json"
}

Write-Host "   JWT Token Received: $($Token.Substring(0, 20))..."

# 3. Create a Project
Write-Host ""
Write-Host "3. Creating a Project..."
$ProjectBody = @{
    name = "Async Workflow Demo Project"
    createdBy = "admin_demo@workhub.local"
} | ConvertTo-Json

$Headers = @{ Authorization = "Bearer $Token" }
$Project = Invoke-RestMethod -Uri "$BaseUrl/projects" -Method Post -Body $ProjectBody -ContentType "application/json" -Headers $Headers
$ProjectId = $Project.id
Write-Host "   Project created with ID: $ProjectId"

# 4. Trigger Async Report Generation
Write-Host ""
Write-Host "4. Triggering Async Report Generation Job..."
$JobResponse = Invoke-RestMethod -Uri "$BaseUrl/projects/$ProjectId/generate-report" -Method Post -Headers $Headers
$JobId = $JobResponse.jobId
$Status = $JobResponse.status

Write-Host "   API Response returned immediately."
Write-Host "   Job ID: $JobId"
Write-Host "   Initial Status: $Status"

# 5. Poll for Job Completion
Write-Host ""
Write-Host "5. Polling for Job Completion (Kafka Consumer is processing...)"

$MaxRetries = 15
$Attempt = 1
$FinalStatus = "PENDING"

while ($Attempt -le $MaxRetries) {
    Start-Sleep -Seconds 1
    $Reports = Invoke-RestMethod -Uri "$BaseUrl/projects/$ProjectId/reports" -Method Get -Headers $Headers
    
    # Find the specific job in the list
    $CurrentJob = $Reports | Where-Object { $_.id -eq $JobId }
    $CurrentStatus = $CurrentJob.status
    
    Write-Host "   [Attempt $Attempt] Job Status: $CurrentStatus"
    
    if ($CurrentStatus -eq "COMPLETED") {
        $FinalStatus = "COMPLETED"
        break
    }
    
    $Attempt++
}

Write-Host ""
if ($FinalStatus -eq "COMPLETED") {
    Write-Host "✅ SUCCESS: Async Job completed successfully via Kafka messaging!" -ForegroundColor Green
} else {
    Write-Host "❌ TIMEOUT: Async Job did not complete within expected time. Check Kafka logs." -ForegroundColor Red
}
Write-Host "======================================================" -ForegroundColor Cyan
