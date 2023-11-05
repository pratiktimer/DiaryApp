package com.prateektimer.diaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import com.prateektimer.diaryapp.navigation.SetupNavGraph
import com.prateektimer.mongo.database.entity.ImageToDeleteDao
import com.prateektimer.mongo.database.entity.ImageToUpload
import com.prateektimer.mongo.database.entity.ImageToUploadDao
import com.prateektimer.mongo.database.entity.ImagesToDelete
import com.prateektimer.ui.theme.DiaryAppTheme
import com.prateektimer.util.Constants.APP_ID
import com.prateektimer.util.Screen
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageToUploadDao: ImageToUploadDao

    @Inject
    lateinit var imagesToDeleteDao: ImageToDeleteDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        FirebaseApp.initializeApp(this)
        setContent {
            DiaryAppTheme(dynamicColor = false) {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        // keepSplashOpened = false
                    }
                )
            }
        }

        cleanupCheck(scope = lifecycleScope, imageToUploadDao = imageToUploadDao, imageToDeleteDao = imagesToDeleteDao)
    }
}

private fun  cleanupCheck(
    scope:CoroutineScope,
    imageToUploadDao: ImageToUploadDao,
    imageToDeleteDao: ImageToDeleteDao
){
scope.launch(Dispatchers.IO)
{
    val result = imageToUploadDao.getAllImages()
    result.forEach {imageToUpload ->
      retryUploadingImageTofirebase(
      imageToUpload = imageToUpload,
      onSuccess = {
         scope.launch(Dispatchers.IO){
             imageToUploadDao.cleanupImage(imageId = imageToUpload.id)
         }
        }
      )
    }

    val result2 = imageToDeleteDao.getAllImages()
    result2.forEach {imageToDelete ->
        retryDeletingImageTofirebase(
            imageToDelete = imageToDelete,
            onSuccess = {
                scope.launch(Dispatchers.IO){
                    imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                }
            }
        )
    }
  }
}
private fun getStartDestination(): String {
    val user = App.create(APP_ID).currentUser
    return if (user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}
fun retryUploadingImageTofirebase(
    imageToUpload: ImageToUpload,
    onSuccess:()-> Unit
){
    val storage = FirebaseStorage.getInstance().reference
    storage.child(imageToUpload.remoteImagePath).putFile(
        imageToUpload.imageUri.toUri(),
        storageMetadata {  },
        imageToUpload.sessionUri.toUri()
    ).addOnSuccessListener { onSuccess() }
}

fun retryDeletingImageTofirebase(
    imageToDelete: ImagesToDelete,
    onSuccess:()-> Unit
){
    val storage = FirebaseStorage.getInstance().reference
    storage.child(imageToDelete.remoteImagePath).delete().addOnSuccessListener { onSuccess() }
}

