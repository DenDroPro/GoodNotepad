package com.goodnotepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodnotepad.ui.screens.home.AppHeader
import com.goodnotepad.ui.screens.home.AppTitle
import com.goodnotepad.ui.screens.home.DrawerBackground

@Composable
fun AppDrawerContent(
    selectedItem: String = "folders",
    onNavigateToFolders: () -> Unit,
    onNavigateToAllNotes: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = DrawerBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppHeader)
                .padding(24.dp)
        ) {
            Text(
                text = "\u0425\u043e\u0440\u043e\u0448\u0438\u0439 \u0431\u043b\u043e\u043a\u043d\u043e\u0442",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppTitle
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
            label = { Text("\u041f\u0430\u043f\u043a\u0438") },
            selected = selectedItem == "folders",
            onClick = onNavigateToFolders,
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = AppHeader.copy(alpha = 0.5f)
            )
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
            label = { Text("\u0412\u0441\u0435 \u0437\u0430\u043c\u0435\u0442\u043a\u0438") },
            selected = selectedItem == "all_notes",
            onClick = onNavigateToAllNotes
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Star, contentDescription = null) },
            label = { Text("\u0418\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435") },
            selected = selectedItem == "favorites",
            onClick = onNavigateToFavorites
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            label = { Text("\u041a\u043e\u0440\u0437\u0438\u043d\u0430") },
            selected = selectedItem == "trash",
            onClick = onNavigateToTrash
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438") },
            selected = selectedItem == "settings",
            onClick = onNavigateToSettings
        )
    }
}
