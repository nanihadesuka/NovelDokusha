package my.noveldokusha.ui.screens.globalSourceSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String
import javax.inject.Inject

@AndroidEntryPoint
class GlobalSourceSearchActivity : ComponentActivity() {
    class IntentData : Intent, GlobalSourceSearchStateBundle {
        override var input by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, input: String) : super(
            ctx,
            GlobalSourceSearchActivity::class.java
        ) {
            this.input = input
        }
    }

    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel by viewModels<GlobalSourceSearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Theme(appPreferences = appPreferences) {
                Column {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(MaterialTheme.colors.surface)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .padding(top = 16.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = getString(R.string.global_search),
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(text = viewModel.input, style = MaterialTheme.typography.subtitle1)
                    }

                    GlobalSourceSearchView(
                        listSources = viewModel.list,
                        onBookClick = ::openBookChaptersPage
                    )
                }
            }
        }
    }

    fun openBookChaptersPage(book: BookMetadata) {
        ChaptersActivity.IntentData(
            this@GlobalSourceSearchActivity,
            bookMetadata = BookMetadata(title = book.title, url = book.url)
        ).let(::startActivity)
    }
}
