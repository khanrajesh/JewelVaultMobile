package com.velox.jewelvault.di

import android.app.Application
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.RoomMigration
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.bluetooth.BleManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import com.velox.jewelvault.utils.AppUpdateManager
import com.velox.jewelvault.data.firebase.RemoteConfigManager
import com.velox.jewelvault.data.remort.RepositoryImpl
import com.velox.jewelvault.utils.SessionManager
import com.velox.jewelvault.utils.sync.SyncManager
import com.velox.jewelvault.utils.fcm.FCMTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://jewelvaultbackend-44960140033.asia-south1.run.app/"

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context

    @Provides
    @Singleton
    fun provideApplication(
        @ApplicationContext context: Context
    ): Application = context as Application

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("vault_datastore")
        }
    }

    @Provides
    @Singleton
    fun provideLoadingState(): MutableState<Boolean> {
        return mutableStateOf(false)
    }

    @Provides
    @Singleton
    @Named("snackMessage")
    fun provideSnackMessageState(): MutableState<String> {
        return mutableStateOf("")
    }

    @Provides
    @Singleton
    @Named("currentScreenHeading")
    fun provideCurrentScreenHeadingState(): MutableState<String> {
        return mutableStateOf("")
    }

    @Provides
    @Singleton
    fun provideMetalRateList(): SnapshotStateList<MetalRate> {
        return mutableStateListOf<MetalRate>()
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(dataStore: DataStore<Preferences>): DataStoreManager {
        return DataStoreManager(dataStore)
    }

    @Provides
    @Singleton
    fun provideSessionManager(dataStoreManager: DataStoreManager): SessionManager {
        return SessionManager(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideFCMTokenManager(
        @ApplicationContext context: Context,
        dataStoreManager: DataStoreManager
    ): FCMTokenManager {
        return FCMTokenManager(context, dataStoreManager)
    }

    // ✅ Room Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vault_room_database"
        )
            .addMigrations(
                RoomMigration.MIGRATION_1_2,
                RoomMigration.MIGRATION_2_3,
                RoomMigration.MIGRATION_3_4,
                RoomMigration.MIGRATION_4_5,
                RoomMigration.MIGRATION_5_6,
                RoomMigration.MIGRATION_6_7,
                RoomMigration.MIGRATION_7_8,
                RoomMigration.MIGRATION_8_9,
                RoomMigration.MIGRATION_9_10,
                RoomMigration.MIGRATION_10_11,
                RoomMigration.MIGRATION_11_12,
                RoomMigration.MIGRATION_12_13,
                RoomMigration.MIGRATION_13_14,
                RoomMigration.MIGRATION_14_15
            )
            .build()
    }

    // region firebase

    @Provides
    @Singleton
    fun provideFirebaseAuthInstance(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFireStore(): FirebaseFirestore {
        return  Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return Firebase.storage
    }

    @Provides
    @Singleton
    fun provideRemoteConfigManager(
        @ApplicationContext context: Context,
        dataStoreManager: DataStoreManager
    ): RemoteConfigManager {
        return RemoteConfigManager(context, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideAppUpdateManager(): AppUpdateManager {
        return AppUpdateManager()
    }

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        database: AppDatabase,
        storage: FirebaseStorage,
        dataStoreManager: DataStoreManager,
        firestore: FirebaseFirestore
    ): SyncManager {
        return SyncManager(
            context = context,
            database = database,
            storage = storage,
            dataStoreManager = dataStoreManager,
            firestore = firestore
        )
    }

    // region Ktor

    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String = BASE_URL

    @Provides
    @Singleton
    fun provideKtorClient(@Named("baseUrl") baseUrl: String): HttpClient {
        return HttpClient(CIO) {
            expectSuccess = false
            engine {
                https {
                    trustManager = object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                    serverName = java.net.URI(baseUrl).host
                }
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("Ktor", message)
                    }
                }
            }
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                url(baseUrl)
            }
        }
    }

    @Provides
    @Singleton
    fun provideMetalRateRemote(
        client: HttpClient
    ): RepositoryImpl = RepositoryImpl(client)


    // endregion

    //endregion

    // region bluetooth printing

    @Provides
    @Singleton
    fun provideBluetoothReceiver(
        @ApplicationContext context: Context
    ): BleManager {
        return BleManager(context)
    }
    //endregion

}
