package com.prateektimer.diaryapp.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.prateektimer.diaryapp.connectivity.ConnectivityObserver
import com.prateektimer.diaryapp.connectivity.NetworkConnectivityObserver
import com.prateektimer.diaryapp.data.database.entity.ImageToDeleteDao
import com.prateektimer.diaryapp.data.database.entity.ImagesToDelete
import com.prateektimer.diaryapp.data.repository.Diaries
import com.prateektimer.diaryapp.data.repository.MongoDB
import com.prateektimer.util.model.RequestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
private val connectivity: NetworkConnectivityObserver,
private val imageToDeleteDao: ImageToDeleteDao
): ViewModel() {

    private lateinit var allDiariesJob: Job
    private lateinit var filterDiariesJob: Job
    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)
    var dateIsSelected by mutableStateOf(false)
        private set
    init {
        getDiaries()
        viewModelScope.launch {
            connectivity.observer().collect{
                network = it
            }
        }
    }

    fun getDiaries(zonedDateTime: ZonedDateTime? = null)
    {
        dateIsSelected = zonedDateTime != null
        diaries.value = RequestState.Loading
        if(dateIsSelected && zonedDateTime != null){
            observeFilterDiaries(zonedDateTime)
        }
        else{
            observeAllDiaries()
        }
    }

    private  fun observeFilterDiaries(zonedDateTime: ZonedDateTime? = null){

       filterDiariesJob = viewModelScope.launch {
           if(::allDiariesJob.isInitialized){
               allDiariesJob.cancelAndJoin()
           }
            MongoDB.getfilterDiaries(zonedDateTime!!).collect{
                    result->
                diaries.value = result
            }
        }
    }
    private  fun observeAllDiaries(){
       allDiariesJob = viewModelScope.launch {
           if(::filterDiariesJob.isInitialized){
               filterDiariesJob.cancelAndJoin()
           }
           MongoDB.getAllDiaries().collect{
               result->
               diaries.value = result
           }
       }
    }

    fun deleteAllDiaries(
        onSuccess:() -> Unit,
        onError:(Throwable) -> Unit
    ){
      if(network == ConnectivityObserver.Status.Available){
          val userId = FirebaseAuth.getInstance().currentUser?.uid
          val imagesDirectory ="images/${userId}"
          val storage = FirebaseStorage.getInstance().reference
          storage.child(imagesDirectory)
              .listAll()
              .addOnSuccessListener {
                it.items.forEach{ ref->
                    val imagePath = "images/${userId}/${ref.name}"
                    storage.child(imagePath).delete().addOnFailureListener{
                      viewModelScope.launch(Dispatchers.IO){
                          imageToDeleteDao.addImageToDelete(
                              ImagesToDelete(remoteImagePath = imagePath)
                          )
                      }
                    }
                }
            }
              .addOnFailureListener(onError)
          viewModelScope.launch(Dispatchers.IO){
              val result = MongoDB.deleteAllDiaries()
              if(result is RequestState.Success){
                  withContext(Dispatchers.Main){
                      onSuccess()
                  }
              }
              else if(result is RequestState.Error){
                  withContext(Dispatchers.Main){
                      onError(result.error)
                  }
              }
          }

        }
        else{
          onError(Exception("No Internet Connection"))
      }
    }
}