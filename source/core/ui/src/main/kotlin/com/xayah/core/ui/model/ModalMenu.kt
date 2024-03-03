package com.xayah.core.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector

data class ActionMenuItem(
    val title: StringResourceToken,
    val icon: ImageVectorToken? = null,
    val color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.LocalContent,
    val backgroundColor: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnPrimary,
    val enabled: Boolean,
    val countdown: Int = 0,
    val dismissOnClick: Boolean = false,
    val secondaryMenu: List<ActionMenuItem>,
    val onClick: (suspend () -> Unit)?,
)

fun getActionMenuReturnItem(onClick: (suspend () -> Unit)? = null) = ActionMenuItem(
    title = StringResourceToken.fromStringId(R.string.word_return),
    icon = ImageVectorToken.fromVector(Icons.Rounded.ArrowBack),
    enabled = true,
    secondaryMenu = listOf(),
    onClick = onClick
)

fun getActionMenuConfirmItem(onClick: suspend () -> Unit) = ActionMenuItem(
    title = StringResourceToken.fromStringId(R.string.confirm),
    icon = ImageVectorToken.fromVector(Icons.Rounded.Warning),
    color = ColorSchemeKeyTokens.Error,
    backgroundColor = ColorSchemeKeyTokens.ErrorContainer,
    enabled = true,
    countdown = 1,
    secondaryMenu = listOf(),
    onClick = onClick
)

fun getActionMenuDeleteItem(onClick: suspend () -> Unit) = ActionMenuItem(
    title = StringResourceToken.fromStringId(R.string.delete),
    icon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
    color = ColorSchemeKeyTokens.Error,
    backgroundColor = ColorSchemeKeyTokens.ErrorContainer,
    enabled = true,
    countdown = 1,
    secondaryMenu = listOf(),
    onClick = onClick
)
