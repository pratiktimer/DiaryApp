package com.prateektimer.home

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prateektimer.mongo.repository.Diaries
import com.prateektimer.util.model.RequestState
import java.time.ZonedDateTime


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    diaries: Diaries,
    drawerState: DrawerState,
    onMenuClicked:() -> Unit,
    onSignOutClicked: () -> Unit,
    onDeleteAllClicked:()-> Unit,
    navigateToWrite:()-> Unit,
    navigateToWriteWithArgs:(String)-> Unit,
    isDateSelected: Boolean,
    onDateSelected:(ZonedDateTime) -> Unit,
    onDateReset:()-> Unit
){
    var padding by remember { mutableStateOf(PaddingValues()) }
    val scrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    NavigationDrawer(
        drawerState = drawerState ,
        onSignOutClicked = onSignOutClicked,
        onDeleteAllClicked = onDeleteAllClicked
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
            topBar = {
                HomeTopBar(
                    scrollBehaviour = scrollBehaviour,
                    onMenuClicked = onMenuClicked,
                    isDateSelected = isDateSelected,
                    onDateReset =onDateReset,
                    onDateSelected =  onDateSelected
                    )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = navigateToWrite) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription ="New Diary Icon" )

                }

            },
            content = {
                padding = it
                when (diaries) {
                    is RequestState.Success -> {
                        HomeContent(
                            paddingValues = it,
                            diaryNotes = diaries.data,
                            onClick = navigateToWriteWithArgs
                        )
                    }
                    is RequestState.Error -> {
                        EmptyPage(
                            title = "Error",
                            subtitle = "${diaries.error.message}"
                        )
                    }
                    is RequestState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {}
                }
            }
        )
        
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    onSignOutClicked:()-> Unit,
    onDeleteAllClicked:()-> Unit,
    content: @Composable ()-> Unit
){
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
                ModalDrawerSheet(
                    content = {
                      Image(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                        painter = painterResource(id =  com.prateektimer.ui.R.drawable.logo),
                        contentDescription = "Logo Image"
                        )
                    NavigationDrawerItem(
                        label = {
                            Row(modifier = Modifier.padding(horizontal = 12.dp)
                            ){
                                Image(
                                    painter = painterResource(id = com.prateektimer.ui.R.drawable.google_logo),
                                    contentDescription = "Google Logo")
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Sign Out")

                            } },
                        selected = false ,
                        onClick = onSignOutClicked)

                        NavigationDrawerItem(
                            label = {
                                Row(modifier = Modifier.padding(horizontal = 12.dp)
                                ){
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete All Icon",
                                        tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Delete All Diaries")

                                } },
                            selected = false ,
                            onClick = onDeleteAllClicked)
                } )
        },
        content = content
    )
}