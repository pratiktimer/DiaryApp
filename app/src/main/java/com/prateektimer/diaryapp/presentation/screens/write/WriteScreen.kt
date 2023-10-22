package com.prateektimer.diaryapp.presentation.screens.write

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.prateektimer.diaryapp.model.Diary
import com.prateektimer.diaryapp.model.Mood
import java.time.ZonedDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WriteScreen(
    uiState: UiState,
    onBackPressed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    pagerState: PagerState,
    ontitleChanged:(String)-> Unit,
    onDescriptionChanged:(String) -> Unit,
    titleMood: () -> String,
    onSaveClicked:(Diary) -> Unit,
    onUpdatedDateTime: (ZonedDateTime) -> Unit
){
    LaunchedEffect(key1 = uiState.mood){
        pagerState.scrollToPage(Mood.valueOf(uiState.mood.name).ordinal)
    }
    Scaffold(
        topBar = {  WriteTopBar(
                                onBackPressed = onBackPressed,
                                selectedDiary = uiState.selectedDiary,
                                onDeleteConfirmed = onDeleteConfirmed,
                                titleMood = titleMood,
                                onUpdatedDateTime = onUpdatedDateTime
                               )
                 },
        content = { 
            WriteContent(
                pagerState = pagerState,
                paddingValues = it ,
                onTitleChanged = ontitleChanged,
                title = uiState.title,
                description = uiState.description ,
                onDescriptionChanged = onDescriptionChanged,
                uiState = uiState,
                onSaveClicked = onSaveClicked
            )
        }
    )
}