package app.cleanlaunch.tv

import app.cleanlaunch.tv.sentence.LineRole
import app.cleanlaunch.tv.sentence.PageFrame
import app.cleanlaunch.tv.sentence.PageLine
import app.cleanlaunch.tv.sentence.PaperColumns
import app.cleanlaunch.tv.sentence.StoryFit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryFitTest {
    @Test
    fun keepsThePreferredSizeWhenTheColumnHasRoom() {
        assertEquals(44, StoryFit.choose(800) { 400 })
    }

    @Test
    fun stepsDownUntilTheMeasuredHeightFits() {
        val size = StoryFit.choose(500) { english -> if (english >= 30) 640 else 480 }
        assertEquals(28, size)
    }

    @Test
    fun stopsAtTheMinimumInsteadOfASizeThatStillOverflows() {
        assertEquals(16, StoryFit.choose(100) { 900 })
    }

    @Test
    fun chineseStaysCloseToTheEnglishSize() {
        assertEquals(28, StoryFit.chinesePx(32))
        assertTrue(StoryFit.leading(32) >= 32 + 6)
        assertTrue(StoryFit.availableDesignPx() > 500)
    }

    @Test
    fun theColumnsShareOneMarginAndGutter() {
        assertEquals(96, PageFrame.marginX)
        assertEquals(54, PageFrame.marginY)
        assertEquals(1040, PageFrame.leftW)
        assertEquals(64, PageFrame.columnGap)
        assertEquals(624, PageFrame.indexW)
        assertEquals(PageFrame.marginX + PageFrame.leftW + PageFrame.columnGap, PageFrame.indexX)
        assertEquals(PageFrame.screenW - PageFrame.marginX, PageFrame.indexX + PageFrame.indexW)
        assertEquals(PageFrame.screenH - PageFrame.marginY, PageFrame.sourcesRow + PageFrame.sourceChipH)
        assertEquals(PageFrame.bodyTop, StoryFit.storyTop)
        assertEquals(488, PageFrame.leadW)
        assertEquals(488, PageFrame.railW)
        assertEquals(PageFrame.leftW, PageFrame.leadW + PageFrame.columnGap + PageFrame.railW)
        assertEquals(PageFrame.marginX + PageFrame.leadW + PageFrame.columnGap, PageFrame.railX)
        assertEquals(PageFrame.indexX, PageFrame.railX + PageFrame.railW + PageFrame.columnGap)
        assertEquals(PageFrame.leadW, StoryFit.leadWidth)
        assertEquals(PageFrame.railW, StoryFit.railWidth)
        assertEquals(PageFrame.leftW, StoryFit.sourcesSpan)
    }

    @Test
    fun weatherAndTheDateStayLeftOfTheWire() {
        val lines = listOf(
            PageLine("It's 21° and raining.", "現在21°，下雨。", role = LineRole.WEATHER),
            PageLine("AAPL  100.00  +1.00%", "", role = LineRole.QUOTE),
            PageLine("A reef survey", "礁調查", role = LineRole.NEWS),
            PageLine("Talks stall", "", role = LineRole.NEWS),
            PageLine("A quieter chip", "更安靜的晶片", role = LineRole.NEWS),
            PageLine("One more desk", "", role = LineRole.NEWS),
            PageLine("And a fifth", "", role = LineRole.NEWS),
            PageLine("Dinner is at seven", "七點吃飯", role = LineRole.NOTE),
        )
        val date = PageLine("Tuesday 20 October", "10月20日 星期二", role = LineRole.DATE)
        val lead = PaperColumns.lead(lines, date)
        val rail = PaperColumns.rail(lines)
        assertEquals(listOf("It's 21° and raining.", "Tuesday 20 October", "Dinner is at seven"), lead.map { it.english })
        assertEquals("AAPL  100.00  +1.00%", rail.first().english)
        assertEquals(3, rail.size)
        assertEquals(listOf("A reef survey", "Talks stall"), rail.drop(1).map { it.english })
        assertTrue(rail.drop(1).all { it.role == LineRole.NEWS })
        assertTrue(rail.none { it.english == "A quieter chip" })
    }
}
