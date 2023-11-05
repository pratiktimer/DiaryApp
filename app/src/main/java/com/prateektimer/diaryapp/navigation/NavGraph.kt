package com.prateektimer.diaryapp.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.prateektimer.home.navigation.homeRoute
import com.prateektimer.navigation.authenticationRoute
import com.prateektimer.util.Screen
import com.prateektimer.write.navigation.writeRoute

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

