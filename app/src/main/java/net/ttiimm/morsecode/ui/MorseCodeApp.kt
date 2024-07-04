package net.ttiimm.morsecode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ttiimm.morsecode.R
import net.ttiimm.morsecode.data.Message
import net.ttiimm.morsecode.data.MessageState
import net.ttiimm.morsecode.ui.theme.MorseCodeTheme

val FROM_ME_STATUS = setOf(MessageState.SENDING, MessageState.SENT)
val FROM_ME_SHAPE = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    // point towards left
    bottomStart = 0.dp,
    bottomEnd = 20.dp
)
val FROM_YOU_SHAPE = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    // point towards right
    bottomEnd = 0.dp
)

@Composable
fun MorseCodeApp() {
    Scaffold(
        topBar = { MorseCodeTopBar() },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            MorseCodeScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseCodeTopBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun MorseCodeScreen(
    chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
) {
    val chatUiState by chatViewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5F)
        ) {
            MessageBubbles(
                messages = chatUiState.messages,
                scrollState = scrollState
            )
        }
        Row {
            MessageInput(
                message = chatViewModel.currentMessage,
                onMessageChange = { chatViewModel.updateCurrentMessage(it) },
                doSend = {
                    chatViewModel.onTransmit()
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun MessageBubbles(
    messages: List<Message>,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = scrollState,
    ) {
        items(messages) {
            MessageBubble(
                message = it,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    LaunchedEffect(messages.size) {
        scrollState.scrollToItem(index = messages.size - 1)
    }
}

@Composable
fun MessageBubble(message: Message, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = when (message.status) {
            in FROM_ME_STATUS -> Alignment.Start
            else -> Alignment.End
        },
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
    ) {
        Card (
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = when (message.status) {
                    in FROM_ME_STATUS -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ),
            shape = when (message.status) {
                in FROM_ME_STATUS -> FROM_ME_SHAPE
                else -> FROM_YOU_SHAPE
            }
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier.padding(8.dp)
            )
        }
        Text(
            text = message.datetime.toString(),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = message.status.toString(),
            style = MaterialTheme.typography.labelSmall
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
    val focusManager = LocalFocusManager.current

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
                focusManager.clearFocus()
                doSend()
            }
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun MorseCodeAppPreview() {
    MorseCodeScreen()
}

@Preview(showBackground = true)
@Composable
fun MorseCodeAppDarkThemePreview() {
    MorseCodeTheme(darkTheme = true) {
        MorseCodeScreen()
    }
}
