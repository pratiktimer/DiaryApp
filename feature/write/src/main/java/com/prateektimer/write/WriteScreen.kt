package com.prateektimer.write

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prateektimer.ui.GalleryImage
import com.prateektimer.ui.GalleryState
import com.prateektimer.util.model.Diary
import com.prateektimer.util.model.Mood
import java.time.ZonedDateTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    uiState: UiState,
    onBackPressed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    pagerState: PagerState,
    ontitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    titleMood: () -> String,
    onSaveClicked: (Diary) -> Unit,
    onUpdatedDateTime: (ZonedDateTime) -> Unit,
    galleryState: GalleryState,
    onImageSelect:(Uri) -> Unit,
    onImageDeleteClicked:(GalleryImage) -> Unit
){
    var selectedGalleyImage by remember { mutableStateOf<GalleryImage?>(null) }
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
                galleryState = galleryState,
                pagerState = pagerState,
                paddingValues = it ,
                onTitleChanged = ontitleChanged,
                title = uiState.title,
                description = uiState.description ,
                onDescriptionChanged = onDescriptionChanged,
                uiState = uiState,
                onSaveClicked = onSaveClicked,
                onImageSelect = onImageSelect,
                onImageClick = {selectedGalleyImage = it}
            )

            AnimatedVisibility(
                visible = selectedGalleyImage != null
            ) {
                Dialog(onDismissRequest = {selectedGalleyImage = null}
                ){
                  if(selectedGalleyImage != null){
                      ZoomableImage(
                          selectedGalleryImage = selectedGalleyImage!!,
                          onCloseClicked = {selectedGalleyImage = null },
                          onDeleteClicked = {
                              if(selectedGalleyImage != null){
                                  onImageDeleteClicked(selectedGalleyImage!!)
                                  selectedGalleyImage = null
                              }
                          }
                      )
                  }
                }
            }
        }
    )
}

@Composable
fun ZoomableImage(
    selectedGalleryImage: GalleryImage,
    onCloseClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = maxOf(1f, minOf(scale * zoom, 5f))
                    val maxX = (size.width * (scale - 1)) / 2
                    val minX = -maxX
                    offsetX = maxOf(minX, minOf(maxX, offsetX + pan.x))
                    val maxY = (size.height * (scale - 1)) / 2
                    val minY = -maxY
                    offsetY = maxOf(minY, minOf(maxY, offsetY + pan.y))
                }
            }
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = maxOf(.5f, minOf(3f, scale)),
                    scaleY = maxOf(.5f, minOf(3f, scale)),
                    translationX = offsetX,
                    translationY = offsetY
                ),
            model = ImageRequest.Builder(LocalContext.current)
                .data(selectedGalleryImage.image.toString())
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Fit,
            contentDescription = "Gallery Image"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onCloseClicked) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Icon")
                Text(text = "Close")
            }
            Button(onClick = onDeleteClicked) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Icon")
                Text(text = "Delete")
            }
        }
    }
}