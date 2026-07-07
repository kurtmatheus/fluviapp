package dev.matheus.fluviapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.matheus.fluviapp.navigation.FluviAppNavHost
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FluviAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FluviApp()
                }
            }
        }
    }

}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun FluviApp(
    navController: NavHostController = rememberNavController()
) {
    FluviApp {
        FluviAppNavHost(
            navController = navController
        )
    }
}

@Composable
fun FluviApp(
    content: @Composable () -> Unit
) {
    content()
}
