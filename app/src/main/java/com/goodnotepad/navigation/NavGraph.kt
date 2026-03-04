package com.goodnotepad.navigation

import androidx.compose.runtime.*
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

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: NoteViewModel
) {
    // Common drawer navigation callbacks
    val navigateToFolders: () -> Unit = {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = true }
        }
    }
    val navigateToAllNotes: () -> Unit = {
        navController.navigate(Routes.ALL_NOTES) {
            popUpTo(Routes.HOME)
        }
    }
    val navigateToFavorites: () -> Unit = {
        navController.navigate(Routes.FAVORITES) {
            popUpTo(Routes.HOME)
        }
    }
    val navigateToTrash: () -> Unit = {
        navController.navigate(Routes.TRASH) {
            popUpTo(Routes.HOME)
        }
    }
    val navigateToSettings: () -> Unit = {
        navController.navigate(Routes.SETTINGS) {
            popUpTo(Routes.HOME)
        }
    }

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
                onNavigateToEditor = { noteId ->
                    navController.navigate(Routes.editor(noteId))
                },
                onNavigateToAllNotes = navigateToAllNotes,
                onNavigateToFavorites = navigateToFavorites,
                onNavigateToTrash = navigateToTrash,
                onNavigateToSettings = navigateToSettings
            )
        }

        // Notes by Folder - Issue #1: pass drawer navigation callbacks
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
                },
                onNavigateToFolders = navigateToFolders,
                onNavigateToAllNotes = navigateToAllNotes,
                onNavigateToFavorites = navigateToFavorites,
                onNavigateToTrash = navigateToTrash,
                onNavigateToSettings = navigateToSettings
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
                },
                onNavigateToFolders = navigateToFolders,
                onNavigateToAllNotes = { },
                onNavigateToFavorites = navigateToFavorites,
                onNavigateToTrash = navigateToTrash,
                onNavigateToSettings = navigateToSettings
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
                },
                onNavigateToFolders = navigateToFolders,
                onNavigateToAllNotes = navigateToAllNotes,
                onNavigateToFavorites = { },
                onNavigateToTrash = navigateToTrash,
                onNavigateToSettings = navigateToSettings
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
                },
                onNavigateToFolders = navigateToFolders,
                onNavigateToAllNotes = navigateToAllNotes,
                onNavigateToFavorites = navigateToFavorites,
                onNavigateToTrash = { },
                onNavigateToSettings = navigateToSettings
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
