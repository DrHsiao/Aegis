#!/bin/bash

# Aegis Failover Watchdog Script for Linux
# Monitors Aegis health and uses iptables DNAT for failover.

AEGIS_PORT=8080
UPSTREAM_HOST="127.0.0.1"
UPSTREAM_PORT=80
HEALTH_URL="http://localhost:$AEGIS_PORT/health/live"
PROCESS_PATTERN="aegis-proxy"

echo "Starting Aegis Watchdog (Linux)..."

while true; do
    if curl -s --head --fail "$HEALTH_URL" > /dev/null; then
        # Health check passed
        # echo "$(date) - Aegis is healthy."
        :
    else
        echo "$(date) - Aegis Health Check FAILED! Initiating iptables failover..."
        
        # 1. Stop Aegis
        pkill -f "$PROCESS_PATTERN"
        
        # 2. Enable IP Forwarding (required for DNAT)
        echo 1 > /proc/sys/net/ipv4/ip_forward
        
        # 3. Apply iptables DNAT rules
        # Redirect incoming traffic on AEGIS_PORT to UPSTREAM_HOST:UPSTREAM_PORT
        iptables -t nat -A PREROUTING -p tcp --dport "$AEGIS_PORT" -j DNAT --to-destination "$UPSTREAM_HOST:$UPSTREAM_PORT"
        
        # Handle local traffic (if clients are on the same machine)
        iptables -t nat -A OUTPUT -p tcp -o lo --dport "$AEGIS_PORT" -j DNAT --to-destination "$UPSTREAM_HOST:$UPSTREAM_PORT"
        
        # Apply MASQUERADE to ensure return packets go back through this host
        iptables -t nat -A POSTROUTING -p tcp -d "$UPSTREAM_HOST" --dport "$UPSTREAM_PORT" -j MASQUERADE
        
        echo "Failover complete. iptables DNAT rules applied."
        echo "Manual restoration required: sysctl -w net.ipv4.ip_forward=0 && iptables -t nat -F"
        break
    fi
    
    sleep 5
done
