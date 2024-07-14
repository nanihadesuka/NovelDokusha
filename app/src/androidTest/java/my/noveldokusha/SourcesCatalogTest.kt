package my.noveldokusha

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.noveldokusha.features.main.MainActivity
import my.noveldoksuha.coreui.AppTestTags
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourcesCatalogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val recordScreenRule = RecordScreenRule()

    @Test
    fun source__novel_updates() {
        checkSourceCanBeOpened("Novel Updates")
    }

    @Test
    fun source__1stkissnovel() {
        checkSourceCanBeOpened("1stKissNovel")
    }

    @Test
    fun source__royalroad() {
        checkSourceCanBeOpened("Royal Road")
    }

    @Test
    fun source__bestlightnovel() {
        checkSourceCanBeOpened("BestLightNovel")
    }

    @Test
    fun source__box_novel() {
        checkSourceCanBeOpened("Box Novel")
    }

    @Test
    fun source__korean_novels_mtl() {
        checkSourceCanBeOpened("Korean Novels MTL")
    }

    @Test
    fun source__light_novel_translations() {
        checkSourceCanBeOpened("Light Novel Translations")
    }

    @Test
    fun source__light_novel_world() {
        checkSourceCanBeOpened("Light Novel World")
    }

    @Test
    fun source__mtlnovel() {
        checkSourceCanBeOpened("MTLNovel")
    }

    @Test
    fun source__novelhall() {
        checkSourceCanBeOpened("NovelHall")
    }

    @Test
    fun source__read_light_novel() {
        checkSourceCanBeOpened("Read Light Novel")
    }

    @Test
    fun source__read_novel_full() {
        checkSourceCanBeOpened("Read Novel Full")
    }

    @Test
    @Ignore("Behind discord now")
    fun source__saikai() {
        checkSourceCanBeOpened("Saikai")
    }

    @Test
    fun source__wuxia() {
        checkSourceCanBeOpened("Wuxia")
    }

    @Test
    fun source__wuxia_world() {
        checkSourceCanBeOpened("Wuxia World")
    }

    @OptIn(ExperimentalTestApi::class)
    private fun checkSourceCanBeOpened(nameOfSource: String) {

        composeTestRule
            .onNodeWithText("Finder")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(nameOfSource))
            .assertExists()
            .performClick()

        composeTestRule.waitUntilDoesNotExist(
            matcher = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
            timeoutMillis = 30_000
        )

        // TODO: check the cloudfare null pointer error
        composeTestRule.waitUntilAtLeastOneExists(
            matcher = hasTestTag(AppTestTags.BOOK_IMAGE_BUTTON_VIEW),
            timeoutMillis = 5_000
        )
    }
}

