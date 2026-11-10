package com.sagay.touristnav

import android.app.Application
import com.sagay.touristnav.data.local.SeedLoader
import com.sagay.touristnav.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import javax.inject.Inject

@HiltAndroidApp
class SagayApp : Application() {

    @Inject lateinit var seedLoader: SeedLoader

    @ApplicationScope
    @Inject lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        appScope.launch { seedLoader.seed() }
    }
}
