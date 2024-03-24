package my.noveldokusha.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.features.chaptersList.ChaptersActivity

class DebugGoToChapters : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_middleware_test)

        startActivity(
            ChaptersActivity.IntentData(
                this,
                bookMetadata = BookMetadata(
                    title = "Star Odyssey",
                    url = "https://bestlightnovel.com/novel_888169055",
                    coverImageUrl = "https://avatar.novelonlinefree.com/avatar_novels/37752-1672803704.jpg",
                    description = "Join Lu Yin on an epic journey across the Universe, pursuing the truth and tragedy of his past. This is a world of science fantasy where the older generations step back and allow the young to take charge of affairs. Heart-wrenching separations, terrifying situations, all with comic relief that will leave you coming back for more. This is a world where the other characters actually matter, and are revisited frequently as their own lives unfold. Dotting Lu Yin’s path are monumental feats of kingdom-building and treacherous political situations where he must tread carefully if he wants to get to the truth of his history."
                )
            )
        )
    }
}

