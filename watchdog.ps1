# Aegis Failover Watchdog Script
# This script monitors the Aegis proxy health and performs a failover to netsh portproxy if it fails.

$AegisPort = 8080
$UpstreamHost = "127.0.0.1"
$UpstreamPort = 80
$HealthUrl = "http://localhost:$AegPort/health/live"
$AegisProcessName = "java" # Assuming it's running via java.exe. Adjust if using a specific service name.

Write-Host "Starting Aegis Watchdog..." -ForegroundColor Cyan

while ($true) {
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -Method Get -TimeoutSec 2 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            # Health check passed, do nothing
            # Write-Host "[$(Get-Date)] Aegis is healthy." -ForegroundColor Green
        } else {
            throw "Status code: $($response.StatusCode)"
        }
    }
    catch {
        Write-Host "[$(Get-Date)] Aegis Health Check FAILED! Initiating failover..." -ForegroundColor Red
        
        # 1. Stop Aegis Process / Service
        # Note: If running as a service, use Stop-Service "Aegis"
        Get-Process $AegisProcessName -ErrorAction SilentlyContinue | Stop-Process -Force
        
        # 2. Configure netsh interface portproxy to redirect traffic directly to upstream
        # This assumes Aegis was listening on $AegisPort and we want to tunnel it to $UpstreamPort
        Write-Host "Configuring netsh portproxy: ListenPort $AegisPort -> ConnectPort $UpstreamPort" -ForegroundColor Yellow
        netsh interface portproxy add v4tov4 listenport=$AegisPort listenaddress=0.0.0.0 connectport=$UpstreamPort connectaddress=$UpstreamHost
        
        Write-Host "Failover complete. Traffic is now routed directly to Upstream." -ForegroundColor Green
        Write-Host "Watchdog stopping. Manual intervention required to restore Aegis." -ForegroundColor White
        break
    }
    
    Start-Sleep -Seconds 5
}
