package net.ttiimm.morsecode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ttiimm.morsecode.ui.theme.MorseCodeTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MorseCodeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MessageLayout(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageLayout(modifier: Modifier = Modifier) {
    var messageInput by remember { mutableStateOf("") }
    var messageSending by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 40.dp)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MessageInput(
            message = messageInput,
            onMessageChange = { messageInput = it },
            doSend = {
                messageSending = messageInput;
                messageInput = ""
            },
            modifier = modifier
                .padding(bottom = 32.dp, top = 40.dp)
                .fillMaxWidth()
        )

        Text (
            text = stringResource(R.string.sending, messageSending),
            style = MaterialTheme.typography.displaySmall
        )
    }
}

@Composable
fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    doSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current;
    Column(modifier = modifier) {
        TextField(
            label = { Text(stringResource(R.string.enter_message)) },
            value = message,
            onValueChange = onMessageChange,
            modifier = modifier,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend =  {
                    focusManager.clearFocus();
                    doSend();
                }
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MessageLayoutPreview() {
    MessageLayout()
}
