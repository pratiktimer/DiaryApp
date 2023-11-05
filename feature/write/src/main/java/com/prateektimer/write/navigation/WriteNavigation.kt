package com.prateektimer.write.navigation


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prateektimer.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.prateektimer.util.Screen
import com.prateektimer.util.model.Mood
import com.prateektimer.write.WriteScreen
import com.prateektimer.write.WriteViewModel


@OptIn(ExperimentalFoundationApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit
){

    composable(route = Screen.Write.route,
        arguments = listOf(navArgument(name = WRITE_SCREEN_ARGUMENT_KEY){
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })
    ){

        val viewModel: WriteViewModel = hiltViewModel()
        val uiState = viewModel.uiState
        val galleryState = viewModel.galleryState
        val pagerState = rememberPagerState(pageCount = { Mood.values().size })
        val pageNumber by remember{ derivedStateOf { pagerState.currentPage }}
        val context = LocalContext.current

        LaunchedEffect(key1 = uiState){
            android.util.Log.d("SelectedDiary", "${uiState.selectedDiaryId}")
        }
        WriteScreen(
            galleryState = galleryState,
            uiState = uiState,
            pagerState = pagerState,
            onBackPressed = onBackPressed,
            onDeleteConfirmed = {
                viewModel.deleteDiary(
                    onSuccess = {
                        android.widget.Toast.makeText(
                            context,
                            "Deleted",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        onBackPressed()
                    },
                    onError = {
                        android.widget.Toast.makeText(
                            context,
                            it,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    })
            },
            ontitleChanged = { viewModel.setTitle(it) },
            onDescriptionChanged = {  viewModel.setDescription(it)},
            onUpdatedDateTime = {viewModel.updateDateTime(it)},
            titleMood = { Mood.values()[pageNumber].name},
            onSaveClicked = {
                viewModel.upsertDiary(
                    diary = it.apply { mood = Mood.values()[pageNumber].name },
                    onSuccess = { onBackPressed()},
                    onError = {})
            },
            onImageSelect = {
                val type = context.contentResolver.getType(it)?.split("/")?.last() ?: "jpeg"
                viewModel.addImage(
                    image = it,
                    imageType = type)

            },
            onImageDeleteClicked = { galleryState.removeImage(it)}
        )
    }
}