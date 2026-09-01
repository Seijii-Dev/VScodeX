/*
 * This file is part of VSCodeX.
 *
 * VSCodeX is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * VSCodeX is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with VSCodeX.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.vscodex.net.ui.screens.editor.components.drawer

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import io.vscodex.net.activities.Editor.LocalEditorDrawerNavController
import io.vscodex.net.activities.SettingsActivity
import io.vscodex.net.activities.TerminalActivity
import io.vscodex.net.app.drawables
import io.vscodex.net.extensions.open
import io.vscodex.net.resources.R
import io.vscodex.net.ui.navigateSingleTop
import io.vscodex.net.ui.screens.EditorDrawerScreens

@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    selectedItemIndex: Int
) {
    val navigationRailItems = listOf(
        stringResource(R.string.files),
        stringResource(R.string.git),
        stringResource(R.string.terminal),
        stringResource(R.string.settings)
    )
    val navRailItemIconsUnselected = listOf(
        Icons.Outlined.Folder,
        ImageVector.vectorResource(drawables.ic_git),
        Icons.Outlined.Terminal,
        Icons.Outlined.Settings
    )
    val navRailItemIconsSelected = listOf(
        Icons.Rounded.Folder,
        ImageVector.vectorResource(drawables.ic_git),
        Icons.Rounded.Terminal,
        Icons.Rounded.Settings
    )

    val context = LocalContext.current
    val navController = LocalEditorDrawerNavController.current

    NavigationBar(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        navigationRailItems.fastForEachIndexed { index, name ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedItemIndex == index) navRailItemIconsSelected[index] else navRailItemIconsUnselected[index],
                        contentDescription = name,
                        modifier = Modifier.size(20.dp),
                    )
                },
                label = { Text(text = name) },
                selected = selectedItemIndex == index,
                onClick = {
                    when (index) {
                        0 -> navController.navigateSingleTop(EditorDrawerScreens.FileExplorer)
                        1 -> navController.navigateSingleTop(EditorDrawerScreens.GitManager)
                        2 -> context.open(TerminalActivity::class.java)
                        3 -> context.open(SettingsActivity::class.java)
                    }
                }
            )
        }
    }
}
