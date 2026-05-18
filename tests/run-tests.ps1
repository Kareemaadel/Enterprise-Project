# WorkHub - Master Integration & Infrastructure Test Orchestrator
# Resolves and runs all local tests against the active Kubernetes cluster.

$ErrorActionPreference = "Stop"

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "         WORKHUB INTEGRATION TEST SUITE               " -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
$TENANT_FILE = "$SCRIPT_DIR/../.tenant_ids.txt"

# 1. Reboot application & fetch fresh Tenant ID
Write-Host "[1/4] Restarting Kubernetes pods to ensure a fresh database and new Tenant IDs..." -ForegroundColor Yellow

try {
    Write-Host "   [REBOOT] Triggering pod deletion in 'workhub' namespace..." -ForegroundColor Yellow
    $null = kubectl delete pods -n workhub --all
    Write-Host "   [OK] All pods deleted successfully! Waiting for them to recreate..." -ForegroundColor Green
} catch {
    Write-Host "   [FAIL] Error: Failed to restart Kubernetes pods via kubectl!" -ForegroundColor Red
    exit 1
}

Write-Host "   [WAIT] Waiting for application to boot and become healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 5 # Give pods a moment to start terminating/recreating

$maxAttempts = 80
$attempt = 1
$healthy = $false

while ($attempt -le $maxAttempts) {
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost/health" -TimeoutSec 3
        $trimmedResp = $resp.Trim()
        if ($trimmedResp -eq "I'm alive :D" -or $trimmedResp -eq "hello this is a new version") {
            $healthy = $true
            break
        }
    } catch {
        # Print actual connection error in dark gray for transparent monitoring
        Write-Host "      [DEBUG] $($_.Exception.Message)" -ForegroundColor DarkGray
    }
    Write-Host "      [Attempt $attempt/$maxAttempts] Still waiting for app to boot..." -ForegroundColor Gray
    Start-Sleep -Seconds 3
    $attempt++
}

if (-not $healthy) {
    Write-Host "   [FAIL] Error: Application failed to boot or become healthy in time!" -ForegroundColor Red
    exit 1
}
Write-Host "   [OK] Application is healthy!" -ForegroundColor Green

Write-Host "   [INFO] Fetching fresh tenant ID from the running pod..." -ForegroundColor Yellow
try {
    # Fetch tenant ID from one of the running stable pods
    $tenant_id = (kubectl exec -n workhub deployment/workhub-app-stable -- cat .tenant_ids.txt).Trim()
    [System.IO.File]::WriteAllText($TENANT_FILE, $tenant_id)
    Write-Host "   [OK] Saved fresh Tenant ID: $tenant_id to $TENANT_FILE" -ForegroundColor Green
} catch {
    Write-Host "   [FAIL] Error: Failed to fetch fresh tenant ID from Kubernetes pod!" -ForegroundColor Red
    exit 1
}

# 2. Check cluster service reachability
Write-Host "[2/4] Verifying LoadBalancer endpoint reachability..." -ForegroundColor Yellow
try {
    $resp = Invoke-RestMethod -Uri "http://localhost/health" -TimeoutSec 5
    Write-Host "   [OK] LoadBalancer online! App Health Status: $resp" -ForegroundColor Green
} catch {
    Write-Host "   [FAIL] Error: Could not connect to http://localhost/health" -ForegroundColor Red
    Write-Host "   Please check if your Kubernetes services are active and running." -ForegroundColor Red
    exit 1
}

$asyncStatus = "FAILED"
$concurrencyStatus = "FAILED"

# 3. Run Async Job integration test
Write-Host ""
Write-Host "[3/4] Running Kafka Asynchronous Messaging Job Test..." -ForegroundColor Yellow
try {
    & "$SCRIPT_DIR/demo-async-job.ps1"
    $asyncStatus = "PASSED"
} catch {
    Write-Host "   [FAIL] Async Job Test Failed!" -ForegroundColor Red
}

# 4. Run Concurrency & Optimistic Lock test
Write-Host ""
Write-Host "[4/4] Running Database Concurrency and Optimistic Lock Test..." -ForegroundColor Yellow
try {
    # Run the python concurrency script
    $pyOutput = python "$SCRIPT_DIR/test_concurrency.py"
    Write-Output $pyOutput
    if ($pyOutput -match "Finished! Status Code: 409") {
        $concurrencyStatus = "PASSED"
    } else {
        $concurrencyStatus = "FAILED (No Conflict Detected)"
    }
} catch {
    Write-Host "   [FAIL] Concurrency Test Failed!" -ForegroundColor Red
}

# 5. Output Report Summary Dashboard
Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "               FINAL TEST RESULTS                     " -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

if ($asyncStatus -eq "PASSED") {
    Write-Host "  Kafka Async Job Test:         [ PASSED ]" -ForegroundColor Green
} else {
    Write-Host "  Kafka Async Job Test:         [ FAILED ]" -ForegroundColor Red
}

if ($concurrencyStatus -eq "PASSED") {
    Write-Host "  Db Optimistic Lock Test:      [ PASSED ]" -ForegroundColor Green
} else {
    Write-Host "  Db Optimistic Lock Test:      [ FAILED ]" -ForegroundColor Red
}

Write-Host "======================================================" -ForegroundColor Cyan
if ($asyncStatus -eq "PASSED" -and $concurrencyStatus -eq "PASSED") {
    Write-Host "  Status: ALL INTEGRATION & INFRASTRUCTURE TESTS PASSED! SUCCESS" -ForegroundColor Green
} else {
    Write-Host "  Status: SOME TESTS FAILED. CHECK LOGS ABOVE." -ForegroundColor Red
}
Write-Host "======================================================" -ForegroundColor Cyan
