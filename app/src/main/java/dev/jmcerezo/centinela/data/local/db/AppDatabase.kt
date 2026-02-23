package dev.jmcerezo.centinela.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos principal de la aplicación utilizando Room.
 * 
 * Gestiona la persistencia de las grabaciones y sus metadatos.
 */
@Database(entities = [GrabacionDato::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Proporciona el DAO para interactuar con la tabla de grabaciones.
     */
    abstract fun grabacionDao(): GrabacionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (Patrón Singleton).
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grabadora_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
