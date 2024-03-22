package fr.uge.mobistorytra

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException


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

data class Claim(
    val id : Int,
    val verboseName: String,
    val value: String,
    val item: Item?
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



data class Item(
    val label: String,
    val description: String,
    val wikipedia: String,
)

fun processJsonFile(fileName: String, context: Context, dbHelper: EventsDatabaseHelper) {
    val gson = Gson()
    try {
        context.openFileInput(fileName).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                try {
                    val jsonElement = gson.fromJson(line, JsonElement::class.java)
                    val event = parseJsonElement(jsonElement)
                    event?.let { dbHelper.insertItem(it) }
                } catch (e: JsonSyntaxException) {
                    println("Erreur lors du parsing de la ligne : ${e.message}")
                }
            }
        }
    } catch (e: Exception) {
        println("Erreur globale lors du parsing : ${e.message}")
    }
}








fun parseJsonElement(jsonElement: JsonElement): Event? {
    val jsonObject = jsonElement.asJsonObject

    // Utilisez elvis operator pour fournir des valeurs par défaut en cas de champs manquants
    val id = jsonObject["id"]?.asInt ?: return null
    val label = jsonObject["label"]?.asString ?: ""
    val aliases = jsonObject["aliases"]?.asString ?: ""
    val description = jsonObject["description"]?.asString ?: ""
    val wikipedia = jsonObject["wikipedia"]?.asString ?: ""

    // Gestion sécurisée de la popularité
    val popularityObject = jsonObject["popularity"]?.asJsonObject
    val popularity = Popularity(
        en = popularityObject?.get("en")?.asInt ?: 0,
        fr = popularityObject?.get("fr")?.asInt ?: 0
    )

    val claimsList = jsonObject["claims"]?.asJsonArray?.mapNotNull { claimElement ->
        val claimObject = claimElement.asJsonObject
        val item = claimObject["item"]?.takeIf { it.isJsonObject }?.let { parseItem(it.asJsonObject) }
        Claim(
            id = 0,
            verboseName = claimObject["verboseName"]?.asString ?: "",
            value = claimObject["value"]?.asString ?: "",
            item = item
        )
    } ?: emptyList()

    return Event(id, label, aliases, description, wikipedia, popularity, claimsList)
}



fun parseItem(jsonObject: JsonObject): Item {
    return Item(
        label = jsonObject["label"]?.asString ?: "",
        description = jsonObject["description"]?.asString ?: "",
        wikipedia = jsonObject["wikipedia"]?.asString ?: "",
    )
}







