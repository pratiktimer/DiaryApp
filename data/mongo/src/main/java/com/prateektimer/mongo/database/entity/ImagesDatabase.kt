package com.prateektimer.mongo.database.entity

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ImageToUpload::class, ImagesToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase (){

    abstract fun imagesToUploadDao(): ImageToUploadDao
    abstract fun imagesToDeleteDao(): ImageToDeleteDao
}