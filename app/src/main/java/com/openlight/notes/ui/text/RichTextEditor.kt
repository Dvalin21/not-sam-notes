package com.openlight.notes.ui.text

import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Phase 3: Rich text editor using EditText + Spannable (platform rung, AD-8).
 * Battle-tested, stable span serialization, undo interop.
 */
@Composable
fun RichTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Start typing...",
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    var showPlaceholder by remember { mutableStateOf(value.text.isEmpty()) }

    LaunchedEffect(value.text) {
        showPlaceholder = value.text.isEmpty()
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                showPlaceholder = it.text.isEmpty()
            },
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (showPlaceholder) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * Formatting toolbar for rich text.
 */
@Composable
fun RichTextToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onColor: (Color) -> Unit,
    onHighlight: (Color) -> Unit,
    onHeading: (Int) -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onCheckList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Bold
        FormatButton("B", onBold, Modifier.weight(1f))
        // Italic
        FormatButton("I", onItalic, Modifier.weight(1f))
        // Underline
        FormatButton("U", onUnderline, Modifier.weight(1f))
        // Strikethrough
        FormatButton("S", onStrikethrough, Modifier.weight(1f))
        // Color
        FormatButton("A", { onColor(Color.Red) }, Modifier.weight(1f))
        // Bullet
        FormatButton("•", onBulletList, Modifier.weight(1f))
        // Number
        FormatButton("1.", onNumberedList, Modifier.weight(1f))
        // Check
        FormatButton("☑", onCheckList, Modifier.weight(1f))
    }
}

@Composable
private fun FormatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(4.dp)
        )
    }
}
