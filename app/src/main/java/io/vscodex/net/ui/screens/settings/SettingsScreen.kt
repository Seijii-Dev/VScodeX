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

package io.vscodex.net.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import io.vscodex.net.activities.AboutActivity
import io.vscodex.net.app.BaseApplication
import io.vscodex.net.app.strings
import io.vscodex.net.extensions.isNotNull
import io.vscodex.net.extensions.isNull
import io.vscodex.net.extensions.open
import io.vscodex.net.github.User
import io.vscodex.net.github.auth.Api
import io.vscodex.net.resources.R
import io.vscodex.net.ui.navigateSingleTop
import io.vscodex.net.ui.screens.SettingScreens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val navController = rememberNavController()

    var user: User? by remember { mutableStateOf(null) }
    LaunchedEffect(key1 = true) {
        user = Api.getUserInfo()?.user
    }

    NavHost(navController, startDestination = SettingScreens.Default) {
        composable<SettingScreens.Default> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    )
                }
                item {
                    Text(
                        text = stringResource(strings.pref_category_configure),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    )
                }
                item {
                    SettingsCategoryItem(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(strings.pref_configure_general),
                        summary = stringResource(strings.pref_configure_general_summary),
                        accent = Color(0xFF6750A4),
                        shape = PreferenceShape.Top,
                        onClick = { navController.navigateSingleTop(SettingScreens.General) },
                    )
                }
                item {
                    SettingsCategoryItem(
                        icon = Icons.Outlined.Code,
                        title = stringResource(strings.pref_configure_editor),
                        summary = stringResource(strings.pref_configure_editor_summary),
                        accent = Color(0xFF3F6374),
                        shape = PreferenceShape.Middle,
                        onClick = { navController.navigateSingleTop(SettingScreens.Editor) },
                    )
                }
                item {
                    SettingsCategoryItem(
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(strings.pref_configure_file_explorer),
                        summary = stringResource(strings.pref_configure_file_explorer_summary),
                        accent = Color(0xFF4D6B45),
                        shape = PreferenceShape.Middle,
                        onClick = { navController.navigateSingleTop(SettingScreens.File) },
                    )
                }
                item {
                    SettingsCategoryItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "AI Agent Configuration",
                        summary = "Configure models, context, tools, and AI rewrite behavior",
                        accent = Color(0xFF8A4F75),
                        shape = PreferenceShape.Bottom,
                        onClick = { navController.navigateSingleTop(SettingScreens.AI) },
                    )
                }
                item { Spacer(Modifier.size(14.dp)) }
                item {
                    Text(
                        text = "Account and about",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    )
                }
                item {
                    SettingsCategoryItem(
                        icon = Icons.Outlined.AccountCircle,
                        title = if (user.isNull()) stringResource(R.string.login_with_github) else stringResource(R.string.logged_in_as, user!!.username, user!!.name ?: ""),
                        summary = if (user.isNull()) "Connect your GitHub account" else user!!.email ?: "GitHub account connected",
                        accent = MaterialTheme.colorScheme.primary,
                        shape = PreferenceShape.Top,
                        onClick = { if (user.isNull()) Api.startLogin(uriHandler) },
                    )
                }
                item {
                    SettingsCategoryItem(
                        icon = Icons.Outlined.Info,
                        title = "About VSCodeX",
                        summary = "License, version, and project information",
                        accent = MaterialTheme.colorScheme.secondary,
                        shape = PreferenceShape.Bottom,
                        onClick = { context.open(AboutActivity::class.java) },
                    )
                }
            }
        }

        composable<SettingScreens.General> {
            ProvidePreferenceLocals {
                GeneralSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp
                )
            }
        }

        composable<SettingScreens.File> {
            ProvidePreferenceLocals {
                FileSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp
                )
            }
        }

        composable<SettingScreens.Editor> {
            ProvidePreferenceLocals {
                EditorSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp,
                    onNavigateToMonacoEditorSettings = {
                        navController.navigateSingleTop(
                            SettingScreens.MonacoEditor
                        )
                    }
                )
            }
        }

        composable<SettingScreens.MonacoEditor> {
            ProvidePreferenceLocals {
                MonacoEditorSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = { navController.navigateSingleTop(SettingScreens.Editor) }
                )
            }
        }

        composable<SettingScreens.AI> {
            ProvidePreferenceLocals {
                AiSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp
                )
            }
        }
    }
}

object PreferenceShape {
    val Top = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )

    val Middle = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )

    val Bottom = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )

    val Alone = RoundedCornerShape(24.dp)
}


@Composable
private fun SettingsCategoryItem(
    icon: ImageVector,
    title: String,
    summary: String,
    accent: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(3.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
