package com.bitchat.android.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.bitchat.android.model.BitchatMessage
import java.util.Locale

/**
 * Utility functions for ChatScreen UI components
 * Extracted from ChatScreen.kt for better organization
 */

fun formatMessageContent(
    message: BitchatMessage,
    currentUserNickname: String
): AnnotatedString {
    val content = message.content
    val builder = AnnotatedString.Builder()
    val tokenRegex = "@[A-Za-z0-9_]+|#[A-Za-z0-9_]+".toRegex()
    var currentIndex = 0
    val lowercaseNickname = currentUserNickname.lowercase(Locale.getDefault())

    tokenRegex.findAll(content).forEach { matchResult ->
        if (matchResult.range.first > currentIndex) {
            builder.append(content.substring(currentIndex, matchResult.range.first))
        }

        val token = matchResult.value
        if (token.startsWith("@")) {
            val name = token.drop(1).lowercase(Locale.getDefault())
            val isSelf = name == lowercaseNickname
            builder.pushStyle(
                SpanStyle(
                    fontWeight = if (isSelf) FontWeight.Bold else FontWeight.SemiBold
                )
            )
            builder.append(token)
            builder.pop()
        } else {
            builder.pushStyle(
                SpanStyle(
                    fontWeight = FontWeight.Medium
                )
            )
            builder.append(token)
            builder.pop()
        }

        currentIndex = matchResult.range.last + 1
    }

    if (currentIndex < content.length) {
        builder.append(content.substring(currentIndex))
    }

    return builder.toAnnotatedString()
}

fun formatSystemMessage(content: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    builder.pushStyle(
        SpanStyle(
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium
        )
    )
    builder.append(content)
    builder.pop()
    return builder.toAnnotatedString()
}
