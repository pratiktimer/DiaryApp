package com.prateektimer.diaryapp.presentation.components


import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.prateektimer.diaryapp.model.Diary
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.prateektimer.diaryapp.model.Mood
import com.prateektimer.diaryapp.ui.theme.Elevation
import com.prateektimer.diaryapp.util.fetchImagesFromFirebase
import com.prateektimer.diaryapp.util.toInstant
import com.prateektimer.diaryapp.util.toRealmInstant
import io.realm.kotlin.ext.realmListOf
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

@Composable
fun DiaryHolder(diary: Diary, onClick: (String) -> Unit){
    var componentHeight by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current
    var galleryOpened by remember { mutableStateOf(false)}
    var galleryLoading by remember { mutableStateOf(false)}
    val downloadImages = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current
    LaunchedEffect(key1 = galleryOpened){
    if(galleryOpened && downloadImages.isEmpty()){
        galleryLoading = true;
        fetchImagesFromFirebase(
            remoteImagePaths = diary.images,
            onImageDownload = {image->
                downloadImages.add(image)
            },
            onImageDownloadFailed = {
                                    Toast.makeText(
                                        context,
                                        "Images not uploaded yet."+
                                        "Wait a little bit, or try uploading again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                galleryLoading = false
                galleryOpened = false
            },
            onReadyToDisplay = {
                galleryLoading = false
                galleryOpened = true
            }
        )
    }
    }
    Row (modifier = Modifier
        .clickable(
            indication = null,
            interactionSource = remember {
                 MutableInteractionSource()
            }
            ){ onClick(diary._id.toString()) }
    ){
        Spacer(modifier = Modifier.width((14.dp)))
        Surface (
            modifier = Modifier
                .width(2.dp)
                .height(componentHeight + 14.dp),
            tonalElevation = Elevation.Level1

        ){}
        Spacer(modifier = Modifier.width((20.dp)))
        Surface (
            modifier = Modifier
                .clip(shape = Shapes().medium)
                .onGloballyPositioned {
                    componentHeight = with(localDensity) { it.size.height.toDp() }
                },
            tonalElevation = Elevation.Level1
        ){
            Column(modifier = Modifier.fillMaxWidth()){
                DiaryHeader(moodName = diary.mood, time = diary.date.toInstant() )
                Text(
                    modifier = Modifier.padding(all = 14.dp),
                    text = diary.description,
                    style = TextStyle(fontSize =  MaterialTheme.typography.bodyMedium.fontSize),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if(diary.images.isNotEmpty()){
                    ShowGalleryButton(
                        galleryLoading = galleryLoading,
                        galleryOpened = galleryOpened,
                        onClick = {
                            galleryOpened = !galleryOpened

                        }
                    )
                }
                AnimatedVisibility(
                    visible = galleryOpened && !galleryLoading,
                    enter = fadeIn() + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                   Column (modifier = Modifier.padding(14.dp)){
                       Gallery(images = downloadImages)
                   }
                }
            }
        }
    }
}

@Composable
fun DiaryHeader(moodName: String, time: Instant) {
    val mood by remember { mutableStateOf(Mood.valueOf(moodName)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(mood.containerColor)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = mood.icon),
                contentDescription = "Mood Icon",
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = mood.name,
                color = mood.contentColor,
                style = TextStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize)
            )
        }
        Text(
            text = SimpleDateFormat("hh:mm a", Locale.US).format(Date.from(time)),
            color = mood.contentColor,
            style = TextStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        )
    }
}

@Composable
fun ShowGalleryButton(
    galleryOpened: Boolean,
    galleryLoading: Boolean,
    onClick: ()-> Unit
){
    TextButton(onClick = onClick) {
        Text(
            text = if(galleryOpened && galleryLoading) "Hide Gallery" else "Show Gallery",
            style = TextStyle(fontSize =  MaterialTheme.typography.bodySmall.fontSize)
            )
    }

}

@Composable
@Preview
fun DiaryHolderPreview(){
    DiaryHolder(diary = Diary().apply
    {
        title =""
        description ="Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged"
        mood = Mood.Happy.name
        images = realmListOf("https://images.pexels.com/photos/15484561/pexels-photo-15484561/free-photo-of-a-train-station-with-a-roof-and-a-train-track.jpeg?auto=compress&cs=tinysrgb&w=1600&lazy=load")
    } , onClick = {})
}