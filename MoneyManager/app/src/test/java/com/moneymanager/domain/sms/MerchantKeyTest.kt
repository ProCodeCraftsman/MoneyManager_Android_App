package com.moneymanager.domain.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantKeyTest {

    @Test
    fun `terminal ids and city suffixes are dropped`() {
        assertEquals("swiggy", MerchantKey.canonical("POS 123456 SWIGGY BLR"))
        assertEquals("swiggy", MerchantKey.canonical("TID 0012 Swiggy Bangalore"))
        assertEquals("keralavisitorshotel", MerchantKey.canonical("Kerala Visitors Hotel Mumbai"))
    }

    @Test
    fun `a terminal word without an id is part of the name`() {
        assertEquals("posswiggy", MerchantKey.canonical("POS SWIGGY"))
    }

    @Test
    fun `UPI qr ids are dropped and the payee name is kept`() {
        assertEquals("", MerchantKey.canonical("paytmqr5ojiw8@paytm"))
        assertEquals("", MerchantKey.canonical("q12345678@ybl"))
        assertEquals("", MerchantKey.canonical("7308080808@okhdfc"))
        assertEquals("greenmart", MerchantKey.canonical("greenmart@icici"))
        assertEquals("greenmart", MerchantKey.canonical("GREENMART paytmqr5ojiw8@paytm"))
    }

    @Test
    fun `store refs at the end go, a lone city stays`() {
        assertEquals("cafe", MerchantKey.canonical("CAFE #204"))
        assertEquals("delhi", MerchantKey.canonical("DELHI"))
    }

    @Test
    fun `truncated bank names keep their prefix`() {
        assertEquals("keralavisi", MerchantKey.canonical("KERALA VISI"))
    }

    @Test
    fun `key is capped at 40 characters`() {
        assertEquals(40, MerchantKey.canonical("A".repeat(60)).length)
    }

    @Test
    fun `legacy key is the old normaliser`() {
        assertEquals("pos123456swiggyblr", MerchantKey.legacy("POS 123456 SWIGGY BLR"))
        assertEquals("swiggy", MerchantKey.legacy("Swiggy"))
    }
}
