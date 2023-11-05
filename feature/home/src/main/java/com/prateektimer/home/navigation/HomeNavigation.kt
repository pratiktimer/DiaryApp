package com.prateektimer.home.navigation

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.prateektimer.home.HomeScreen
import com.prateektimer.home.HomeViewModel
import com.prateektimer.ui.components.DisplayAlertDialog
import com.prateektimer.util.Constants
import com.prateektimer.util.Screen
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue

fun NavGraphBuilder.homeRoute(
    navigateToWrite:()-> Unit,
    navigateToWriteWithArgs:(String)-> Unit,
    navigateToAuth: () -> Unit
){
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val context = LocalContext.current
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var signOutDialogOpened by remember{ mutableStateOf(false) }
        var deleteAllDialogOpened by remember{ mutableStateOf(false) }
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
            onDeleteAllClicked ={
                deleteAllDialogOpened = true
            },
            isDateSelected = viewModel.dateIsSelected,
            onDateSelected = { viewModel.getDiaries(zonedDateTime = it) },
            onDateReset = { viewModel.getDiaries() },
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
                    val user = App.create(Constants.APP_ID).currentUser
                    if(user != null){
                        user.logOut()
                        signOutDialogOpened = false
                        withContext(Dispatchers.Main){
                            navigateToAuth()
                        }
                    }
                }
            })

        DisplayAlertDialog(
            title = "Delete All",
            message = "Are you sure you want to permananetly delete all diaries ?" ,
            dialogOpened = deleteAllDialogOpened,
            onDialogClosed = {  deleteAllDialogOpened = false},
            onYesClicked =
            {
                viewModel.deleteAllDiaries(
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "All Diaries Deleted.",
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch { drawerState.close() }
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch { drawerState.close() }
                    }
                )
            })
    }
}