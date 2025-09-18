package com.bitchat.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.bitchat.android.ui.theme.EchoBrushes
import com.bitchat.android.ui.theme.EchoPalette

/**
 * VisualTransformation that styles slash commands in the input field.
 */
class SlashCommandVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val slashCommandRegex = Regex("(/\\w+)(?=\\s|$)")
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0

            slashCommandRegex.findAll(text.text).forEach { match ->
                if (match.range.first > lastIndex) {
                    append(text.text.substring(lastIndex, match.range.first))
                }

                withStyle(
                    style = SpanStyle(
                        color = EchoPalette.BrandBlueDeep,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(match.value)
                }

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.text.length) {
                append(text.text.substring(lastIndex))
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

/**
 * VisualTransformation that styles mentions in the input field.
 */
class MentionVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val mentionRegex = Regex("@([a-zA-Z0-9_]+)")
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0

            mentionRegex.findAll(text.text).forEach { match ->
                if (match.range.first > lastIndex) {
                    append(text.text.substring(lastIndex, match.range.first))
                }

                withStyle(
                    style = SpanStyle(
                        color = EchoPalette.IncomingPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(match.value)
                }

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.text.length) {
                append(text.text.substring(lastIndex))
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

/**
 * VisualTransformation that composes multiple transformations together.
 */
class CombinedVisualTransformation(
    private val transformations: List<VisualTransformation>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        var resultText = text
        transformations.forEach { transformation ->
            resultText = transformation.filter(resultText).text
        }
        return TransformedText(resultText, OffsetMapping.Identity)
    }
}

@Composable
fun MessageInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    modifier: Modifier = Modifier,
    inputEnabled: Boolean = true,
    readOnlyHint: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val sendEnabled = inputEnabled && value.text.isNotBlank()
    val placeholder = when {
        !inputEnabled && !readOnlyHint.isNullOrBlank() -> readOnlyHint
        selectedPrivatePeer != null -> "send a private message…"
        currentChannel != null -> "message ${currentChannel}"
        else -> "type a message…"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surfaceVariant.copy(alpha = 0.9f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        if (inputEnabled) onValueChange(it)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                        TextStyle(
                            color = colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif
                        )
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    enabled = inputEnabled,
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (sendEnabled) onSend()
                    }),
                    visualTransformation = CombinedVisualTransformation(
                        listOf(SlashCommandVisualTransformation(), MentionVisualTransformation())
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }
        }

        val buttonBrush: Brush = EchoBrushes.outgoingBubble()
        val buttonModifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .let { base ->
                if (sendEnabled) {
                    base.background(buttonBrush)
                } else {
                    base.background(colorScheme.surfaceVariant)
                }
            }
            .clickable(
                enabled = sendEnabled,
                role = Role.Button
            ) { onSend() }

        Box(
            modifier = buttonModifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = null,
                tint = if (sendEnabled) Color.White else colorScheme.onSurfaceVariant
            )
        }
    }
}
