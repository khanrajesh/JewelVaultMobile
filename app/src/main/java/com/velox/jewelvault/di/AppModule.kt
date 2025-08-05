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
import com.velox.jewelvault.utils.AppUpdateManager
import com.velox.jewelvault.utils.RemoteConfigManager
import com.velox.jewelvault.utils.SessionManager
import com.velox.jewelvault.utils.backup.BackupManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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
    fun provideSnackMessageState(): MutableState<String> {
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
                RoomMigration.MIGRATION_5_6
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
        dataStoreManager: DataStoreManager
    ): BackupManager {
        return BackupManager(
            context = context,
            database = database,
            storage = storage,
            dataStoreManager = dataStoreManager
        )
    }

    //endregion

}