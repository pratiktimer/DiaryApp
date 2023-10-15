package com.prateektimer.diaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.prateektimer.diaryapp.navigation.Screen
import com.prateektimer.diaryapp.navigation.SetupNavGraph
import com.prateektimer.diaryapp.ui.theme.DiaryAppTheme
import com.prateektimer.diaryapp.util.Constants.APP_ID
import io.realm.kotlin.mongodb.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            DiaryAppTheme {
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
    }
        private fun getStartDestination(): String {
            val user = App.create(APP_ID).currentUser
            return if (user != null && user.loggedIn) Screen.Home.route
            else Screen.Authentication.route
        }
}
