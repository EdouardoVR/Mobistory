package fr.uge.mobistorytra

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.annotation.RequiresApi
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.Manifest
import java.time.LocalDate
import java.util.Collections
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {

    private val context = this

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = EventsDatabaseHelper(this)
        if(!dbHelper.hasEventsData()){
            val downloader = EventsDownloader(
                this,
                "https://igm.univ-mlv.fr/~chilowi/data/historyevents/events.txt.gz"
            )
            downloader.downloadAndDecompressFile()
            processJsonFile("events.txt", context, dbHelper)
        }
        val viewModel = ViewModelProvider(this, ViewModelFactory(dbHelper))[ItemsViewModel::class.java]
    }

    enum class DisplayMode {
        Alphabetical,
        Temporal,
        Populaire,
        Geo,
    }

    @Composable
    fun SortDropdownMenu(selectedOption: DisplayMode, onOptionSelected: (DisplayMode) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val options = listOf<DisplayMode>(DisplayMode.Populaire, DisplayMode.Temporal, DisplayMode.Alphabetical, DisplayMode.Geo)

        Box(Modifier.wrapContentSize(Alignment.TopStart)) {
            Text(
                "$selectedOption", modifier = Modifier
                    .clickable(onClick = { expanded = true })
                    .padding(16.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text("$option") }, onClick = {
                        onOptionSelected(option)
                        expanded = false
                    })
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun findEventsByDate(events: List<Event>): Event? {
        val today = LocalDate.now()
        val month = today.monthValue
        val dayOfMonth = today.dayOfMonth
        for (event in events.shuffled()){
            if(event.date?.month == month && event.date.day == dayOfMonth){
                return event
            }
        }
        return null
    }

}