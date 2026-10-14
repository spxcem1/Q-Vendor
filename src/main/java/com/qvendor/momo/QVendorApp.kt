package com.qvendor.momo

import android.app.Application
import com.qvendor.momo.data.AppDatabase
import com.qvendor.momo.data.CredentialsStore

class QVendorApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val credentials: CredentialsStore by lazy { CredentialsStore(this) }
}
