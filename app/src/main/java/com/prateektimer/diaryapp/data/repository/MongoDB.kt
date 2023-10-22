package com.prateektimer.diaryapp.data.repository


import com.prateektimer.diaryapp.model.Diary
import com.prateektimer.diaryapp.util.Constants.APP_ID
import com.prateektimer.diaryapp.util.RequestState
import com.prateektimer.diaryapp.util.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import org.mongodb.kbson.ObjectId
object MongoDB : MongoRepository {
    private val app = App.create(APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm

    init {
        configureTheRealm()
    }

    override fun configureTheRealm() {
        if (user != null) {
            val config = SyncConfiguration.Builder(user, setOf(Diary::class))
                .initialSubscriptions { sub ->
                    add(
                        query = sub.query<Diary>(query = "owner_id == $0", user.id),
                        name = "User's Diaries"
                    )
                }
               // .log(LogLevel.ALL)
                .build()
            realm = Realm.open(config)
        }
    }

    override fun getAllDiaries(): Flow<Diaries> {
        return if (user != null) {
            try {
                realm.query<Diary>(query = "owner_id == $0", user.id)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map { result ->
                         RequestState.Success(
                            data = result.list.groupBy {
                                it.date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        )
                    }
            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticatedException())) }
        }
    }

    override fun getSelectedDiary(diaryId: ObjectId): Flow<RequestState<Diary>> {
       return if(user != null) {
          try{
               realm.query<Diary>(query = "_id == $0",diaryId).asFlow().map {
                   RequestState.Success(data = it.list.first())
               }

          }
          catch (e: Exception){
              flow { emit(RequestState.Error(e)) }
          }
      }
        else{
            flow { emit(RequestState.Error(UserNotAuthenticatedException()))}
      }
    }

    override suspend fun insertNewDiary(diary: Diary): RequestState<Diary> {
        return if(user != null) {

           realm.write {
               try{
                   val addedDiary = copyToRealm(diary.apply { owner_id = user.id })
                   RequestState.Success(data = addedDiary)
               }
               catch (e: Exception){
                   RequestState.Error(e)
               }
           }
        }
        else{
            RequestState.Error(UserNotAuthenticatedException())
        }
    }
    override suspend fun updateDiary(diary: Diary): RequestState<Diary> {
        return if(user != null) {

            realm.write {
               val queryDiary = query<Diary>(query =  "_id == $0", diary._id).first().find()
               if(queryDiary != null){
                   queryDiary.title = diary.title
                   queryDiary.description = diary.description
                   queryDiary.mood = diary.mood
                   queryDiary.date = diary.date
                   RequestState.Success(data = queryDiary)

               }
                else{
                   RequestState.Error(error = Exception("Queries diary does not exist"))
               }
            }
        }
        else{
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteDiary(diaryId: ObjectId): RequestState<Diary> {
        return if(user != null) {
          realm.write {
              val diary =
                  query<Diary>(query = "_id == $0 AND owner_id == $1", diaryId, user.id)
                      .first().find()
              if(diary != null) {
                  try {

                      delete(diary)
                      RequestState.Success(data = diary)
                  } catch (e: Exception) {
                      RequestState.Error(error = e)
                  }
              }
              else{
                  RequestState.Error(error = Exception("Queries diary does not exist"))
              }
          }
        }
        else{
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

}
    private class UserNotAuthenticatedException : Exception("User is not Logged in.")