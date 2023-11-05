package com.prateektimer.mongo.repository


import com.prateektimer.util.model.Diary
import com.prateektimer.util.model.RequestState
import kotlinx.coroutines.flow.Flow
import org.mongodb.kbson.ObjectId
import java.time.LocalDate
import java.time.ZonedDateTime

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>
interface MongoRepository {

    fun configureTheRealm()
    fun getAllDiaries(): Flow<Diaries>
    fun getSelectedDiary(diaryId : ObjectId): Flow<RequestState<Diary>>
    suspend fun insertNewDiary(diary: Diary): RequestState<Diary>
    suspend fun updateDiary(diary: Diary): RequestState<Diary>
    suspend fun deleteDiary(diaryId: ObjectId): RequestState<Diary>
    suspend fun deleteAllDiaries(): RequestState<Boolean>

    suspend fun getfilterDiaries(zonedDateTime: ZonedDateTime): Flow<Diaries>
}