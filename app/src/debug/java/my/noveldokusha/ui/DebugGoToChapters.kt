package my.noveldokusha.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.R
import my.noveldokusha.features.chapterslist.ChaptersActivity
import my.noveldokusha.tooling.local_database.BookMetadata

class DebugGoToChapters : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_middleware_test)

        startActivity(
            ChaptersActivity.IntentData(
                this,
                bookMetadata = BookMetadata(
                    title = "Orc Hero Story - Discovery Chronicles",
                    url = "https://www.novelhall.com/orc-hero-story-discovery-chronicles-orc-eroica-21023/",
                    coverImageUrl = "https://www.novelhall.com/upload/images/article/20210724/orc-hero-story-discovery-chronicles-orc-eroica.jpg",
                    description = "Empty"
                )
            )
        )
    }
}

