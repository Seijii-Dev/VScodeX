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

package io.vscodex.net.compose.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import io.vscodex.net.compose.ui.components.AnimatedTab
import io.vscodex.net.compose.ui.graphics.rememberSvgAssetImageBitmap
import io.vscodex.net.core.FileIcons
import io.vscodex.net.ui.screens.editor.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTab(
    files: List<EditorViewModel.OpenedFile>,
    selectedFileIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTabReselected: (Int) -> Unit = {},
    onCloseOthers: (Int) -> Unit = {},
    onCloseAll: () -> Unit = {}
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedFileIndex,
        containerColor = Color.Transparent,
        edgePadding = 4.dp,
        indicator = {},
        divider = {},
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        files.fastForEachIndexed { index, file ->
            val isActive = index == selectedFileIndex
            var isMenuExpanded by remember { mutableStateOf(false) }

            val svgIconPath = FileIcons.getSvgIconForFile(file.file.path)
            val isDefaultIcon = svgIconPath == "files/icons/file.svg"

            AnimatedTab(
                index = index,
                selectedIndex = selectedFileIndex,
                onClick = {
                    if (isActive) {
                        isMenuExpanded = true
                        onTabReselected(index)
                    } else {
                        onTabSelected(index)
                    }
                }
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(start = 6.dp, top = 2.dp, bottom = 2.dp, end = 2.dp)
                            .animateContentSize()
                    ) {
                        // File icon
                        if (isDefaultIcon) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = LocalContentColor.current
                            )
                        } else {
                            Image(
                                bitmap = rememberSvgAssetImageBitmap(svgIconPath),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = file.file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Modified dot
                        AnimatedVisibility(
                            visible = file.isModified,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LocalContentColor.current)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onTabClose(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close ${file.file.name}",
                                modifier = Modifier.size(14.dp),
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(
                            extraSmall = RoundedCornerShape(16.dp)
                        )
                    ) {
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close", fontWeight = FontWeight.Medium) },
                                onClick = { onTabClose(index); isMenuExpanded = false },
                                leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Close Others", fontWeight = FontWeight.Medium) },
                                onClick = { onCloseOthers(index); isMenuExpanded = false }
                            )

                            DropdownMenuItem(
                                text = { Text("Close All", fontWeight = FontWeight.Bold) },
                                onClick = { onCloseAll(); isMenuExpanded = false },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
