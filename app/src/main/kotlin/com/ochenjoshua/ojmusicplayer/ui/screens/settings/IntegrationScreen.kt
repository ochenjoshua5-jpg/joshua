/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.ochenjoshua.ojmusicplayer.ui.component.GlassDefaults
import com.ochenjoshua.ojmusicplayer.ui.component.GlassScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.ochenjoshua.ojmusicplayer.LocalPlayerAwareWindowInsets
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.constants.ListenBrainzEnabledKey
import com.ochenjoshua.ojmusicplayer.constants.ListenBrainzTokenKey
import com.ochenjoshua.ojmusicplayer.ui.component.IconButton
import com.ochenjoshua.ojmusicplayer.ui.component.InfoLabel
import com.ochenjoshua.ojmusicplayer.ui.component.PreferenceEntry
import com.ochenjoshua.ojmusicplayer.ui.component.PreferenceGroup
import com.ochenjoshua.ojmusicplayer.ui.component.SwitchPreference
import com.ochenjoshua.ojmusicplayer.ui.component.TextFieldDialog
import com.ochenjoshua.ojmusicplayer.ui.theme.ThemePreviews
import com.ochenjoshua.ojmusicplayer.ui.theme.TestThemeWrapper
import com.ochenjoshua.ojmusicplayer.ui.utils.backToMain
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.ui.settings.SettingsDimensions
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(navController: NavController) {
    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")

    var showListenBrainzTokenEditor = remember { mutableStateOf(false) }

    GlassScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.integration)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = GlassDefaults.topAppBarColors(),
            )
        },
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding()

        Column(
            Modifier
                .padding(top = topPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimensions.ScreenBottomPadding),
        ) {
            PreferenceGroup(title = stringResource(R.string.general)) {
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.discord_integration)) },
                        icon = { Icon(painterResource(R.drawable.discord), null) },
                        onClick = {
                            navController.navigate("settings/discord")
                        },
                        showChevron = true,
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.scrobbling)) {
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.lastfm_integration)) },
                        icon = { Icon(painterResource(R.drawable.token), null) },
                        onClick = {
                            navController.navigate("settings/lastfm")
                        },
                        showChevron = true,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.listenbrainz_scrobbling)) },
                        description = stringResource(R.string.listenbrainz_scrobbling_description),
                        icon = { Icon(painterResource(R.drawable.token), null) },
                        checked = listenBrainzEnabled,
                        onCheckedChange = onListenBrainzEnabledChange,
                    )
                }

                item {
                    PreferenceEntry(
                        title = {
                            Text(
                                if (listenBrainzToken.isBlank()) {
                                    stringResource(
                                        R.string.set_listenbrainz_token,
                                    )
                                } else {
                                    stringResource(R.string.edit_listenbrainz_token)
                                },
                            )
                        },
                        icon = { Icon(painterResource(R.drawable.token), null) },
                        onClick = { showListenBrainzTokenEditor.value = true },
                    )
                }
            }
        }
    }

    if (showListenBrainzTokenEditor.value) {
        TextFieldDialog(
            initialTextFieldValue =
                androidx.compose.ui.text.input
                    .TextFieldValue(listenBrainzToken),
            onDone = { data ->
                onListenBrainzTokenChange(data)
                showListenBrainzTokenEditor.value = false
            },
            onDismiss = { showListenBrainzTokenEditor.value = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = {
                it.isNotEmpty()
            },
            extraContent = {
                InfoLabel(text = stringResource(R.string.listenbrainz_scrobbling_description))
            },
        )
    }
}

@ThemePreviews
@Composable
private fun IntegrationScreenPreview() {
    TestThemeWrapper {
        IntegrationScreen(navController = rememberNavController())
    }
}
