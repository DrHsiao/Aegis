# Aegis Installation Helper - vacating Port 80
# This script ensures Port 80 is available for Aegis by moving IIS Default Web Site to Port 8080.

$AppCmd = "$env:windir\system32\inetsrv\appcmd.exe"
$AegisPort = 80
$NewIisPort = 8080
$TargetWebsite = "Default Web Site"

Write-Host "--- Aegis Installation Helper ---" -ForegroundColor Cyan

# 1. Check if IIS management tool exists
if (!(Test-Path $AppCmd)) {
    Write-Host "[INFO] IIS appcmd.exe not found. Skipping IIS port migration." -ForegroundColor Gray
    exit 0
}

# 2. Check if Port 80 is currently occupied
$connection = Get-NetTCPConnection -LocalPort $AegisPort -State Listen -ErrorAction SilentlyContinue

if ($connection) {
    Write-Host "[!] Port $AegisPort is currently occupied." -ForegroundColor Yellow
    
    # 3. Check if it's the IIS "Default Web Site"
    $bindings = & $AppCmd list site "$TargetWebsite" /text:bindings 2>$null
    
    if ($bindings -like "*:80:*") {
        Write-Host "[*] Found '$TargetWebsite' bound to Port $AegisPort. Changing to Port $NewIisPort..." -ForegroundColor Cyan
        
        # Modify the binding
        try {
            # Use appcmd to update the binding for the site
            & $AppCmd set site /site.name:"$TargetWebsite" /bindings:http/*:$NewIisPort:
            
            Write-Host "[*] Binding updated. Restarting IIS service..." -ForegroundColor Yellow
            iisreset /restart
            
            Write-Host "[SUCCESS] Port $AegisPort is now vacant for Aegis." -ForegroundColor Green
            Write-Host "[INFO] IIS '$TargetWebsite' is now accessible on Port $NewIisPort." -ForegroundColor White
        }
        catch {
            Write-Host "[ERROR] Failed to update IIS bindings: $_" -ForegroundColor Red
        }
    }
    else {
        Write-Host "[WARNING] Port $AegisPort is occupied by another process ($($connection.OwningProcess)). Manual intervention required." -ForegroundColor Red
    }
}
else {
    Write-Host "[OK] Port $AegisPort is already vacant." -ForegroundColor Green
}
