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

    private val _eventoftheDay = MutableLiveData<Event>()
    val eventoftheDay: LiveData<Event> = _eventoftheDay

    fun updateEvent(newEvent: Event) {
        _eventoftheDay.value = newEvent
    }

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




    @RequiresApi(Build.VERSION_CODES.O)
    fun sortEvents(
        sortOption: MainActivity.DisplayMode,
        currentLatitude: Double,
        currentLongitude: Double
    ) {
        val sortedList = items.value?.sortedWith(compareByDescending<Event> { it.isFavorite }
            .thenBy {
                when (sortOption) {
                    MainActivity.DisplayMode.Populaire -> {
                        if (it.popularity.en == 0 && it.popularity.fr == 0) {
                            // Les deux popularités sont à 0, ces éléments vont à la fin.
                            Int.MAX_VALUE
                        } else if (it.popularity.en == 0) {
                            // Seulement la popularité en anglais est à 0, utilisez la popularité en français.
                            it.popularity.fr
                        } else if (it.popularity.fr == 0) {
                            // Seulement la popularité en français est à 0, utilisez la popularité en anglais.
                            it.popularity.en
                        } else {
                            // Aucune des popularités est à 0, utilisez la plus grande des deux.
                            minOf(it.popularity.en, it.popularity.fr)
                        }
                    }
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
                    MainActivity.DisplayMode.Etiquette -> it.etiquette ?: "\uFFFF"
                    else -> "\uFFFF"
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

