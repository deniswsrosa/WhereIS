package com.acme.clara.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpriteSlugTest {
    @Test fun snakeCollapsesPunctuation() {
        assertEquals("sport_club", snake("Sport Club"))
        assertEquals("radio_station", snake("Radio Station"))
    }

    @Test fun snakeStripsAccents() {
        assertEquals("restaurant_cafe", snake("Restaurant / Café"))
        assertEquals("cote_d_ivoire", snake("Côte d'Ivoire"))
    }

    @Test fun cafeVenueResolvesToShippedSprite() {
        val sprite = File("src/main/assets/sprites/venues/venue_${snake("Restaurant / Café")}.png")
        assertTrue("missing ${sprite.path}", sprite.exists())
    }
}
