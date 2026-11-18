package com.tesfa.dualbeat.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Retrieves all valid IPv4 local addresses across active interfaces (wlan0, ap0, etc.)
     */
    fun getLocalIpAddresses(): List<String> {
        val addresses = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return addresses
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                            addresses.add(host)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return addresses
    }

    /**
     * Gets the most probable primary local IP address to display to the user.
     */
    fun getPrimaryIpAddress(): String {
        val ips = getLocalIpAddresses()
        // Prioritize Wi-Fi / Hotspot ranges (192.168.43.x for hotspot, 192.168.x.x for standard LAN)
        return ips.firstOrNull { it.startsWith("192.168.43.") }
            ?: ips.firstOrNull { it.startsWith("192.168.") }
            ?: ips.firstOrNull { it.startsWith("10.") }
            ?: ips.firstOrNull { it.startsWith("172.") }
            ?: ips.firstOrNull()
            ?: "Offline (No Wi-Fi/Hotspot)"
    }

    /**
     * Resolves all subnet-directed broadcast addresses and fallbacks for reliable UDP discovery.
     */
    fun getBroadcastAddresses(): Set<InetAddress> {
        val broadcasts = mutableSetOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                for (intf in interfaces) {
                    if (!intf.isUp || intf.isLoopback) continue
                    for (intfAddr in intf.interfaceAddresses) {
                        val broadcast = intfAddr.broadcast
                        if (broadcast != null && broadcast is Inet4Address) {
                            broadcasts.add(broadcast)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Add standard subnet directed broadcast fallbacks
        val fallbacks = listOf("192.168.43.255", "192.168.49.255", "192.168.1.255", "192.168.0.255", "255.255.255.255")
        for (fb in fallbacks) {
            try {
                broadcasts.add(InetAddress.getByName(fb))
            } catch (e: Exception) {
                // Ignore invalid resolution
            }
        }
        return broadcasts
    }

    /**
     * If this device is a hotspot client, its default gateway is typically 192.168.43.1
     */
    fun getHotspotGatewayAddress(): InetAddress? {
        return try {
            InetAddress.getByName("192.168.43.1")
        } catch (e: Exception) {
            null
        }
    }
}
