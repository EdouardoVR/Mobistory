package fr.uge.mobistorytra

import android.os.Build
import android.view.Display
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

class ItemsViewModel(private val dbHelper: EventsDatabaseHelper) : ViewModel() {
    private val _items = MutableLiveData<List<Event>>()
    val items: LiveData<List<Event>> = _items
    private var itemList = dbHelper.getAllItems()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            itemList = dbHelper.getAllItems().toMutableList()
            extractEventDates()
            extractEventGeo()
            val sortedList = itemList.sortedWith(compareByDescending<Event> { it.isFavorite })
            _items.postValue(sortedList)
        }
    }

    private fun extractEventDates() {
        if (!dbHelper.hasValidDateData()) {
            val eventDateMap = mutableMapOf<Int, Date>()
            itemList.forEach { event ->
                val firstDateClaim = event.claims.firstOrNull { it.value.startsWith("date:") }
                firstDateClaim?.let {
                    val dateString = it.value.removePrefix("date:")
                    val year = extractYearFromDate(dateString)
                    if (year != null) {
                        val (yearStr, month, day) = dateString.split("-")
                            .map { it.toIntOrNull() ?: 0 }
                        val date = Date(event.id, year, month, day)
                        eventDateMap[event.id] = date
                    }
                }
            }
            dbHelper.addDateAllEvent(eventDateMap)
        }
    }

    private fun extractEventGeo() {
        if (!dbHelper.hasGeoData()) {
            val eventGeoMap = mutableMapOf<Int, Geo>()

            itemList.forEach { event ->
                val geoClaim = event.claims.firstOrNull { it.value.startsWith("geo:") }
                geoClaim?.let {
                    val geoString = it.value.removePrefix("geo:")
                    val (latitude, longitude) = geoString.split(",")
                        .map { coord -> coord.toDoubleOrNull() ?: 0.0 }
                    if (latitude != 0.0 && longitude != 0.0) { // Assurez-vous que les coordonnées sont valides
                        val geo = Geo(event.id, latitude, longitude)
                        eventGeoMap[event.id] = geo
                    }
                }
            }

            if (eventGeoMap.isNotEmpty()) {
                dbHelper.addGeoAllEvent(eventGeoMap)
            }
        }
    }

    fun extractYearFromDate(date: String): Int? {
        val isNegativeYear = date.startsWith("-")

        val yearString = if (isNegativeYear) {
            date.substring(1).split("-").firstOrNull()?.let { "-$it" }
        } else {
            date.split("-").firstOrNull()
        }

        return yearString?.toIntOrNull()
    }
}