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
}