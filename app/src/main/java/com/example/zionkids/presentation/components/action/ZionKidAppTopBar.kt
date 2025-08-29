package com.example.zionkids.presentation.components.action

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zionkids.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZionKidAppTopBar(
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    txtLabel: String = "",
    dateTxt: String = "",
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigateUp: () -> Unit = {},
    onReportsClick: () -> Unit = {},   // NEW
    onSignOutClick: () -> Unit = {},   // NEW
) {
    var menuExpanded by remember { mutableStateOf(false) }

       // ---- MENU COLORS (brand or themed) ----
    val menuBg   = MaterialTheme.colorScheme.secondary
    val menuText = MaterialTheme.colorScheme.secondary
    val menuIcon = MaterialTheme.colorScheme.secondary

    val useBrandMenuColors: Boolean = true

//    if (useBrandMenuColors) {
//        // Fixed brand colors
//        menuBg   = Color(0xFF162D0D)            // Deep Green
//        itemText = Color.Black
//        itemIcon = Color.White
//    } else {
//        // Follow Material theme (auto light/dark)
//        menuBg   = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
//        itemText = MaterialTheme.colorScheme.onSurface
//        itemIcon = MaterialTheme.colorScheme.onSurfaceVariant
//    }
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zion_kids_logo),
                    contentDescription = stringResource(R.string.company_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_size))
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = txtLabel.trim(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (dateTxt.isNotBlank()) {
                        Text(
                            text = dateTxt,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },

        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleLeft,
                        contentDescription = stringResource(id = R.string.back_button),
                        tint = menuIcon
                    )
                }
            } else {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
//                        colors = IconButtonDefaults.iconButtonColors(
//                            containerColor = MaterialTheme.colorScheme.secondary
//                        )
                        ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_open_menu),
                            tint = menuIcon
                        )

                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
//                            .background(menuBg)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Reports",
                                    color = menuText
                                ) },
                            leadingIcon = {
                                Icon(Icons.Filled.Assessment, contentDescription = null, tint = menuIcon)
                            },
                            onClick = {
                                menuExpanded = false
                                onReportsClick()
                            },
//                            colors = MenuDefaults.itemColors(
////                                textColor = menuText,
//                                leadingIconColor = menuIcon
//                            )
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Sign out",
                                    color = menuText
                                    )
                                   },
                            leadingIcon = {
                                Icon(Icons.Filled.Logout, contentDescription = null, tint = menuIcon)
                            },
                            onClick = {
                                menuExpanded = false
                                onSignOutClick()
                            },
//                            colors = MenuDefaults.itemColors(
////                                textColor = menuText,
//                                leadingIconColor = menuIcon
//                            )
                        )
                    }
                }
            }
        },

//        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
//            containerColor              = MaterialTheme.colorScheme.secondary,
//            titleContentColor           = MaterialTheme.colorScheme.onSecondary,
//            navigationIconContentColor  = MaterialTheme.colorScheme.onSecondary,
//            actionIconContentColor      = MaterialTheme.colorScheme.onSecondary
//        )        ,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        windowInsets = WindowInsets(0)
    )
}
