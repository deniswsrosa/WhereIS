package com.acme.clara

import com.acme.clara.game.SoundCue
import com.acme.clara.ui.captionFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-logic coverage for the caption mapping used by Options ▸ Captions. */
class AccessibilityLogicTest {

    @Test fun everyCueHasACaptionExceptTheNeutralBriefing() {
        for (cue in SoundCue.values()) {
            if (cue == SoundCue.BRIEFING) assertNull(captionFor(cue))
            else assertNotNull("expected a caption for $cue", captionFor(cue))
        }
    }

    @Test fun keyCuesReadTheRightWords() {
        assertEquals("Warrant issued", captionFor(SoundCue.WARRANT))
        assertEquals("Case solved", captionFor(SoundCue.WIN))
        assertEquals("The thief got away", captionFor(SoundCue.WRONG_ARREST))
        assertEquals("Out of time", captionFor(SoundCue.OUT_OF_TIME))
    }
}
