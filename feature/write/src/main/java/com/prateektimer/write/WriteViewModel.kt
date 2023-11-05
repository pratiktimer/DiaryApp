package com.prateektimer.write

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.prateektimer.util.model.Diary
import com.prateektimer.util.model.Mood
import com.prateektimer.util.model.RequestState
import com.prateektimer.mongo.database.entity.ImageToDeleteDao
import com.prateektimer.mongo.database.entity.ImageToUpload
import com.prateektimer.mongo.database.entity.ImageToUploadDao
import com.prateektimer.mongo.database.entity.ImagesToDelete
import com.prateektimer.mongo.repository.MongoDB
import com.prateektimer.ui.GalleryImage
import com.prateektimer.ui.GalleryState
import com.prateektimer.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.prateektimer.util.fetchImagesFromFirebase
import com.prateektimer.util.toRealmInstant
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private  val imageToUploadDao: ImageToUploadDao,
    private  val imageToDeleteDao: ImageToDeleteDao
): ViewModel() {
    val galleryState = GalleryState()
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

                            fetchImagesFromFirebase(
                                remoteImagePaths = diary.data.images,
                                onImageDownload = {
                                    downloadedImage ->
                                    galleryState.addImage(
                                        GalleryImage(
                                            image = downloadedImage,
                                            remoteImagePath = extractRemoteImagePath(
                                                remotePath = downloadedImage.toString()
                                            )
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun extractRemoteImagePath(remotePath: String): String {
        val chunks = remotePath.split("%2F")
        var imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/${imageName}"
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
            uploadImagesToFirebase()
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
            //galleryState.clearImagesToBeDeleted()
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
            uploadImagesToFirebase()
            withContext(Dispatchers.Main){
                deleteImagesFromFirebase()
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
                        uiState.selectedDiary?.let { deleteImagesFromFirebase(images = it.images) }
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

    fun  addImage(image: Uri, imageType: String){
     val remoteImagePath = "images/${FirebaseAuth.getInstance().currentUser?.uid}/"+"${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"

        galleryState.addImage(
            GalleryImage(
                image = image,
                remoteImagePath = remoteImagePath
            )
        )
    }

    private fun deleteImagesFromFirebase(images: List<String> ? = null){
        val storage = FirebaseStorage.getInstance().reference
        if(images != null) {
            images.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                     viewModelScope.launch(Dispatchers.IO){
                       imageToDeleteDao.addImageToDelete(
                           ImagesToDelete(
                               remoteImagePath = remotePath
                           )
                       )
                   }
                 }
            }
        }
        else{
            galleryState.imagesToBeDeleted.map {
                it.remoteImagePath
            }.forEach {remotePath->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO){
                            imageToDeleteDao.addImageToDelete(
                                ImagesToDelete(
                                    remoteImagePath = remotePath
                                )
                            )
                        }
                    }
            }
        }
    }

    private fun uploadImagesToFirebase(){
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach {galleryImage->
            val imagePath = storage.child(galleryImage.remoteImagePath)
            imagePath.putFile(galleryImage.image)
                .addOnProgressListener {
                    val sessionUri = it.uploadSessionUri
                    if(sessionUri != null){
                        viewModelScope.launch(Dispatchers.IO) {
                            imageToUploadDao.addImagesToUpload(
                                ImageToUpload(
                                    remoteImagePath = galleryImage.remoteImagePath,
                                    imageUri = galleryImage.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
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