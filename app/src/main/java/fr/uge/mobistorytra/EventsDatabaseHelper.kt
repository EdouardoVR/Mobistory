package fr.uge.mobistorytra

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EventsDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 21
        private const val DATABASE_NAME = "MyDatabase.db"

        private const val TABLE_EVENTS = "events"
        private const val COLUMN_ID = "id"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_ALIASES = "aliases"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_WIKIPEDIA = "wikipedia"
        private const val COLUMN_POPU_FR = "popularity_fr"
        private const val COLUMN_POPU_EN = "popularity_en"
        private const val COLUMN_FAVORITE = "favorite"
        private const val COLUMN_ETIQUETTE = "etiquette"

        private const val TABLE_GEO = "geo"
        private const val COLUMN_GEO_EVENTID = "geo_id"
        private const val COLUMN_GEO_ID = "id"
        private const val COLUMN_LATITUDE= "latitude"
        private const val COLUMN_LONGITUDE= "longitude"



        private const val TABLE_DATE = "date"
        private const val COLUMN_DATE_EVENTID = "date_id"
        private const val COLUMN_DATE_ID = "id"
        private const val COLUMN_YEAR= "year"
        private const val COLUMN_MONTH = "month"
        private const val COLUMN_DAY = "day"


        private const val COLUMN_CLAIMS_EVENTID = "events_id"
        private const val COLUMN_CLAIMS_VERBOSE = "verboseName"
        private const val COLUMN_CLAIMS_VALUE = "value"
        private const val COLUMN_CLAIMS_ID = "id"


        private const val TABLE_CLAIMS = "claims"
        private const val COLUMN_ITEM_LABEL = "Itemlabel"
        private const val COLUMN_ITEM_DESCRITPION = "Itemdescription"
        private const val COLUMN_ITEM_WIKI = "Itemwikipedia"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createEventsTableStatement = """
        CREATE TABLE $TABLE_EVENTS (
            $COLUMN_ID INTEGER PRIMARY KEY,
            $COLUMN_LABEL TEXT,
            $COLUMN_ALIASES TEXT,
            $COLUMN_DESCRIPTION TEXT,
            $COLUMN_WIKIPEDIA TEXT,
            $COLUMN_POPU_FR INTEGER,
            $COLUMN_POPU_EN INTEGER,
            $COLUMN_FAVORITE INTEGER,
            $COLUMN_ETIQUETTE TEXT

        )
        """.trimIndent()

        val createClaimsTableStatement = """
        CREATE TABLE $TABLE_CLAIMS (
            $COLUMN_CLAIMS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_CLAIMS_EVENTID INTEGER,
            $COLUMN_CLAIMS_VERBOSE TEXT,
            $COLUMN_CLAIMS_VALUE TEXT,
            $COLUMN_ITEM_LABEL TEXT,
            $COLUMN_ITEM_DESCRITPION TEXT,
            $COLUMN_ITEM_WIKI TEXT,
            FOREIGN KEY ($COLUMN_CLAIMS_EVENTID) REFERENCES $TABLE_EVENTS($COLUMN_ID)
        )
        """.trimIndent()

        val createDateTableStatement = """
        CREATE TABLE $TABLE_DATE (
            $COLUMN_DATE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_DATE_EVENTID INTEGER,
            $COLUMN_YEAR INTEGER,
            $COLUMN_MONTH INTEGER,
            $COLUMN_DAY INTEGER,
            FOREIGN KEY ($COLUMN_DATE_EVENTID) REFERENCES $TABLE_EVENTS($COLUMN_ID)
        )
        """.trimIndent()

        val createGeoTableStatement = """
        CREATE TABLE $TABLE_GEO (
            $COLUMN_GEO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_GEO_EVENTID INTEGER,
            $COLUMN_LATITUDE DOUBLE,
            $COLUMN_LONGITUDE DOUBLE,
            FOREIGN KEY ($COLUMN_GEO_EVENTID) REFERENCES $TABLE_EVENTS($COLUMN_ID)
        )
        """.trimIndent()

        db.execSQL(createGeoTableStatement)
        db.execSQL(createEventsTableStatement)
        db.execSQL(createClaimsTableStatement)
        db.execSQL(createDateTableStatement)
    }



    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS");
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLAIMS");
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DATE");
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GEO");
        onCreate(db);
    }


    fun hasEventsData(): Boolean {
        val db = this.readableDatabase
        var hasData = false

        val query = "SELECT COUNT(*) FROM $TABLE_EVENTS"

        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                hasData = cursor.getInt(0) > 0
            }
        }

        db.close()
        return hasData
    }


    fun getAllItems(): List<Event> {
        val events = mutableListOf<Event>()
        val db = this.readableDatabase

        // Récupérer tous les événements
        val eventsCursor = db.rawQuery("SELECT * FROM events", null)

        while (eventsCursor.moveToNext()) {
            val id = eventsCursor.getInt(eventsCursor.getColumnIndexOrThrow("id"))
            val label = eventsCursor.getString(eventsCursor.getColumnIndexOrThrow("label"))
            val aliases = eventsCursor.getString(eventsCursor.getColumnIndexOrThrow("aliases"))
            val description = eventsCursor.getString(eventsCursor.getColumnIndexOrThrow("description"))
            val wikipedia = eventsCursor.getString(eventsCursor.getColumnIndexOrThrow("wikipedia"))
            val popularityEn = eventsCursor.getInt(eventsCursor.getColumnIndexOrThrow("popularity_en"))
            val popularityFr = eventsCursor.getInt(eventsCursor.getColumnIndexOrThrow("popularity_fr"))
            val favorite = eventsCursor.getInt(eventsCursor.getColumnIndexOrThrow(COLUMN_FAVORITE))
            val etiquette = eventsCursor.getString(eventsCursor.getColumnIndexOrThrow(COLUMN_ETIQUETTE))


            val popularity = Popularity(en = popularityEn, fr = popularityFr)

            val claims = mutableListOf<Claim>()
            val claimsCursor = db.rawQuery("SELECT * FROM claims WHERE events_id = ?", arrayOf(id.toString()))

            while (claimsCursor.moveToNext()) {
                val claimId = claimsCursor.getInt(claimsCursor.getColumnIndexOrThrow("id"))
                val verboseName = claimsCursor.getString(claimsCursor.getColumnIndexOrThrow("verboseName"))
                val value = claimsCursor.getString(claimsCursor.getColumnIndexOrThrow("value"))
                val itemLabel = claimsCursor.getString(claimsCursor.getColumnIndexOrThrow("Itemlabel"))
                val itemDescription = claimsCursor.getString(claimsCursor.getColumnIndexOrThrow("Itemdescription"))
                val itemWikipedia = claimsCursor.getString(claimsCursor.getColumnIndexOrThrow("Itemwikipedia"))
                val item = if (itemLabel != null && itemDescription != null && itemWikipedia != null) {
                    Item(label = itemLabel, description = itemDescription, wikipedia = itemWikipedia)
                } else {
                    null
                }

                claims.add(Claim(id = claimId ,verboseName = verboseName, value = value, item = item))
            }
            claimsCursor.close()

            var date: Date? = null
            val dateCursor = db.rawQuery("SELECT * FROM date WHERE date_id = ?", arrayOf(id.toString()))

            while (dateCursor.moveToNext()) {
                val dateId = dateCursor.getInt(dateCursor.getColumnIndexOrThrow(COLUMN_DATE_ID))
                val year = dateCursor.getInt(dateCursor.getColumnIndexOrThrow(COLUMN_YEAR))
                val month = dateCursor.getInt(dateCursor.getColumnIndexOrThrow(COLUMN_MONTH))
                val day = dateCursor.getInt(dateCursor.getColumnIndexOrThrow(COLUMN_DAY))

                date = Date(dateId, year, month, day)
            }

            dateCursor.close()

            var geo: Geo? = null
            val geoCursor = db.rawQuery("SELECT * FROM geo WHERE geo_id = ?", arrayOf(id.toString()))

            while (geoCursor.moveToNext()) {
                val geoId = geoCursor.getInt(geoCursor.getColumnIndexOrThrow(COLUMN_GEO_ID))
                val latitude = geoCursor.getDouble(geoCursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val longitude = geoCursor.getDouble(geoCursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))

                geo = Geo(geoId, latitude, longitude)
            }

            geoCursor.close()
            events.add(Event(id = id, label = label, aliases = aliases, description = description, wikipedia = wikipedia, popularity = popularity, claims = claims, date, geo ,favorite, etiquette))
        }

        eventsCursor.close()
        db.close()
        return events
    }


    fun deleteAllItems() {
        val db = this.writableDatabase
        db.delete(TABLE_EVENTS, null, null)
        db.close()
    }


    fun hasGeoData(): Boolean {
        val db = this.readableDatabase
        var hasData = false

        val query = "SELECT EXISTS (SELECT 1 FROM $TABLE_GEO WHERE $COLUMN_LATITUDE IS NOT NULL AND $COLUMN_LONGITUDE IS NOT NULL LIMIT 1)"

        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                hasData = cursor.getInt(0) == 1
            }
        }
        db.close()
        return hasData
    }

    fun hasValidDateData(): Boolean {
        val db = this.readableDatabase
        var hasData = false

        // Requête pour vérifier l'existence de lignes avec des valeurs différentes de 0 pour year, month, et day
        val query = """
        SELECT EXISTS (
            SELECT 1 
            FROM $TABLE_DATE 
            WHERE $COLUMN_YEAR != 0 OR $COLUMN_MONTH != 1 OR $COLUMN_DAY != 1 
            LIMIT 1
        )
    """.trimIndent()

        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                // Si le résultat est 1, cela signifie qu'il existe au moins une ligne avec des valeurs valides (différentes de 0)
                hasData = cursor.getInt(0) == 1
            }
        }

        db.close()
        return hasData
    }



    fun addGeoAllEvent(eventGeoMap: Map<Int, Geo>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            eventGeoMap.forEach { (eventId, geo) ->
                // Vérifiez si l'entrée existe déjà
                val cursor = db.query(
                    TABLE_GEO,
                    arrayOf(COLUMN_DATE_EVENTID),
                    "$COLUMN_GEO_EVENTID = ?",
                    arrayOf(eventId.toString()),
                    null,
                    null,
                    null
                )
                val exists = cursor.moveToFirst()
                cursor.close()

                val geoValues = ContentValues().apply {
                    put(COLUMN_GEO_EVENTID, eventId)
                    put(COLUMN_LATITUDE, geo.latitude)
                    put(COLUMN_LONGITUDE, geo.longitude)
                }

                if (exists) {
                    db.update(TABLE_GEO, geoValues, "$COLUMN_GEO_EVENTID = ?", arrayOf(eventId.toString()))
                } else {
                    db.insert(TABLE_GEO, null, geoValues)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        db.close()
    }



    fun addDateAllEvent(eventDateMap: Map<Int, Date>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            eventDateMap.forEach { (eventId, date) ->
                val cursor = db.query(
                    TABLE_DATE,
                    arrayOf(COLUMN_DATE_EVENTID),
                    "$COLUMN_DATE_EVENTID = ?",
                    arrayOf(eventId.toString()),
                    null, null, null
                )
                val exists = cursor.moveToFirst()
                cursor.close()

                val eventValues = ContentValues().apply {
                    put(COLUMN_DATE_EVENTID, eventId)
                    put(COLUMN_YEAR, date.year)
                    put(COLUMN_MONTH, date.month)
                    put(COLUMN_DAY, date.day)
                }

                if (exists) {
                    // Mise à jour de l'entrée si elle existe déjà
                    db.update(
                        TABLE_DATE,
                        eventValues,
                        "$COLUMN_DATE_EVENTID = ?",
                        arrayOf(eventId.toString())
                    )
                } else {
                    // Insertion de la nouvelle entrée
                    db.insert(TABLE_DATE, null, eventValues)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        db.close()
    }



    fun toggleFavorite(eventId: Int) {
        val db = this.writableDatabase

        // Récupérer l'état actuel du favori pour cet eventId
        val cursor = db.query(
            TABLE_EVENTS,
            arrayOf(COLUMN_FAVORITE),
            "$COLUMN_ID = ?",
            arrayOf(eventId.toString()),
            null, null, null
        )

        var isFavorite = 0

        if (cursor.moveToFirst()) { // Move to the first row of the cursor
            val columnIndex = cursor.getColumnIndex(COLUMN_FAVORITE)
            if (columnIndex >= 0) {
                isFavorite = cursor.getInt(columnIndex)
            }
        }

        cursor.close()

        // Inverser l'état du favori
        val newValue = if (isFavorite == 1) 0 else 1

        // Mettre à jour l'état du favori dans la base de données
        val contentValues = ContentValues()
        contentValues.put(COLUMN_FAVORITE, newValue)

        db.update(
            TABLE_EVENTS,
            contentValues,
            "$COLUMN_ID = ?",
            arrayOf(eventId.toString())
        )

        db.close()
    }


    fun toggleEtiquette(eventId: Int, etiquette: String) {
        val db = this.writableDatabase

        // Mettre à jour l'état du favori dans la base de données
        val contentValues = ContentValues()
        contentValues.put(COLUMN_ETIQUETTE, etiquette)

        db.update(
            TABLE_EVENTS,
            contentValues,
            "$COLUMN_ID = ?",
            arrayOf(eventId.toString())
        )
        db.close()
    }


    fun insertItem(event: Event) {
        val db = this.writableDatabase

        val cursor = db.query(
            TABLE_EVENTS,
            arrayOf(COLUMN_ID),
            "$COLUMN_ID = ?",
            arrayOf(event.id.toString()),
            null, null, null
        )

        val exists = cursor.count > 0
        cursor.close()

        if (!exists) {
            val eventValues = ContentValues().apply {
                put(COLUMN_ID, event.id)
                put(COLUMN_LABEL, event.label)
                put(COLUMN_ALIASES, event.aliases)
                put(COLUMN_DESCRIPTION, event.description)
                put(COLUMN_WIKIPEDIA, event.wikipedia)
                put(COLUMN_POPU_FR, event.popularity.fr)
                put(COLUMN_POPU_EN, event.popularity.en)
                put(COLUMN_FAVORITE, event.isFavorite)
                put(COLUMN_ETIQUETTE, event.etiquette)
            }
            db.insert(TABLE_EVENTS, null, eventValues)
        }

        event.claims.forEach { claim ->

            val cursorClaim = db.query(
                TABLE_CLAIMS,
                arrayOf(COLUMN_CLAIMS_ID),
                "$COLUMN_CLAIMS_ID = ?",
                arrayOf(claim.id.toString()),
                null, null, null
            )

            val claimExists = cursorClaim.count > 0
            cursorClaim.close()

            if (!claimExists) {
                val claimValues = ContentValues().apply {
                    put(COLUMN_CLAIMS_EVENTID, event.id)
                    put(COLUMN_CLAIMS_VERBOSE, claim.verboseName)
                    put(COLUMN_CLAIMS_VALUE, claim.value)
                    put(COLUMN_ITEM_LABEL, claim.item?.label ?: "")
                    put(COLUMN_ITEM_DESCRITPION, claim.item?.description ?: "")
                    put(COLUMN_ITEM_WIKI, claim.item?.wikipedia ?: "")
                }
                db.insert(TABLE_CLAIMS, null, claimValues)
            }
        }

        val cursordate = db.query(
            TABLE_DATE,
            arrayOf(COLUMN_DATE_ID),
            "$COLUMN_DATE_ID = ?",
            arrayOf(event.date?.id.toString()),
            null, null, null
        )

        val dateExists = cursordate.count > 0
        cursordate.close()

        if (!dateExists) {
            val eventValues = ContentValues().apply {
                put(COLUMN_DATE_EVENTID, event.id)
                put(COLUMN_YEAR, 0)
                put(COLUMN_MONTH, 1)
                put(COLUMN_DAY,1)
            }
            db.insert(TABLE_DATE, null, eventValues)
        }

        val cursorGeo = db.query(
            TABLE_GEO,
            arrayOf(COLUMN_GEO_ID),
            "$COLUMN_GEO_ID = ?",
            arrayOf(event.geo?.id.toString()),
            null, null, null
        )

        val geoExiste = cursorGeo.count > 0
        cursorGeo.close()

        if (!geoExiste) {
            val eventValues = ContentValues().apply {
                put(COLUMN_GEO_EVENTID, event.id)
                put(COLUMN_LATITUDE, 0)
                put(COLUMN_LONGITUDE, 0)
            }
            db.insert(TABLE_GEO, null, eventValues)
        }
        db.close()
    }
}