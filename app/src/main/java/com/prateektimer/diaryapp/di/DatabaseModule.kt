package com.prateektimer.diaryapp.di

import android.content.Context
import androidx.room.Room
import com.prateektimer.diaryapp.connectivity.NetworkConnectivityObserver
import com.prateektimer.diaryapp.data.database.entity.ImagesDatabase
import com.prateektimer.diaryapp.util.Constants.IMAGES_DATABASE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ):ImagesDatabase{
        return Room.databaseBuilder(
            context = context,
            klass = ImagesDatabase::class.java,
            name = IMAGES_DATABASE
        ).build()

    }

    @Singleton
    @Provides
    fun provideFirstDao(database: ImagesDatabase) = database.imagesToUploadDao()
    @Singleton
    @Provides
    fun provideSecondDao(database: ImagesDatabase) = database.imagesToDeleteDao()

    @Singleton
    @Provides
    fun provideNetworkConnectivityObserver(
        @ApplicationContext context: Context
    ) = NetworkConnectivityObserver(context)

}