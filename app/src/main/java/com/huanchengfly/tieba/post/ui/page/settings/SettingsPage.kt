package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MoreSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.PrivacySettingsPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.SettingsBackup
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun LeadingIcon(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides ExtendedTheme.colors.primary) {
        content()
        Spacer(modifier = Modifier.width(56.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowAccountItem(
    account: Account?,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    if (account != null) {
        TextPref(
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_now_account, account.nameShow ?: account.name),
            enabled = true,
            onClick = { navigator.navigate(AccountManagePageDestination) },
            leadingIcon = {
                LeadingIcon {
                    Avatar(
                        data = StringUtil.getAvatarUrl(account.portrait),
                        size = Sizes.Small,
                        contentDescription = null,
                        username = account.nameShow ?: account.name
                    )
                }
            },
            modifier = modifier,
        )
    } else {
        TextPref(
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_not_logged_in),
            enabled = true,
            onClick = { navigator.navigate(LoginPageDestination) },
            leadingIcon = {
                LeadingIcon {
                    AvatarIcon(
                        icon = Icons.Rounded.AccountCircle,
                        size = Sizes.Small,
                        contentDescription = stringResource(id = R.string.title_new_account),
                        color = ExtendedTheme.colors.onChip,
                        backgroundColor = ExtendedTheme.colors.chip,
                    )
                }
            },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun SettingsPage(
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        // Provide snackbar host for import/export feedback.
        val scaffoldState = androidx.compose.material.rememberScaffoldState()
        val snackbarHostState = scaffoldState.snackbarHostState

        var pendingExportJson by remember { mutableStateOf<String?>(null) }
        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            val json = pendingExportJson
            pendingExportJson = null
            if (uri == null || json == null) return@rememberLauncherForActivityResult

            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_export_success))
                }
            }.onFailure {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_export_failed))
                }
            }
        }

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("empty file")
                    SettingsBackup.importAndOverwrite(context, json)
                }.onSuccess {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_import_success))
                }.onFailure {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_import_failed))
                }
            }
        }

        MyScaffold(
            scaffoldState = scaffoldState,
            backgroundColor = Color.Transparent,
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_settings),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                        )
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    }
                )
            },
        ) {
            PrefsScreen(
                dataStore = context.dataStore,
                dividerThickness = 0.dp,
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
            ) {
                prefsItem {
                    NowAccountItem(account = LocalAccount.current)
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_block_settings),
                        summary = stringResource(id = R.string.summary_block_settings),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_settings_block),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(BlockSettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_custom),
                        summary = stringResource(id = R.string.summary_settings_custom),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_brush_black_24dp),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(CustomSettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_read_habit),
                        summary = stringResource(id = R.string.summary_settings_habit),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_dashboard_customize_black_24),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(HabitSettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_privacy),
                        summary = stringResource(id = R.string.summary_settings_privacy),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_shield),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(PrivacySettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_export_settings),
                        summary = stringResource(id = R.string.summary_export_settings),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = Icons.Rounded.Download,
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = {
                            coroutineScope.launch {
                                runCatching {
                                    val json = SettingsBackup.export(context)
                                    pendingExportJson = json
                                    exportLauncher.launch("tieba-lite-settings.json")
                                }.onFailure {
                                    snackbarHostState.showSnackbar(context.getString(R.string.toast_export_failed))
                                }
                            }
                        }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_import_settings),
                        summary = stringResource(id = R.string.summary_import_settings),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = Icons.Rounded.UploadFile,
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_more),
                        summary = stringResource(id = R.string.summary_settings_more),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_more_horiz_black_24),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = {
                            navigator.navigate(MoreSettingsPageDestination)
                        }
                    )
                }
            }
        }
    }
}
