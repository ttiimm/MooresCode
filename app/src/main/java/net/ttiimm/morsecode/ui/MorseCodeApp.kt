package net.ttiimm.morsecode.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ttiimm.morsecode.R
import net.ttiimm.morsecode.data.CameraChatRepository
import net.ttiimm.morsecode.data.Message
import net.ttiimm.morsecode.data.MessageState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.camera.core.Preview as CameraPreview

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
    var showCameraPreview by remember { mutableStateOf(false) }
    var isShowing by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        isGranted -> showCameraPreview = isGranted && isShowing
    }

    Scaffold(
        topBar = { MorseCodeTopBar(
            onCameraClick = {
                isShowing = !isShowing
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
     },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            MorseCodeScreen(showCameraPreview = showCameraPreview)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseCodeTopBar(
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )

                IconButton(onClick = onCameraClick) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = stringResource(R.string.open_camera),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
fun MorseCodeTopBarPreview() {
    MorseCodeTopBar({})
}

@Composable
fun MorseCodeScreen(
    chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory),
    showCameraPreview: Boolean = false
) {
    val chatUiState by chatViewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    Column {
        if (showCameraPreview) {
            Row {
                CameraReceiver(chatViewModel)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
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
                    chatViewModel.doTransmit()
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MorseCodeScreenPreview() {
    val chatRepository = CameraChatRepository(LocalContext.current)
    val chatViewModel = ChatViewModel(chatRepository)
    MorseCodeScreen(chatViewModel)
}

@Preview(showBackground = true)
@Composable
fun MorseCodeScreenWithCameraPreview() {
    val chatRepository = CameraChatRepository(LocalContext.current)
    val chatViewModel = ChatViewModel(chatRepository)
    MorseCodeScreen(
        chatViewModel = chatViewModel,
        showCameraPreview = true
    )
}

// This snippet is from
// https://medium.com/@deepugeorge2007travel/mastering-camerax-in-jetpack-compose-a-comprehensive-guide-for-android-developers-92ec3591a189
@Composable
fun CameraReceiver(chatViewModel: ChatViewModel) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val preview = CameraPreview.Builder().build()
    val previewView = remember {
        PreviewView(context)
    }
    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
    AndroidView(factory = { previewView })
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
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
