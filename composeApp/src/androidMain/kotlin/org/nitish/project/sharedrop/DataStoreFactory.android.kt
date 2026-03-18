package org.nitish.project.sharedrop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

actual fun provideDataStore(): DataStore<Preferences> {
    if (AndroidContext.context == null) {
        throw IllegalStateException("Context is not available. Make sure to initialize AndroidContext in your Application class.")
    }
    return createDataStore(
        producePath = {
            AndroidContext.context!!.filesDir.resolve(dataStoreFileName).absolutePath
        }
    )
}