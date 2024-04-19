package rs.ruffle

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
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
                "application/x-shockwave-flash"
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
