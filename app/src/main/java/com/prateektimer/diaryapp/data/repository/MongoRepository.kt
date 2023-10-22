package com.prateektimer.diaryapp.data.repository

import com.prateektimer.diaryapp.model.Diary
import com.prateektimer.diaryapp.util.RequestState
import kotlinx.coroutines.flow.Flow
import org.mongodb.kbson.ObjectId
import java.time.LocalDate

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>
interface MongoRepository {

    fun configureTheRealm()
    fun getAllDiaries(): Flow<Diaries>
    fun getSelectedDiary(diaryId : ObjectId): Flow<RequestState<Diary>>
    suspend fun insertNewDiary(diary: Diary): RequestState<Diary>
    suspend fun updateDiary(diary: Diary): RequestState<Diary>

    suspend fun deleteDiary(diaryId: ObjectId): RequestState<Diary>
}