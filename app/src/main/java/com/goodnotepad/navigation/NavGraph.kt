package com.goodnotepad.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.goodnotepad.ui.NoteViewModel
import com.goodnotepad.ui.screens.allnotes.NoteListScreen
import com.goodnotepad.ui.screens.allnotes.NoteListType
import com.goodnotepad.ui.screens.editor.EditorScreen
import com.goodnotepad.ui.screens.home.HomeScreen
import com.goodnotepad.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: NoteViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNotes = { folderId ->
                    navController.navigate(Routes.notes(folderId))
                },
                onNavigateToAllNotes = {
                    navController.navigate(Routes.ALL_NOTES)
                },
                onNavigateToFavorites = {
                    navController.navigate(Routes.FAVORITES)
                },
                onNavigateToTrash = {
                    navController.navigate(Routes.TRASH)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        // Notes by Folder
        composable(
            route = Routes.NOTES,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: 0L
            var folderName by remember { mutableStateOf("Папка") }

            LaunchedEffect(folderId) {
                val folder = viewModel.getFolderById(folderId)
                folderName = folder?.name ?: "Папка"
            }

            NoteListScreen(
                viewModel = viewModel,
                listType = NoteListType.FOLDER,
                folderId = folderId,
                title = folderName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { noteId ->
                    navController.navigate(Routes.editor(noteId))
                }
            )
        }

        // All Notes
        composable(Routes.ALL_NOTES) {
            NoteListScreen(
                viewModel = viewModel,
                listType = NoteListType.ALL,
                title = "Все заметки",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { noteId ->
                    navController.navigate(Routes.editor(noteId))
                }
            )
        }

        // Favorites
        composable(Routes.FAVORITES) {
            NoteListScreen(
                viewModel = viewModel,
                listType = NoteListType.FAVORITES,
                title = "Избранное",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { noteId ->
                    navController.navigate(Routes.editor(noteId))
                }
            )
        }

        // Trash
        composable(Routes.TRASH) {
            NoteListScreen(
                viewModel = viewModel,
                listType = NoteListType.TRASH,
                title = "Корзина",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { noteId ->
                    navController.navigate(Routes.editor(noteId))
                }
            )
        }

        // Editor
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            EditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
