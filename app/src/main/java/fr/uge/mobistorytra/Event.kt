package fr.uge.mobistorytra

data class Event(
    val id: Int,
    val label: String,
    val aliases: String,
    val description: String,
    val wikipedia: String,
    val popularity: Popularity,
    val claims: List<Claim>,
    val date : Date? = null,
    val geo : Geo? = null,
    val isFavorite: Int = 0,
    val etiquette: String? = null

)

data class Date(
    val id : Int,
    val year: Int,
    val month: Int,
    val day : Int
)

data class Geo(
    val id : Int,
    val latitude: Double,
    val longitude: Double
)

data class Popularity(
    val en: Int,
    val fr: Int
)

data class Claim(
    val id : Int,
    val verboseName: String,
    val value: String,
    val item: Item?
)

data class Item(
    val label: String,
    val description: String,
    val wikipedia: String,
)