package rs.ruffle

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val scrollState = rememberScrollState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.Top
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(id = R.string.work_in_progress_warning),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )

            SelectSwfUrlOrFile(openSwf)

            // Add spacing between sections
            Spacer(modifier = Modifier.height(16.dp))

            // Display favorites section if there are entries
            val favoriteEntries = SwfFavoritesManager.getFavoriteEntries()
            if (favoriteEntries.isNotEmpty()) {
                FavoritesSection(favoriteEntries, openSwf)
                // Add spacing between sections
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Display bundled demos section
            DemosSection(openSwf)

            // Add spacing between sections
            Spacer(modifier = Modifier.height(8.dp))

            // Display history section if there are entries
            val historyEntries = SwfHistoryManager.getHistoryEntries()
            if (historyEntries.isNotEmpty()) {
                HistorySection(historyEntries, openSwf)
            }

            // Add bottom padding for better scrolling experience
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
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
                "*/*"
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

@Composable
fun DemosSection(openSwf: (Uri) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val gameDemos = DemosManager.getGameDemos()
    val animationDemos = DemosManager.getAnimationDemos()

    if (gameDemos.isNotEmpty() || animationDemos.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.demos_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Games section
            if (gameDemos.isNotEmpty()) {
                DemoCategorySection(
                    titleResId = R.string.demos_games_title,
                    icon = Icons.Filled.PlayArrow,
                    demos = gameDemos,
                    openSwf = { demo ->
                        val uri = DemosManager.extractDemoToFile(context, demo.assetPath)
                        if (uri != null) {
                            Log.d("SelectSwfScreen", "Opening demo game: $uri (${demo.name})")
                            // Explicitly specify intent data to ensure it's properly passed to PlayerActivity
                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                data = uri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } else {
                            // Show error toast
                            android.widget.Toast.makeText(
                                context,
                                "Failed to load game: ${demo.name}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            // Add space between sections
            if (gameDemos.isNotEmpty() && animationDemos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Animations section
            if (animationDemos.isNotEmpty()) {
                DemoCategorySection(
                    titleResId = R.string.demos_animations_title,
                    icon = Icons.Filled.Videocam,
                    demos = animationDemos,
                    openSwf = { demo ->
                        val uri = DemosManager.extractDemoToFile(context, demo.assetPath)
                        if (uri != null) {
                            Log.d("SelectSwfScreen", "Opening demo animation: $uri (${demo.name})")
                            // Explicitly specify intent data to ensure it's properly passed to PlayerActivity
                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                data = uri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } else {
                            // Show error toast
                            android.widget.Toast.makeText(
                                context,
                                "Failed to load animation: ${demo.name}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DemoCategorySection(
    titleResId: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    demos: List<DemosManager.DemoEntry>,
    openSwf: (DemosManager.DemoEntry) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = titleResId),
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = titleResId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 200.dp)
            ) {
                items(demos) { entry ->
                    DemoItem(entry) {
                        openSwf(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun DemoItem(entry: DemosManager.DemoEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = SLIGHTLY_DEEMPHASIZED_ALPHA
                    )
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(id = R.string.open_swf)
                )
            }
        }
    }
}

@Composable
fun FavoritesSection(favoriteEntries: List<SwfFavoriteEntry>, openSwf: (Uri) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(id = R.string.favorites_title),
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.favorites_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = { SwfFavoritesManager.clearFavorites(context) }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(id = R.string.clear_favorites)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (favoriteEntries.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                ) {
                    items(favoriteEntries) { entry ->
                        FavoriteItem(entry) {
                            openSwf(Uri.parse(entry.uri))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItem(entry: SwfFavoriteEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    if (entry.notes.isNotEmpty()) {
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = SLIGHTLY_DEEMPHASIZED_ALPHA
                            )
                        )
                    }
                }
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = R.string.open_swf)
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySection(historyEntries: List<SwfHistoryEntry>, openSwf: (Uri) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = stringResource(id = R.string.history_title),
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.history_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = { SwfHistoryManager.clearHistory(context) }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(id = R.string.clear_history)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (historyEntries.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_history),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                ) {
                    items(historyEntries) { entry ->
                        HistoryItem(entry) {
                            openSwf(Uri.parse(entry.uri))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(entry: SwfHistoryEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = SLIGHTLY_DEEMPHASIZED_ALPHA
                    )
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(id = R.string.open_swf)
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
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
