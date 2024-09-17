package rs.ruffle

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import rs.ruffle.ui.theme.RuffleTheme

@Composable
fun PanicScreen(message: String) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(align = Alignment.Center),
                style = MaterialTheme.typography.headlineLarge,
                text = "Ruffle Panicked :("
            )
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.Center)
                        .padding(horizontal = 8.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    text = message,
                    softWrap = false
                )
            }
        }
    }
}

@Preview(name = "Panic - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Panic - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PanicScreenPreview() {
    RuffleTheme {
        PanicScreen(
            message = """Error: panicked at core/src/display_object/movie_clip.rs:477:9:
assertion `left == right` failed: Called replace_movie on a clip with LoaderInfo set
  left: Some(LoaderInfoObject(LoaderInfoObject { ptr: 0x31b30a8 }))
 right: None
    at n.wbg.__wbg_new_796382978dfd4fb0 (https://unpkg.com/@ruffle-rs/ruffle/core.ruffle.90db0a0ab193ed0c601b.js:1:83857)
    at ruffle_web.wasm.js_sys::Error::new::hfb561c222a4e70eb (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[12733]:0x98671a)
    at ruffle_web.wasm.core::ops::function::FnOnce::call_once{{vtable.shim}}::h8a2a563fa204b611 (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[9789]:0x9164aa)
    at ruffle_web.wasm.std::panicking::rust_panic_with_hook::h33fe77d38d305ca3 (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[6355]:0x8070ed)
    at ruffle_web.wasm.core::panicking::panic_fmt::hde8b7aa66e2831e1 (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[9511]:0x9071fd)
    at ruffle_web.wasm.core::panicking::assert_failed_inner::hc95b7725cb4077cb (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[4402]:0x73cb5e)
    at ruffle_web.wasm.ruffle_core::display_object::movie_clip::MovieClip::replace_with_movie::haf940b0718ed269c (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[2052]:0x50a035)
    at ruffle_web.wasm.ruffle_core::loader::Loader::movie_loader::{{closure}}::h566c935379317178 (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[1053]:0x2bc268)
    at ruffle_web.wasm.<ruffle_web::navigator::WebNavigatorBackend as ruffle_core::backend::navigator::NavigatorBackend>::spawn_future::{{closure}}::h13f3540dbe40e875 (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[1520]:0x419980)
    at ruffle_web.wasm.wasm_bindgen_futures::queue::Queue::new::{{closure}}::hf37247571cf9bbf7 (wasm://wasm/ruffle_web.wasm-0321683a:wasm-function[3648]:0x6ba342)"""
        )
    }
}
