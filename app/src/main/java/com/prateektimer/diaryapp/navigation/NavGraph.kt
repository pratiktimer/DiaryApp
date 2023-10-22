package com.prateektimer.diaryapp.navigation


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prateektimer.diaryapp.model.Diary
import com.prateektimer.diaryapp.model.Mood
import com.prateektimer.diaryapp.presentation.components.DisplayAlertDialog
import com.prateektimer.diaryapp.presentation.screens.auth.AuthenticationScreen
import com.prateektimer.diaryapp.presentation.screens.auth.AuthenticationViewModel
import com.prateektimer.diaryapp.presentation.screens.home.HomeScreen
import com.prateektimer.diaryapp.presentation.screens.home.HomeViewModel
import com.prateektimer.diaryapp.presentation.screens.write.WriteScreen
import com.prateektimer.diaryapp.presentation.screens.write.WriteViewModel
import com.prateektimer.diaryapp.util.Constants.APP_ID
import com.prateektimer.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
    onDataLoaded: () -> Unit
){
    NavHost(
        startDestination = startDestination,
        navController = navController,
    ) {
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            },
            onDataLoaded = onDataLoaded
        )
        homeRoute(navigateToWrite =
        {
            navController.navigate(Screen.Write.route)
        },
            navigateToWriteWithArgs = {
                navController.navigate(Screen.Write.passDiaryId(it.toString()))
            },
            navigateToAuth = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            })
        writeRoute(onBackPressed = {
            navController.popBackStack()
        })
      }
}
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.authenticationRoute(
    navigateToHome: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Authentication.route) {
        val viewModel: AuthenticationViewModel = viewModel()
        val authenticated by viewModel.authenticated
        val loadingState by viewModel.loadingState
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }

        AuthenticationScreen(
            authenticated = authenticated,
            loadingState = loadingState,
            oneTapState = oneTapState,
            messageBarState = messageBarState,
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(true)
            },
            onSuccessfulFirebaseSignIn = { tokenId ->
                viewModel.signInWithMongoAtlas(
                    tokenId = tokenId,
                    onSuccess = {
                        messageBarState.addSuccess("Successfully Authenticated!")
                        viewModel.setLoading(false)
                    },
                    onError = {
                        messageBarState.addError(it)
                        viewModel.setLoading(false)
                    }
                )
            },
            onFailedFirebaseSignIn = {
                messageBarState.addError(it)
                viewModel.setLoading(false)
            },
            onDialogDismissed = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)
            },
            navigateToHome = navigateToHome
        )
    }
}


fun NavGraphBuilder.homeRoute(
    navigateToWrite:()-> Unit,
    navigateToWriteWithArgs:(String)-> Unit,
    navigateToAuth: () -> Unit
){
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = viewModel()
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var signOutDialogOpened by remember{mutableStateOf(false) }
        HomeScreen(
            diaries = diaries,
            drawerState = drawerState,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                               }
                            },
            onSignOutClicked =
            {
            signOutDialogOpened = true
            },
            navigateToWrite = navigateToWrite,
            navigateToWriteWithArgs = navigateToWriteWithArgs
        )
        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?" ,
            dialogOpened = signOutDialogOpened,
            onDialogClosed = {  signOutDialogOpened = false},
            onYesClicked =
            {
                scope.launch (Dispatchers.IO)
                {
                    val user = App.create(APP_ID).currentUser
                    if(user != null){
                        user.logOut()
                        signOutDialogOpened = false
                        withContext(Dispatchers.Main){
                            navigateToAuth()
                        }
                    }
                }
            })
    }
}

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

        val viewModel: WriteViewModel = viewModel()
        val uiState = viewModel.uiState
        val pagerState = rememberPagerState(pageCount = { Mood.values().size })
        val pageNumber by remember{ derivedStateOf { pagerState.currentPage }}
        val context = LocalContext.current

        LaunchedEffect(key1 = uiState){
            Log.d("SelectedDiary", "${uiState.selectedDiaryId}")
        }
        WriteScreen(
            uiState = uiState,
            pagerState = pagerState,
            onBackPressed = onBackPressed,
            onDeleteConfirmed = {
                viewModel.deleteDiary(
                    onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                        onBackPressed()
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            it,
                            Toast.LENGTH_SHORT
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
            }
       )
    }
}