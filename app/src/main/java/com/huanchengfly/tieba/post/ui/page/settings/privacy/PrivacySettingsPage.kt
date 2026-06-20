package com.huanchengfly.tieba.post.ui.page.settings.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPrivacyManager
import com.huanchengfly.tieba.post.utils.AppPrivacyManager.PrivacyData
import com.huanchengfly.tieba.post.toastShort
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun PrivacySettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    var activeTemplate by remember { mutableStateOf(AppPrivacyManager.getActiveTemplateName()) }
    var privacyData by remember { mutableStateOf(AppPrivacyManager.getPrivacyData()) }

    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }

    var customTemplates by remember { mutableStateOf(AppPrivacyManager.getCustomTemplates()) }

    fun refreshData() {
        privacyData = AppPrivacyManager.getPrivacyData()
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_privacy),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                actions = {
                    IconButton(onClick = {
                        showSaveTemplateDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = stringResource(id = R.string.btn_save_as_template),
                            tint = ExtendedTheme.colors.primary
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Preset Templates
                item {
                    Text(
                        text = stringResource(id = R.string.privacy_section_templates),
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = ExtendedTheme.colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = ExtendedTheme.colors.card,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        // Default Masked Template
                        TemplateItem(
                            name = stringResource(id = R.string.template_default_masked),
                            isSelected = activeTemplate == "default",
                            onClick = {
                                AppPrivacyManager.setActiveTemplateName("default")
                                activeTemplate = "default"
                                refreshData()
                                context.toastShort(context.getString(R.string.template_applied_default))
                            }
                        )

                        Divider(color = ExtendedTheme.colors.divider)

                        // Real Device Template
                        TemplateItem(
                            name = stringResource(id = R.string.template_real_device),
                            isSelected = activeTemplate == "real_device",
                            onClick = {
                                AppPrivacyManager.setActiveTemplateName("real_device")
                                activeTemplate = "real_device"
                                refreshData()
                                context.toastShort(context.getString(R.string.template_applied_real))
                            }
                        )

                        Divider(color = ExtendedTheme.colors.divider)

                        // Secure Sandbox Template
                        TemplateItem(
                            name = stringResource(id = R.string.template_secure_sandbox),
                            isSelected = activeTemplate == "secure",
                            onClick = {
                                AppPrivacyManager.setActiveTemplateName("secure")
                                activeTemplate = "secure"
                                refreshData()
                                context.toastShort(context.getString(R.string.template_applied_secure))
                            }
                        )

                        if (customTemplates.isNotEmpty()) {
                            customTemplates.forEach { template ->
                                Divider(color = ExtendedTheme.colors.divider)
                                TemplateItem(
                                    name = template.name,
                                    isSelected = activeTemplate == template.name,
                                    onClick = {
                                        AppPrivacyManager.setActiveTemplateName(template.name)
                                        activeTemplate = template.name
                                        refreshData()
                                        context.toastShort(context.getString(R.string.template_applied_custom, template.name))
                                    },
                                    onDelete = {
                                        AppPrivacyManager.deleteCustomTemplate(template.name)
                                        customTemplates = AppPrivacyManager.getCustomTemplates()
                                        activeTemplate = AppPrivacyManager.getActiveTemplateName()
                                        refreshData()
                                        context.toastShort(context.getString(R.string.template_deleted, template.name))
                                    }
                                )
                            }
                        }
                    }
                }

                // Section: Customizable Fields
                item {
                    Text(
                        text = stringResource(id = R.string.privacy_section_fields),
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = ExtendedTheme.colors.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = ExtendedTheme.colors.card,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        PrivacyFieldInput(
                            label = "_phone_imei (IMEI)",
                            value = privacyData.phoneImei,
                            onValueChange = {
                                privacyData = privacyData.copy(phoneImei = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "model (设备型号)",
                            value = privacyData.model,
                            onValueChange = {
                                privacyData = privacyData.copy(model = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "brand (设备品牌)",
                            value = privacyData.brand,
                            onValueChange = {
                                privacyData = privacyData.copy(brand = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "board (主板)",
                            value = privacyData.board,
                            onValueChange = {
                                privacyData = privacyData.copy(board = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "_os_version (Android 版本)",
                            value = privacyData.osVersion,
                            onValueChange = {
                                privacyData = privacyData.copy(osVersion = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "cuid (设备唯一标识)",
                            value = privacyData.cuid,
                            onValueChange = {
                                privacyData = privacyData.copy(cuid = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "cuid_galaxy2 (设备标识2)",
                            value = privacyData.cuidGalaxy2,
                            onValueChange = {
                                privacyData = privacyData.copy(cuidGalaxy2 = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "c3_aid (c3_aid 标识)",
                            value = privacyData.c3Aid,
                            onValueChange = {
                                privacyData = privacyData.copy(c3Aid = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "oaid (匿名广告标识符)",
                            value = privacyData.oaid,
                            onValueChange = {
                                privacyData = privacyData.copy(oaid = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "client_id",
                            value = privacyData.clientId,
                            onValueChange = {
                                privacyData = privacyData.copy(clientId = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "net_type",
                            value = privacyData.netType,
                            onValueChange = {
                                privacyData = privacyData.copy(netType = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "android_id",
                            value = privacyData.androidId,
                            onValueChange = {
                                privacyData = privacyData.copy(androidId = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "first_install_time (首次安装时间)",
                            value = privacyData.firstInstallTime,
                            onValueChange = {
                                privacyData = privacyData.copy(firstInstallTime = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "last_update_time (上次更新时间)",
                            value = privacyData.lastUpdateTime,
                            onValueChange = {
                                privacyData = privacyData.copy(lastUpdateTime = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "device_score (设备性能评分)",
                            value = privacyData.deviceScore,
                            onValueChange = {
                                privacyData = privacyData.copy(deviceScore = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "scr_dip (屏幕密度比例)",
                            value = privacyData.scrDip,
                            onValueChange = {
                                privacyData = privacyData.copy(scrDip = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "scr_h (屏幕像素高度)",
                            value = privacyData.scrH,
                            onValueChange = {
                                privacyData = privacyData.copy(scrH = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "scr_w (屏幕像素宽度)",
                            value = privacyData.scrW,
                            onValueChange = {
                                privacyData = privacyData.copy(scrW = it)
                            }
                        )

                        PrivacyFieldInput(
                            label = "z_id (安全获取标识)",
                            value = privacyData.zId,
                            onValueChange = {
                                privacyData = privacyData.copy(zId = it)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (activeTemplate == "default" || activeTemplate == "real_device" || activeTemplate == "secure") {
                                    // Preset templates cannot be directly overwritten with modified custom fields, save as new template
                                    showSaveTemplateDialog = true
                                } else {
                                    // Overwrite custom template
                                    AppPrivacyManager.saveCustomTemplate(activeTemplate, privacyData)
                                    customTemplates = AppPrivacyManager.getCustomTemplates()
                                    context.toastShort(context.getString(R.string.template_saved_success, activeTemplate))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = ExtendedTheme.colors.primary,
                                contentColor = ExtendedTheme.colors.onPrimary
                            )
                        ) {
                            Text(text = stringResource(id = R.string.btn_save_privacy_settings))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text(text = stringResource(id = R.string.title_save_as_template)) },
            text = {
                OutlinedTextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text(text = stringResource(id = R.string.label_template_name)) },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = ExtendedTheme.colors.primary,
                        focusedLabelColor = ExtendedTheme.colors.primary,
                        cursorColor = ExtendedTheme.colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTemplateName.isNotBlank()) {
                            val name = newTemplateName.trim()
                            if (name == "default" || name == "real_device" || name == "secure") {
                                context.toastShort(context.getString(R.string.error_invalid_template_name))
                            } else {
                                AppPrivacyManager.saveCustomTemplate(name, privacyData)
                                AppPrivacyManager.setActiveTemplateName(name)
                                activeTemplate = name
                                customTemplates = AppPrivacyManager.getCustomTemplates()
                                showSaveTemplateDialog = false
                                newTemplateName = ""
                                context.toastShort(context.getString(R.string.template_saved_and_applied, name))
                            }
                        } else {
                            context.toastShort(context.getString(R.string.error_blank_template_name))
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = android.R.string.ok),
                        color = ExtendedTheme.colors.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                        color = ExtendedTheme.colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
fun TemplateItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ExtendedTheme.colors.primary,
                    unselectedColor = ExtendedTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.body1,
                color = if (isSelected) ExtendedTheme.colors.primary else ExtendedTheme.colors.text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.btn_delete_template),
                    tint = ExtendedTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun PrivacyFieldInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = ExtendedTheme.colors.primary,
                focusedLabelColor = ExtendedTheme.colors.primary,
                unfocusedBorderColor = ExtendedTheme.colors.divider,
                textColor = ExtendedTheme.colors.text,
                cursorColor = ExtendedTheme.colors.primary,
                backgroundColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
