package com.exantt.btrouterserver

/**
 * Pure Kotlin helper for identifying active Wi-Fi Hotspot (SoftAP) state
 * from network interface snapshots and tethering broadcast extras without Android dependencies.
 */
object HotspotDetector {

    data class InterfaceSnapshot(
        val name: String,
        val isUp: Boolean,
        val isLoopback: Boolean,
        val isPointToPoint: Boolean,
        val hasSiteLocalIpv4: Boolean
    )

    private val excludedPrefixes = listOf(
        "p2p",
        "rndis",
        "usb",
        "ncm",
        "bt-pan",
        "bnep",
        "tun",
        "tap",
        "ppp",
        "v4-",
        "clat",
        "rmnet",
        "ccmni",
        "dummy",
        "sit",
        "ip6tnl"
    )

    private val softApRegex = Regex(
        "^(swlan\\d*.*|ap\\d+.*|ap_br_.*|softap\\d*.*|sap\\d+.*|wlan[1-9]\\d*)$"
    )

    fun isSoftApInterfaceName(
        rawName: String?,
        upstreamWifiIfaces: Set<String> = emptySet()
    ): Boolean {
        val name = rawName?.trim()?.lowercase() ?: return false
        if (name.isEmpty()) return false
        if (name == "lo" || name == "wlan0") return false
        if (upstreamWifiIfaces.any { it.trim().lowercase() == name }) return false
        if (excludedPrefixes.any { name.startsWith(it) }) return false
        return softApRegex.matches(name)
    }

    /**
     * Returns 1 if an active SoftAP interface with site-local IPv4 is found,
     * 0 if interfaces were inspected and none match, or -1 if no interface data is available.
     */
    fun detectFromInterfaces(
        interfaces: Iterable<InterfaceSnapshot>,
        upstreamWifiIfaces: Set<String> = emptySet()
    ): Int {
        var inspectedAny = false
        for (iface in interfaces) {
            inspectedAny = true
            if (iface.isUp &&
                !iface.isLoopback &&
                !iface.isPointToPoint &&
                iface.hasSiteLocalIpv4 &&
                isSoftApInterfaceName(iface.name, upstreamWifiIfaces)
            ) {
                return 1
            }
        }
        return if (inspectedAny) 0 else -1
    }

    /**
     * Returns 1 if [tetheredInterfaces] contains a SoftAP interface name,
     * 0 if the list is present and contains no SoftAP interface,
     * or -1 if [tetheredInterfaces] is null (extra missing).
     */
    fun detectFromTetherArray(tetheredInterfaces: Collection<String>?): Int {
        if (tetheredInterfaces == null) return -1
        return if (tetheredInterfaces.any { isSoftApInterfaceName(it) }) 1 else 0
    }
}
