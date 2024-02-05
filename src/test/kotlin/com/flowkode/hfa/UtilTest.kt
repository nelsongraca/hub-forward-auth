package com.flowkode.hfa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UtilTest {

    @Test
    fun testInRange() {
        val util = Util("","","10.0.0.0/8",true)

        Assertions.assertTrue(util.isWhiteListed("10.0.0.0"))
        Assertions.assertTrue(util.isWhiteListed("10.255.255.255"))

        Assertions.assertFalse(util.isWhiteListed("192.168.1.1"))
        Assertions.assertFalse(util.isWhiteListed(""))
        Assertions.assertFalse(util.isWhiteListed("    "))
        Assertions.assertFalse(util.isWhiteListed("\n"))
        Assertions.assertFalse(util.isWhiteListed("\t"))
    }
}