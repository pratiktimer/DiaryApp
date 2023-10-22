package com.prateektimer.diaryapp.presentation.screens.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prateektimer.diaryapp.data.repository.MongoDB
import com.prateektimer.diaryapp.model.Diary
import com.prateektimer.diaryapp.model.Mood
import com.prateektimer.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.prateektimer.diaryapp.util.RequestState
import com.prateektimer.diaryapp.util.toRealmInstant
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId
import java.time.ZonedDateTime


class WriteViewModel(
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    var uiState by mutableStateOf(UiState())
        private  set
    init {
        getDiaryIdArgument()
        fetchSelectedDiary()
    }



    private fun getDiaryIdArgument(){
        uiState = uiState.copy(
            selectedDiaryId = savedStateHandle.get<String>(
                key = WRITE_SCREEN_ARGUMENT_KEY
            )
        )
    }

    fun getObjectIdFromString(inputString: String): ObjectId {
        val regex = Regex("""BsonObjectId\((\w+)\)""")
        val matchResult = regex.find(inputString)

        return matchResult?.groupValues?.get(1)?.let {
            try {
                ObjectId(it)
            } catch (e: IllegalArgumentException) {
                null // Return null if the conversion fails
            }

        }!!
    }
    private  fun fetchSelectedDiary(){
        if(uiState.selectedDiaryId != null){
            viewModelScope.launch(Dispatchers.Main)
            {
                val diary = getObjectIdFromString(uiState.selectedDiaryId!!)?.let {
                    MongoDB.getSelectedDiary(
                        diaryId = it
                    ).catch {
                        emit(RequestState.Error(Exception("Diary is already deleted")))
                    }
                        .collect{ diary ->
                        if(diary is RequestState.Success){
                            setSelectedDiary(diary = diary.data)
                            setTitle(diary.data.title)
                            setDescription(diary.data.description)
                            setMood(Mood.valueOf(diary.data.mood))
                        }
                    }
                }
            }
        }
    }

    fun setSelectedDiary(diary: Diary){
        uiState = uiState.copy(selectedDiary = diary)
    }
    fun setTitle(title: String){
        uiState = uiState.copy(title = title)
    }

    fun setDescription(description: String){
        uiState = uiState.copy(description = description)
    }

    fun setMood(mood: Mood){
        uiState = uiState.copy(mood = mood)
    }

    fun updateDateTime(zonedateTime: ZonedDateTime){
        uiState = uiState.copy(updatedDateTime = zonedateTime.toInstant().toRealmInstant())
    }

    suspend fun insertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ){
        val result = MongoDB.insertNewDiary(diary = diary.apply {
            if(uiState.updatedDateTime != null){
                date = uiState.updatedDateTime!!
            }
        })
        if(result is RequestState.Success){
            withContext(Dispatchers.Main){
                onSuccess()
            }
        }
        else if(result is RequestState.Error){
            onError(result.error.message.toString())
        }
    }

    fun upsertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        viewModelScope.launch(Dispatchers.IO) {
            if(uiState.selectedDiaryId != null){
                updateDiary(diary = diary,onSuccess = onSuccess, onError = onError)
            }
            else{
                insertDiary(diary = diary,onSuccess = onSuccess, onError = onError)
            }
        }
    }
    suspend fun updateDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ){
        val result = MongoDB.updateDiary(diary = diary.apply {
            _id = getObjectIdFromString(uiState.selectedDiaryId!!)
            date = if(uiState.updatedDateTime != null)uiState.updatedDateTime!! else uiState.selectedDiary!!.date
        })
        if(result is RequestState.Success){
            withContext(Dispatchers.Main){
                onSuccess()
            }
        }
        else if(result is RequestState.Error){
            onError(result.error.message.toString())
        }
    }

    fun  deleteDiary(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        viewModelScope.launch(Dispatchers.IO) {
            if(uiState.selectedDiaryId != null){
                val result = MongoDB.deleteDiary(diaryId = getObjectIdFromString(uiState.selectedDiaryId!!))
                if(result is RequestState.Success){
                    withContext(Dispatchers.Main){
                        onSuccess()
                    }
                }
                else if(result is RequestState.Error){
                    withContext(Dispatchers.Main){
                        onError(result.error.message.toString())
                    }
                }

            }
        }
    }
}

data class UiState(
    val selectedDiaryId: String? = null,
    val selectedDiary : Diary? = null,
    val title: String ="",
    val description: String ="",
    val mood: Mood = Mood.Neutral,
    val updatedDateTime : RealmInstant? = null
)