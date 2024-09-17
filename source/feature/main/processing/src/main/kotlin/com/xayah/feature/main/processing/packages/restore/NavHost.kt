package com.xayah.feature.main.processing.packages.restore

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.route.MainRoutes
import com.xayah.feature.main.processing.PageProcessing
import com.xayah.feature.main.processing.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PackagesRestoreProcessingGraph() {
    val localNavController = rememberNavController()
    val viewModel = hiltViewModel<RestoreViewModelImpl>()

    AnimatedNavHost(
        navController = localNavController,
        startDestination = MainRoutes.PackagesRestoreProcessingSetup.route,
    ) {
        composable(MainRoutes.PackagesRestoreProcessing.route) {
            PageProcessing(
                topBarTitleId = { state ->
                    when (state) {
                        OperationState.PROCESSING -> R.string.processing
                        OperationState.DONE -> R.string.restore_completed
                        else -> R.string.restore
                    }
                },
                finishedTitleId = R.string.restore_completed,
                finishedSubtitleId = R.string.args_apps_restored,
                viewModel = viewModel
            )
        }
        composable(MainRoutes.PackagesRestoreProcessingSetup.route) {
            PagePackagesRestoreProcessingSetup(localNavController = localNavController, viewModel = viewModel)
        }
    }
}
