package my.noveldokusha.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.R
import my.noveldokusha.ui.screens.reader.ReaderActivity

class DebugGoToReader : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_middleware_test)

        startActivity(
            ReaderActivity.IntentData(
                this,
                bookUrl = "https://bestlightnovel.com/novel_888169055",
                chapterUrl = "https://bestlightnovel.com/novel_888169055/chapter_2"
            )
        )
    }
}