package app.cleanlaunch.tv

import app.cleanlaunch.tv.data.NowPlayingPick
import app.cleanlaunch.tv.data.NowPlayingPick.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingTest {
    @Test
    fun nothingLiveShowsNothing() {
        assertNull(NowPlayingPick.choose(emptyList()))
        assertNull(NowPlayingPick.choose(listOf(Candidate(live = false, playing = false, lastPlayAt = 5))))
    }

    @Test
    fun playingBeatsPaused() {
        val list = listOf(
            Candidate(live = true, playing = false, lastPlayAt = 900),
            Candidate(live = true, playing = true, lastPlayAt = 100),
        )
        assertEquals(1, NowPlayingPick.choose(list))
    }

    @Test
    fun mostRecentlyStartedPlayerWins() {
        val list = listOf(
            Candidate(live = true, playing = true, lastPlayAt = 100),
            Candidate(live = true, playing = true, lastPlayAt = 400),
            Candidate(live = true, playing = true, lastPlayAt = 200),
        )
        assertEquals(1, NowPlayingPick.choose(list))
    }

    @Test
    fun whenNothingPlaysTheLatestPausedOneStays() {
        val list = listOf(
            Candidate(live = true, playing = false, lastPlayAt = 100),
            Candidate(live = true, playing = false, lastPlayAt = 300),
            Candidate(live = false, playing = false, lastPlayAt = 900),
        )
        assertEquals(1, NowPlayingPick.choose(list))
    }

    @Test
    fun tiesKeepTheSystemOrder() {
        val list = listOf(
            Candidate(live = true, playing = false, lastPlayAt = 0),
            Candidate(live = true, playing = false, lastPlayAt = 0),
        )
        assertEquals(0, NowPlayingPick.choose(list))
    }

    @Test
    fun positionAdvancesOnlyWhilePlayingAndStopsAtTheEnd() {
        assertEquals(15_000L, NowPlayingPick.position(10_000, 1_000, 6_000, 1f, true, 200_000))
        assertEquals(10_000L, NowPlayingPick.position(10_000, 1_000, 6_000, 1f, false, 200_000))
        assertEquals(20_000L, NowPlayingPick.position(10_000, 1_000, 6_000, 2f, true, 200_000))
        assertEquals(200_000L, NowPlayingPick.position(199_000, 1_000, 60_000, 1f, true, 200_000))
    }
}
