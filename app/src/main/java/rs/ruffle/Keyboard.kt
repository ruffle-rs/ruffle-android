package rs.ruffle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import rs.ruffle.ui.theme.RuffleTheme

val BUTTON_ROWS = arrayOf(
    arrayOf(
        KeyboardButton(keyCode = 49, keyChar = '1'),
        KeyboardButton(keyCode = 50, keyChar = '2'),
        KeyboardButton(keyCode = 51, keyChar = '3'),
        KeyboardButton(keyCode = 52, keyChar = '4'),
        KeyboardButton(keyCode = 53, keyChar = '5'),
        KeyboardButton(keyCode = 54, keyChar = '6'),
        KeyboardButton(keyCode = 55, keyChar = '7'),
        KeyboardButton(keyCode = 56, keyChar = '8'),
        KeyboardButton(keyCode = 57, keyChar = '9'),
        KeyboardButton(keyCode = 48, keyChar = '0')
    ),
    arrayOf(
        KeyboardButton(keyCode = 81, keyChar = 'q', text = "Q"),
        KeyboardButton(keyCode = 87, keyChar = 'w', text = "W"),
        KeyboardButton(keyCode = 69, keyChar = 'e', text = "E"),
        KeyboardButton(keyCode = 82, keyChar = 'r', text = "R"),
        KeyboardButton(keyCode = 84, keyChar = 't', text = "T"),
        KeyboardButton(keyCode = 89, keyChar = 'y', text = "Y"),
        KeyboardButton(keyCode = 85, keyChar = 'u', text = "U"),
        KeyboardButton(keyCode = 73, keyChar = 'i', text = "I"),
        KeyboardButton(keyCode = 79, keyChar = 'o', text = "O"),
        KeyboardButton(keyCode = 80, keyChar = 'p', text = "P")
    ),
    arrayOf(
        KeyboardButton(keyCode = 65, keyChar = 'a', text = "A"),
        KeyboardButton(keyCode = 83, keyChar = 's', text = "S"),
        KeyboardButton(keyCode = 68, keyChar = 'd', text = "D"),
        KeyboardButton(keyCode = 70, keyChar = 'f', text = "F"),
        KeyboardButton(keyCode = 71, keyChar = 'g', text = "G"),
        KeyboardButton(keyCode = 72, keyChar = 'h', text = "H"),
        KeyboardButton(keyCode = 74, keyChar = 'j', text = "J"),
        KeyboardButton(keyCode = 75, keyChar = 'k', text = "K"),
        KeyboardButton(keyCode = 76, keyChar = 'l', text = "L")
    ),
    arrayOf(
        KeyboardButton(keyCode = 90, keyChar = 'z', text = "Z"),
        KeyboardButton(keyCode = 88, keyChar = 'x', text = "X"),
        KeyboardButton(keyCode = 67, keyChar = 'c', text = "C"),
        KeyboardButton(keyCode = 86, keyChar = 'v', text = "V"),
        KeyboardButton(keyCode = 66, keyChar = 'b', text = "B"),
        KeyboardButton(keyCode = 78, keyChar = 'n', text = "N"),
        KeyboardButton(keyCode = 77, keyChar = 'm', text = "M"),
        KeyboardButton(keyCode = 13, keyChar = '\u000D', text = "↵")
    ),
    arrayOf(
        KeyboardButton(keyCode = 17, text = "CTRL"),
        KeyboardButton(keyCode = 18, text = "ALT"),
        KeyboardButton(keyCode = 32, keyChar = ' ', text = "␣"),
        KeyboardButton(
            keyCode = 37,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            text = "Left"
        ),
        KeyboardButton(keyCode = 38, icon = Icons.Filled.KeyboardArrowUp, text = "Up"),
        KeyboardButton(keyCode = 40, icon = Icons.Filled.KeyboardArrowDown, text = "Down"),
        KeyboardButton(
            keyCode = 39,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            text = "Right"
        )
    )
)

data class KeyboardButton(
    val keyCode: Byte,
    val keyChar: Char = '\u0000',
    val text: String = keyChar.toString(),
    val icon: ImageVector? = null
)

class ContextMenuItem(
    val text: String,
    val separatorBefore: Boolean,
    val enabled: Boolean,
    val checked: Boolean,
    val onClick: () -> Unit
)

@Composable
fun OnScreenControls(
    onKeyClick: (code: Byte, char: Char) -> Unit,
    onShowContextMenu: () -> Unit,
    onHideContextMenu: () -> Unit,
    contextMenuItems: List<ContextMenuItem>
) {
    var showKeyboard by rememberSaveable { mutableStateOf(true) }
    val menuHasAnyCheckmark = contextMenuItems.any { it.checked }

    Surface {
        Column(modifier = Modifier.padding(horizontal = 1.dp)) {
            if (showKeyboard) {
                VirtualKeyboard(onKeyClick)
            }

            BottomMenu(
                toggleKeyboard = { showKeyboard = !showKeyboard },
                onShowContextMenu,
                contextMenuItems,
                onHideContextMenu,
                menuHasAnyCheckmark
            )
        }
    }
}

@Composable
private fun BottomMenu(
    toggleKeyboard: () -> Unit,
    onShowContextMenu: () -> Unit,
    contextMenuItems: List<ContextMenuItem>,
    onHideContextMenu: () -> Unit,
    menuHasAnyCheckmark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { toggleKeyboard() },
            contentPadding = PaddingValues(1.dp),
            shape = RectangleShape
        ) {
            Text("⌨")
        }

        Button(
            onClick = { onShowContextMenu() },
            contentPadding = PaddingValues(1.dp),
            shape = RectangleShape
        ) {
            DropdownMenu(
                expanded = contextMenuItems.isNotEmpty(),
                onDismissRequest = { onHideContextMenu() }
            ) {
                contextMenuItems.forEach {
                    if (it.separatorBefore) {
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        enabled = it.enabled,
                        text = {
                            Row {
                                if (menuHasAnyCheckmark) {
                                    if (it.checked) {
                                        Icon(Icons.Filled.Check, "")
                                    } else {
                                        Spacer(
                                            modifier = Modifier.width(
                                                Icons.Filled.Check.defaultWidth
                                            )
                                        )
                                    }
                                }
                                Text(it.text)
                            }
                        },
                        onClick = { it.onClick() }
                    )
                }
            }
            Text("▤")
        }
    }
}

@Composable
private fun VirtualKeyboard(onKeyClick: (code: Byte, char: Char) -> Unit) {
    Column {
        BUTTON_ROWS.forEach {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                it.forEach {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        button = it,
                        onKeyClick
                    )
                }
            }
        }
    }
}

@Composable
fun TextButton(
    modifier: Modifier,
    button: KeyboardButton,
    onKeyClick: (code: Byte, char: Char) -> Unit
) {
    Button(
        modifier = modifier,
        onClick = {
            onKeyClick(button.keyCode, button.keyChar)
        },
        contentPadding = PaddingValues(1.dp),
        shape = RectangleShape
    ) {
        if (button.icon != null) {
            Icon(button.icon, contentDescription = button.text)
        } else {
            Text(text = button.text)
        }
    }
}

@Preview
@Composable
fun OnScreenControlsPreview() {
    RuffleTheme {
        OnScreenControls(
            onKeyClick = { _: Byte, _: Char -> },
            onShowContextMenu = {},
            onHideContextMenu = {},
            contextMenuItems = listOf()
        )
    }
}
