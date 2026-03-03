package com.goodnotepad.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Folder::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN formatting TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN titleTextAlign TEXT NOT NULL DEFAULT 'LEFT'")
                db.execSQL("ALTER TABLE notes ADD COLUMN lineOpacity REAL NOT NULL DEFAULT 0.15")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goodnotepad_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromNoteTheme(value: NoteTheme): String = value.name
    @TypeConverter fun toNoteTheme(value: String): NoteTheme = NoteTheme.valueOf(value)

    @TypeConverter fun fromPageStyle(value: PageStyle): String = value.name
    @TypeConverter fun toPageStyle(value: String): PageStyle = PageStyle.valueOf(value)

    @TypeConverter fun fromHeaderColor(value: HeaderColor): String = value.name
    @TypeConverter fun toHeaderColor(value: String): HeaderColor = HeaderColor.valueOf(value)

    @TypeConverter fun fromTextAlign(value: TextAlign): String = value.name
    @TypeConverter fun toTextAlign(value: String): TextAlign = TextAlign.valueOf(value)

    @TypeConverter fun fromFolderColor(value: FolderColor): String = value.name
    @TypeConverter fun toFolderColor(value: String): FolderColor = FolderColor.valueOf(value)

    @TypeConverter fun fromViewMode(value: ViewMode): String = value.name
    @TypeConverter fun toViewMode(value: String): ViewMode = ViewMode.valueOf(value)
}
