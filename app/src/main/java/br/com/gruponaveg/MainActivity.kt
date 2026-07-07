package br.com.gruponaveg

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
import br.com.gruponaveg.navigation.NavegAppNavHost
import br.com.gruponaveg.ui.theme.NavegAppTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavegAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavegApp()
                }
            }
        }
    }

}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavegApp(
    navController: NavHostController = rememberNavController()
) {
    NavegApp {
        NavegAppNavHost(
            navController = navController
        )
    }
}

@Composable
fun NavegApp(
    content: @Composable () -> Unit
) {
    content()
}
