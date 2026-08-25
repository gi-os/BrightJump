package com.gios.brightjump

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoringTest {

    private val self = "com.gios.brightjump"

    private fun c(pkg: String, label: String, cls: String = "$pkg.MainActivity") =
        Candidate(pkg, cls, label)

    @Test
    fun `exact chats label beats a partial match`() {
        assertEquals(100, Scoring.labelScore("Chats"))
        assertEquals(100, Scoring.labelScore("  chats "))
        assertEquals(30, Scoring.labelScore("Chats (beta)"))
    }

    @Test
    fun `an unrelated label scores nothing`() {
        assertEquals(0, Scoring.labelScore("Camera"))
        assertEquals(0, Scoring.labelScore(""))
    }

    @Test
    fun `lightos wins a tie on label`() {
        val best = Scoring.best(
            listOf(
                c("com.google.android.apps.messaging", "Messages"),
                c("com.lightos", "Messages"),
            ),
            self,
        )
        assertEquals("com.lightos", best?.packageName)
    }

    @Test
    fun `our own apps are never the answer`() {
        // The one that matters. BrightChat is labelled "Chat", so on label alone it outranks a
        // LightOS tool called "Messages" — and the icon would silently open the wrong app.
        val best = Scoring.best(
            listOf(
                c("com.gios.lightchat", "Chat"),
                c("com.lightos", "Messages"),
            ),
            self,
        )
        assertEquals("com.lightos", best?.packageName)
    }

    @Test
    fun `the app never resolves to itself`() {
        assertNull(Scoring.best(listOf(c(self, "Chats")), self))
    }

    @Test
    fun `nothing plausible means null, not a guess`() {
        val best = Scoring.best(
            listOf(c("com.lightos", "Camera"), c("com.example.notes", "Notes")),
            self,
        )
        assertNull(best)
    }

    @Test
    fun `ties break the same way every time`() {
        val a = c("com.aaa.msg", "Messages")
        val b = c("com.bbb.msg", "Messages")
        assertEquals(a, Scoring.best(listOf(b, a), self))
        assertEquals(a, Scoring.best(listOf(a, b), self))
    }
}
