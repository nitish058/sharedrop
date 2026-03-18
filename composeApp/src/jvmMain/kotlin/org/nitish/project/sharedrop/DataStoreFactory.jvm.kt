package org.nitish.project.sharedrop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

actual fun provideDataStore(): DataStore<Preferences> = createDataStore(
    producePath = {
        // Salva nella cartella home dell'utente (es. /Users/nome/.sharedrop/...)
        val dataDir = File(System.getProperty("user.home"), ".sharedrop")
        if (!dataDir.exists()) dataDir.mkdirs()
        File(dataDir, dataStoreFileName).absolutePath
    }
)