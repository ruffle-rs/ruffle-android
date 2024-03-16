package rs.ruffle

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Destinations {
    const val SELECT_SWF_ROUTE = "select"
}

@Composable
fun RuffleNavHost(
    navController: NavHostController = rememberNavController(),
    openSwf: (uri: Uri) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.SELECT_SWF_ROUTE
    ) {
        composable(Destinations.SELECT_SWF_ROUTE) {
            SelectSwfRoute(
                openSwf = openSwf
            )
        }
    }
}
