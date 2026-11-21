package com.exantt.btrouterserver

import com.exantt.btrouterserver.HotspotDetector.InterfaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotspotDetectorTest {

    private fun activeIface(name: String) = InterfaceSnapshot(
        name = name,
        isUp = true,
        isLoopback = false,
        isPointToPoint = false,
        hasSiteLocalIpv4 = true
    )

    @Test
    fun softApNamesMatchAcrossOemVariants() {
        assertTrue(HotspotDetector.isSoftApInterfaceName("swlan0"))
        assertTrue(HotspotDetector.isSoftApInterfaceName("SWLAN0"))
        assertTrue(HotspotDetector.isSoftApInterfaceName("ap0"))
        assertTrue(HotspotDetector.isSoftApInterfaceName("ap_br_wlan2"))
        assertTrue(HotspotDetector.isSoftApInterfaceName("softap0"))
        assertTrue(HotspotDetector.isSoftApInterfaceName("sap0"))
        assertTrue(HotspotDetector.isSoftApInterfaceName("wlan1"))
    }

    @Test
    fun nonSoftApAndUpstreamInterfacesAreExcluded() {
        assertFalse(HotspotDetector.isSoftApInterfaceName("wlan0"))
        assertFalse(HotspotDetector.isSoftApInterfaceName("wlan1", upstreamWifiIfaces = setOf("wlan1")))
        assertFalse(HotspotDetector.isSoftApInterfaceName("p2p-wlan0-0"))
        assertFalse(HotspotDetector.isSoftApInterfaceName("rndis0"))
        assertFalse(HotspotDetector.isSoftApInterfaceName("usb0"))
        assertFalse(HotspotDetector.isSoftApInterfaceName("bt-pan"))
        assertFalse(HotspotDetector.isSoftApInterfaceName("tun0"))
        assertFalse(HotspotDetector.isSoftApInterfaceName("lo"))
        assertFalse(HotspotDetector.isSoftApInterfaceName(""))
        assertFalse(HotspotDetector.isSoftApInterfaceName("   "))
        assertFalse(HotspotDetector.isSoftApInterfaceName(null))
    }

    @Test
    fun detectFromInterfacesDetectsActiveSoftAp() {
        val interfaces = listOf(
            activeIface("lo").copy(isLoopback = true),
            activeIface("wlan0"),
            activeIface("swlan0")
        )
        assertEquals(1, HotspotDetector.detectFromInterfaces(interfaces))
        assertEquals(1, HotspotDetector.detectFromInterfaces(listOf(activeIface("ap_br_wlan2"))))
        assertEquals(1, HotspotDetector.detectFromInterfaces(listOf(activeIface("ap0"))))
        assertEquals(1, HotspotDetector.detectFromInterfaces(listOf(activeIface("softap0"))))
        assertEquals(1, HotspotDetector.detectFromInterfaces(listOf(activeIface("sap0"))))
        assertEquals(1, HotspotDetector.detectFromInterfaces(listOf(activeIface("wlan1"))))
    }

    @Test
    fun detectFromInterfacesIgnoresDownNoSiteLocalLoopbackP2pAndUpstream() {
        assertEquals(
            0,
            HotspotDetector.detectFromInterfaces(
                listOf(activeIface("swlan0").copy(isUp = false))
            )
        )
        assertEquals(
            0,
            HotspotDetector.detectFromInterfaces(
                listOf(activeIface("swlan0").copy(hasSiteLocalIpv4 = false))
            )
        )
        assertEquals(
            0,
            HotspotDetector.detectFromInterfaces(
                listOf(activeIface("swlan0").copy(isLoopback = true))
            )
        )
        assertEquals(
            0,
            HotspotDetector.detectFromInterfaces(
                listOf(activeIface("swlan0").copy(isPointToPoint = true))
            )
        )
        assertEquals(
            0,
            HotspotDetector.detectFromInterfaces(
                listOf(activeIface("wlan1")),
                upstreamWifiIfaces = setOf("wlan1")
            )
        )
        assertEquals(
            0,
            HotspotDetector.detectFromInterfaces(
                listOf(
                    activeIface("wlan0"),
                    activeIface("p2p-wlan0-0"),
                    activeIface("rndis0"),
                    activeIface("usb0"),
                    activeIface("bt-pan"),
                    activeIface("tun0")
                )
            )
        )
        assertEquals(-1, HotspotDetector.detectFromInterfaces(emptyList()))
    }

    @Test
    fun detectFromTetherArrayHandlesPresentEmptyNullAndOtherTethers() {
        assertEquals(1, HotspotDetector.detectFromTetherArray(listOf("swlan0")))
        assertEquals(1, HotspotDetector.detectFromTetherArray(listOf("rndis0", "ap_br_wlan2")))
        assertEquals(0, HotspotDetector.detectFromTetherArray(listOf("rndis0")))
        assertEquals(0, HotspotDetector.detectFromTetherArray(listOf("bt-pan")))
        assertEquals(0, HotspotDetector.detectFromTetherArray(emptyList()))
        assertEquals(-1, HotspotDetector.detectFromTetherArray(null))
    }
}
