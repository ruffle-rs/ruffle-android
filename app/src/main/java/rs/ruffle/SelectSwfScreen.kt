package rs.ruffle

import android.content.res.Configuration
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import rs.ruffle.ui.theme.RuffleTheme
import rs.ruffle.ui.theme.SLIGHTLY_DEEMPHASIZED_ALPHA

@Composable
fun BrandBar() {
    Image(
        painter = painterResource(id = R.drawable.ic_logo_dark),
        contentDescription = stringResource(id = R.string.logo_description),
        modifier = Modifier
            .wrapContentSize(align = Alignment.Center)
            .padding(vertical = 75.dp)
    )
}

@Composable
fun SelectSwfRoute(openSwf: (uri: Uri) -> Unit) {
    SelectSwfScreen(
        openSwf = openSwf
    )
}

@Composable
fun SelectSwfScreen(openSwf: (uri: Uri) -> Unit) {
    Scaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    // Keep the content centered if it fits on the screen
                    .heightIn(min = maxHeight)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(align = Alignment.Center)
                ) {
                    BrandBar()
                }
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .wrapContentSize(align = Alignment.Center)
                        .padding(horizontal = 8.dp, vertical = 20.dp),
                    text = stringResource(id = R.string.work_in_progress_warning),
                    textAlign = TextAlign.Center
                )
                SelectSwfUrlOrFile(openSwf)
                SwfListSection(
                    title = R.string.favorites,
                    list = SwfLists.favorites,
                    openSwf = openSwf
                )
                SwfListSection(
                    title = R.string.recently_opened,
                    list = SwfLists.history,
                    openSwf = openSwf,
                    supportingText = { entry ->
                        DateUtils.getRelativeTimeSpanString(entry.time).toString()
                    },
                    headerAction = {
                        val context = LocalContext.current
                        TextButton(onClick = { SwfLists.history.clear(context) }) {
                            Text(text = stringResource(id = R.string.clear))
                        }
                    }
                )
                DemosSection(openSwf)
            }
        }
    }
}

@Composable
private fun SwfListSection(
    @StringRes title: Int,
    list: SwfList,
    openSwf: (uri: Uri) -> Unit,
    supportingText: (entry: SwfEntry) -> String? = { null },
    headerAction: @Composable () -> Unit = {}
) {
    if (list.items.isEmpty()) return
    val context = LocalContext.current
    Section(title, headerAction) {
        for (entry in list.items) {
            SwfListItem(
                name = entry.name,
                supportingText = supportingText(entry),
                onClick = { openSwf(entry.uri) },
                trailingContent = {
                    IconButton(onClick = { list.remove(context, entry.uri) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.remove)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun DemosSection(openSwf: (uri: Uri) -> Unit) {
    Section(R.string.demos) {
        for (demo in Demos.all) {
            SwfListItem(
                name = demo.title,
                supportingText = stringResource(id = demo.kind, demo.author),
                onClick = { openSwf(demo.uri) }
            )
        }
    }
}

@Composable
private fun Section(
    @StringRes title: Int,
    headerAction: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center)
            .width(488.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            headerAction()
        }
        content()
    }
}

@Composable
private fun SwfListItem(
    name: String,
    supportingText: String?,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = supportingText?.let { text ->
            { Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = trailingContent
    )
}

@Composable
private fun SelectSwfUrlOrFile(openSwf: (uri: Uri) -> Unit) {
    val urlState by rememberSaveable(stateSaver = UrlStateSaver) {
        mutableStateOf(UrlState())
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
    ) {
        val submitUrl = {
            if (urlState.isValid) {
                openSwf(Uri.parse(urlState.text))
            } else {
                urlState.enableShowErrors()
            }
        }
        OutlinedTextField(
            value = urlState.text,
            onValueChange = { urlState.text = it },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(align = Alignment.Center)
                .width(488.dp)
                .onFocusChanged { focusState ->
                    urlState.onFocusChange(focusState.isFocused)
                    if (!focusState.isFocused) {
                        urlState.enableShowErrors()
                    }
                },
            label = {
                Text(
                    text = stringResource(id = R.string.url),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            isError = urlState.showErrors(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Go,
                keyboardType = KeyboardType.Uri
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitUrl() }
            ),
            singleLine = true
        )
        urlState.getError()?.let { error -> TextFieldError(textError = error) }

        Button(
            onClick = submitUrl,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(align = Alignment.Center)
                .width(320.dp)
                .padding(top = 28.dp, bottom = 3.dp)
        ) {
            Text(
                text = stringResource(id = R.string.open_url),
                style = MaterialTheme.typography.titleSmall
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(align = Alignment.Center)
                .width(320.dp)
        ) {
            Text(
                text = stringResource(id = R.string.or),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = SLIGHTLY_DEEMPHASIZED_ALPHA
                ),
                modifier = Modifier.paddingFromBaseline(top = 25.dp)
            )
            PickSwfButton(openSwf)
        }
    }
}

@Composable
fun PickSwfButton(onSelect: (uri: Uri) -> Unit) {
    // Not using `GetContent`, because only `OpenDocument` lets us keep access
    // to the picked file, which is needed to reopen it from the history.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            onSelect(it)
        }
    }

    OutlinedButton(
        onClick = {
            launcher.launch(
                // Ideally "application/x-shockwave-flash" would be
                // used here, but Android doesn't recognize many
                // downloaded .swf files as such for some reason... :/
                arrayOf("*/*")
            )
        },
        modifier = Modifier
            .width(320.dp)
            .padding(top = 20.dp, bottom = 24.dp)
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center)
            .width(320.dp)

    ) {
        Text(text = stringResource(id = R.string.select_a_swf))
    }
}

@Composable
fun TextFieldError(textError: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = textError,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Preview(name = "Select SWF - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Select SWF - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SelectSwfScreenPreview() {
    RuffleTheme {
        Surface {
            SelectSwfScreen(
                openSwf = {}
            )
        }
    }
}
