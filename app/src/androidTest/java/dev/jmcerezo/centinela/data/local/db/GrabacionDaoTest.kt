package dev.jmcerezo.centinela.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrabacionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GrabacionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.grabacionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertarYLeer() = runBlocking {
        val grabacion = GrabacionDato(
            nombre = "Test", 
            rutaArchivo = "ruta", 
            fecha = "20/10/2024", 
            hash = "123", 
            ubicacion = "Ubicacion"
        )
        dao.insert(grabacion)
        val lista = dao.getAll().first()
        assertEquals(1, lista.size)
        assertEquals("Test", lista[0].nombre)
    }

    @Test
    fun testActualizarFavorito() = runBlocking {
        val grabacion = GrabacionDato(
            nombre = "Test Favorito", 
            rutaArchivo = "ruta_fav", 
            fecha = "21/10/2024", 
            hash = "456"
        )
        dao.insert(grabacion)
        
        // Recuperamos la grabacion insertada (Room le asigna un ID)
        val insertada = dao.getAll().first()[0]
        
        // Marcamos como favorito y actualizamos
        val actualizada = insertada.copy(esFavorito = true)
        dao.update(actualizada)
        
        // Verificamos el cambio
        val resultado = dao.getAll().first()[0]
        assertTrue("La grabación debería estar marcada como favorita", resultado.esFavorito)
        assertEquals("Test Favorito", resultado.nombre)
    }
}
