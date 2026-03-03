package com.goodnotepad.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Folder::class],
    version = 4,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN icon TEXT NOT NULL DEFAULT 'FOLDER'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN lineAlignments TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN titleFontColor INTEGER NOT NULL DEFAULT ${0xFF333333.toInt()}")
                db.execSQL("ALTER TABLE notes ADD COLUMN contentFontColor INTEGER NOT NULL DEFAULT ${0xFF333333.toInt()}")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goodnotepad_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromNoteTheme(value: NoteTheme): String = value.name
    @TypeConverter fun toNoteTheme(value: String): NoteTheme = try { NoteTheme.valueOf(value) } catch (_: Exception) { NoteTheme.WHITE }

    @TypeConverter fun fromPageStyle(value: PageStyle): String = value.name
    @TypeConverter fun toPageStyle(value: String): PageStyle = try { PageStyle.valueOf(value) } catch (_: Exception) { PageStyle.LINED }

    @TypeConverter fun fromHeaderColor(value: HeaderColor): String = value.name
    @TypeConverter fun toHeaderColor(value: String): HeaderColor = try { HeaderColor.valueOf(value) } catch (_: Exception) { HeaderColor.NONE }

    @TypeConverter fun fromTextAlign(value: TextAlign): String = value.name
    @TypeConverter fun toTextAlign(value: String): TextAlign = try { TextAlign.valueOf(value) } catch (_: Exception) { TextAlign.LEFT }

    @TypeConverter fun fromFolderColor(value: FolderColor): String = value.name
    @TypeConverter fun toFolderColor(value: String): FolderColor = try { FolderColor.valueOf(value) } catch (_: Exception) { FolderColor.BROWN }

    @TypeConverter fun fromFolderIcon(value: FolderIcon): String = value.name
    @TypeConverter fun toFolderIcon(value: String): FolderIcon = try { FolderIcon.valueOf(value) } catch (_: Exception) { FolderIcon.FOLDER }

    @TypeConverter fun fromViewMode(value: ViewMode): String = value.name
    @TypeConverter fun toViewMode(value: String): ViewMode = try { ViewMode.valueOf(value) } catch (_: Exception) { ViewMode.GRID_2 }
}
