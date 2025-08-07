package com.example.gpt_messenger.uzi

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.gpt_messenger.api.ApiClient
import com.example.gpt_messenger.api.ChatRequest
import com.example.gpt_messenger.api.ChatResponse
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import android.util.Log
import android.widget.EditText
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gpt_messenger.data.DatabaseModule
import com.example.gpt_messenger.data.database.DatabaseProvider
import com.example.gpt_messenger.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

data class Message(val text: String, val isUser: Boolean)
data class Dialog(val id: String, val title: String)

val Context.dataStore by preferencesDataStore("user_prefs")


object UserPreferences {
    val USER_ID_KEY = intPreferencesKey("user_id")

    suspend fun saveUserId(context: Context, userId: Int) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    fun getUserId(context: Context): Flow<Int?> = context.dataStore.data.map { it[USER_ID_KEY] }


    val USERNAME_KEY = stringPreferencesKey("username")
    val PASSWORD_KEY = stringPreferencesKey("password")

    val OldLanguage_KEY = stringPreferencesKey("olglang")
    val NewLanguage_KEY = stringPreferencesKey("newlang")

    val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

    suspend fun saveUsername(context: Context, username: String) {
        context.dataStore.edit { prefs -> prefs[USERNAME_KEY] = username }
    }


    fun getOldLang(context: Context): Flow<String?> =
        context.dataStore.data.map { it[OldLanguage_KEY] }
    suspend fun saveOldLang(context: Context, newLanguage: String) {
        context.dataStore.edit { prefs -> prefs[OldLanguage_KEY] = newLanguage }
    }

    fun getNewLang(context: Context): Flow<String?> =
        context.dataStore.data.map { it[NewLanguage_KEY] }
    suspend fun saveNewLang(context: Context, newLanguage: String) {
        context.dataStore.edit { prefs -> prefs[NewLanguage_KEY] = newLanguage }
    }

    fun getUsername(context: Context): Flow<String?> =
        context.dataStore.data.map { it[USERNAME_KEY] }

    suspend fun savePassword(context: Context, password: String) {
        context.dataStore.edit { prefs -> prefs[PASSWORD_KEY] = password }
    }

    fun getPassword(context: Context): Flow<String?> =
        context.dataStore.data.map { it[PASSWORD_KEY] }

    suspend fun saveDarkTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_THEME_KEY] = isDark }
    }

    fun getDarkTheme(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[DARK_THEME_KEY] ?: false }
}


@Composable
fun MessageCard(message: Message, isDarkTheme: Boolean) {
    val backgroundColor = if (message.isUser) Color(0xFFDCF8C6) else Color.White
    val arrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val textColor = Color.Black

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    modifier = Modifier.widthIn(max = 280.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.text))
                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Копировать"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    var isDarkTheme by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        UserPreferences.getDarkTheme(context).collectLatest {
            isDarkTheme = it
        }
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
        NavHost(navController, startDestination = "login") {
            composable("login") { LoginScreen(navController) }
            composable("main") { MainChatScreen(navController) }
            composable("start") { StartingSettings(navController) }
            composable("settings") {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { newTheme ->
                        isDarkTheme = newTheme
                        scope.launch {
                            UserPreferences.saveDarkTheme(context, newTheme)
                        }
                    },
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = DatabaseProvider.getDatabase(context)
    val userDao = db.userDao()

    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UserPreferences.getUsername(context).collectLatest { savedUsername ->
            if (!savedUsername.isNullOrEmpty()) {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    fun validateInputs(): Boolean {
        if (username.isBlank()) {
            Toast.makeText(context, "Введите имя пользователя", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isBlank() || password.length < 4) {
            Toast.makeText(context, "Пароль должен быть не менее 4 символов", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLoginMode) "Вход" else "Регистрация",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("Имя пользователя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (!validateInputs()) return@Button

                isLoading = true
                scope.launch {
                    if (isLoginMode) {
                        val existingUser = userDao.getUserByUsername(username.trim())
                        if (existingUser == null) {
                            Toast.makeText(context, "Пользователь не зарегистрирован", Toast.LENGTH_SHORT).show()
                        } else if (existingUser.passwordHash == password) {
                            UserPreferences.saveUsername(context, existingUser.username)
                            UserPreferences.saveUserId(context, existingUser.id)

                            navController.navigate("start") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Неверный пароль", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val existingUser = userDao.getUserByUsername(username.trim())
                        if (existingUser != null) {
                            Toast.makeText(context, "Пользователь уже существует", Toast.LENGTH_SHORT).show()
                        } else {
                            val newUser = UserEntity(username = username.trim(), passwordHash = password)
                            val newUserId = userDao.insertUser(newUser).toInt()

                            UserPreferences.saveUsername(context, username.trim())
                            UserPreferences.saveUserId(context, newUserId)

                            Toast.makeText(context, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show()

                            navController.navigate("start") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Загрузка..." else if (isLoginMode) "Войти" else "Зарегистрироваться")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(navController: NavHostController) {
    var dialogs by remember {
        mutableStateOf(
            listOf(
                Dialog("1", "Чат 1"),
                Dialog("2", "Чат 2"),
            )
        )
    }
    var currentDialog by remember { mutableStateOf(dialogs.first()) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUserIdState = remember { mutableStateOf(0) }


    LaunchedEffect(Unit) {
        UserPreferences.getUserId(context).collect { id ->
            currentUserIdState.value = id ?: 0
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Диалоги",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                LazyColumn {
                    items(dialogs) { dialog ->
                        ListItem(
                            modifier = Modifier
                                .clickable {
                                    currentDialog = dialog
                                    scope.launch { drawerState.close() }
                                },
                            headlineContent = { Text(dialog.title) }
                        )
                        Divider()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    modifier = Modifier.padding(16.dp),
                    onClick = {
                        val newId = (dialogs.size + 1).toString()
                        val newDialog = Dialog(newId, "Новый чат $newId")
                        dialogs = dialogs + newDialog
                        currentDialog = newDialog
                        scope.launch { drawerState.close() }
                    }
                ) {
                    Text("Новый чат")
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentDialog.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Выйти") },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        UserPreferences.saveUsername(context, "")
                                        navController.navigate("login") {
                                            popUpTo("main") { inclusive = true }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    menuExpanded = false
                                    navController.navigate("settings")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            ChatScreenContent(
                modifier = Modifier.padding(padding),
                dialogId = currentDialog.id,
                dialogTitle = currentDialog.title,
                isDarkTheme = false,
                currentUserId = currentUserIdState.value
            )
        }
    }
}

@Composable
fun ChatScreenContent(
    modifier: Modifier = Modifier,
    dialogId: String,
    dialogTitle: String,
    isDarkTheme:Boolean,
    currentUserId: Int
) {
    val context = LocalContext.current
    val db = com.example.gpt_messenger.data.DatabaseModule.getDatabase(context)
    val messageDao = db.messageDao()

    val dialogIdInt = dialogId.toIntOrNull() ?: 0

    val messagesFlow = remember(dialogId, currentUserId) {
        messageDao.getMessagesForDialogAndUser(dialogIdInt, currentUserId)
    }
    val ownFlow = UserPreferences.getOldLang(context)
    val newFlow = UserPreferences.getNewLang(context)
    val ownLanguage by ownFlow.collectAsState(initial = "Английский")
    val newLanguage by newFlow.collectAsState(initial = "Китайский")

    val messages by messagesFlow.collectAsState(initial = emptyList())

    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { msgEntity ->
                MessageCard(
                    Message(
                        text = msgEntity.messageText,
                        isUser = msgEntity.isFromUser
                    ),
                    isDarkTheme
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Введите сообщение") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (message.isNotBlank()) {
                    scope.launch {
                        Log.d("CHAT", "Preparing to send message...")
                        val textToSend = message.trim()
                        Log.d("CHAT", "Original message: '$textToSend'")

                        val userMessage = com.example.gpt_messenger.data.entities.MessageEntity(
                            dialogId = dialogIdInt,
                            userId = currentUserId,
                            messageText = textToSend,
                            isFromUser = true
                        )
                        val id = messageDao.insertMessage(userMessage)
                        Log.d("ChatScreen", "Inserted user message with id=$id text='$textToSend'")

                        message = ""
                        val fullMessage = textToSend + ". Отвечай как учитель $newLanguage с 10 годами стажа(Поправляй ошибки). Родной язык ученика: $ownLanguage Если спросят о прошлых сообщениях - импровизируй"

                        Log.d("CHAT", "Full message to API: $fullMessage")




                        sendMessageToApi(fullMessage) { response ->
                            scope.launch {
                                val aiMessage = com.example.gpt_messenger.data.entities.MessageEntity(
                                    dialogId = dialogIdInt,
                                    userId = currentUserId,
                                    messageText = response,
                                    isFromUser = false
                                )
                                messageDao.insertMessage(aiMessage)
                            }
                        }
                    }
                }

            }) {
                Text("Отпр.")
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current

    val usernameFlow = UserPreferences.getUsername(context)
    val passwordFlow = UserPreferences.getPassword(context)

    val oldLangFlow = UserPreferences.getOldLang(context)
    val newLangFlow = UserPreferences.getNewLang(context)

    val oldLanguage by oldLangFlow.collectAsState(initial = "Русский")
    val newLanguage by newLangFlow.collectAsState(initial = "Русский")


    var ownLang by remember { mutableStateOf("") }
    var newLang by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val username by usernameFlow.collectAsState(initial = "Неизвестно")
    val password by passwordFlow.collectAsState(initial = "Неизвестно")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Настройки",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))


            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Темная тема",
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = ownLang,
                onValueChange = { ownLang = it
                    scope.launch {
                        UserPreferences.saveOldLang(context, ownLang.trim())
                    }
                },
                placeholder = { Text("Какой язык уже знаете?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )



            Spacer(Modifier.height(16.dp))
            TextField(
                value = newLang,
                onValueChange = { newLang = it
                    scope.launch {
                        UserPreferences.saveNewLang(context, newLang.trim())
                    }
                },
                placeholder = { Text("Какой язык хотите изучать?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Текущий логин:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = username ?: "Неизвестно",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Текущий пароль:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = password?.let { "*".repeat(it.length.coerceAtLeast(4)) } ?: "Неизвестно",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )



            Spacer(Modifier.height(24.dp))

            Text(
                text = "Родной язык:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = oldLanguage ?: "Неизвестно",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Изучаемый язык:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = newLanguage ?: "Неизвестно",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Назад")
            }
        }
    }

}




fun sendMessageToApi(message: String, onResponse: (String) -> Unit) {

    println("Sending message in test mode: $message")
    val call = ApiClient.api.sendMessage(ChatRequest(message))
    call.enqueue(object : Callback<ChatResponse> {
        override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
            if (response.isSuccessful) {
                onResponse(response.body()?.response ?: "Пустой ответ")
            } else {
                onResponse("Ошибка: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
            onResponse("Ошибка: ${t.localizedMessage}")
        }
    })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartingSettings(
    navController: NavHostController
) {
    val context = LocalContext.current

    val oldLangFlow = UserPreferences.getOldLang(context)
    val newLangFlow = UserPreferences.getNewLang(context)

    val oldLanguage by oldLangFlow.collectAsState(initial = "Русский")
    val newLanguage by newLangFlow.collectAsState(initial = "Английский")


    var ownLang by remember { mutableStateOf("") }
    var newLang by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            TextField(
                value = ownLang,
                onValueChange = { ownLang = it
                    scope.launch {
                        UserPreferences.saveOldLang(context, ownLang.trim())
                    }
                },
                placeholder = { Text("Какой язык уже знаете?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )



            Spacer(Modifier.height(16.dp))
            TextField(
                value = newLang,
                onValueChange = { newLang = it
                    scope.launch {
                        UserPreferences.saveNewLang(context, newLang.trim())
                    }
                },
                placeholder = { Text("Какой язык хотите изучать?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(44.dp))

            Button(
                onClick = {
                    navController.navigate("main")
                          },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDarkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UserPreferences.getDarkTheme(context).collectLatest { isDarkTheme = it }
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(navController)
            }
            composable("main") {
                MainChatScreen(navController)
            }

            composable("start") {
                StartingSettings(navController)
            }

            composable("settings") {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { newTheme ->
                        isDarkTheme = newTheme
                        scope.launch {
                            UserPreferences.saveDarkTheme(context, newTheme)
                        }
                    },
                    navController = navController
                )
            }
        }
    }
}

