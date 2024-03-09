package fr.uge.mobistorytra

import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

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

    fun searchEvents(query: String) {
        val filteredEvents = if (query.isEmpty()) {
            itemList
        } else {
            itemList.filter {
                it.label.contains(query, ignoreCase = true)
            }
        }
        _items.postValue(filteredEvents)
    }


    fun toggleFavorite(eventId: Int) {
        _items.value = _items.value?.map { event ->
            if (event.id == eventId) {
                event.copy(isFavorite = if (event.isFavorite == 0) 1 else 0)
            } else {
                event
            }
        }
        dbHelper.toggleFavorite(eventId)
    }

    fun toggleEtiquette(eventId: Int, text: String) {
        _items.value = _items.value?.map { event ->
            if (event.id == eventId) {
                event.copy(etiquette = text)
            } else {
                event
            }
        }
        dbHelper.toggleEtiquette(eventId, text)
    }


    /*@RequiresApi(Build.VERSION_CODES.O)
    fun adjustAndParseDate(dateStr: String): LocalDate {
        val adjustedDateStr = dateStr.split("-").mapIndexed { index, value ->
            if (index > 0 && value == "0") "1" else value
        }.joinToString("-")

        val formatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 1, 10, SignStyle.NORMAL)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NORMAL)
            .toFormatter()
        return LocalDate.parse(adjustedDateStr, formatter)
    }*/


    /*@RequiresApi(Build.VERSION_CODES.O)
    fun sortEvents(sortOption: MainActivity.DisplayMode) {
        val eventDateMap = extractEventDates()

        val sortedList = items.value?.sortedWith(compareByDescending<Event> { it.isFavorite }
            .thenBy {
                when (sortOption) {
                    MainActivity.DisplayMode.Populaire -> it.popularity.en
                    MainActivity.DisplayMode.Alphabetical -> if(it.label == "") "Z" else it.label.substringAfter("fr:").substringBefore("||en:").trim()
                    MainActivity.DisplayMode.Temporal -> eventDateMap[it.id]?.let { dateStr ->
                        adjustAndParseDate(dateStr)
                    } ?: LocalDate.MAX
                    else -> it.label
                }
            })
        _items.postValue(sortedList ?: emptyList())
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    fun sortEvents(
        sortOption: MainActivity.DisplayMode,
        currentLatitude: Double,
        currentLongitude: Double
    ) {
        val sortedList = items.value?.sortedWith(compareByDescending<Event> { it.isFavorite }
            .thenBy {
                when (sortOption) {
                    MainActivity.DisplayMode.Populaire -> it.popularity.en
                    MainActivity.DisplayMode.Alphabetical -> if (it.label == "") "Z" else it.label.substringAfter(
                        "fr:"
                    ).substringBefore("||en:").trim()

                    MainActivity.DisplayMode.Temporal -> it.date?.year
                    MainActivity.DisplayMode.Geo -> it.geo?.latitude?.let { it1 ->
                        it.geo.longitude.let { it2 ->
                            calculateDistance(
                                it1, it2, currentLatitude, currentLongitude
                            )
                        }
                    }

                    else -> it.label
                }
            })
        _items.postValue(sortedList ?: emptyList())
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // Approx Earth radius in KM
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
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