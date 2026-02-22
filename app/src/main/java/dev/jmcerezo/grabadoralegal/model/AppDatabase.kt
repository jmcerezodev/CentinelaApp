package dev.jmcerezo.grabadoralegal.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GrabacionDato::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun grabacionDao(): GrabacionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grabadora_database"
                )
                // Usamos la versión no deprecada para Room 2.7+
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
