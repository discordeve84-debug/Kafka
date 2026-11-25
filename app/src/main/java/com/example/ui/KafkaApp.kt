package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Logout
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.sharp.AccountTree
import androidx.compose.material.icons.sharp.AutoAwesome
import androidx.compose.material.icons.sharp.DarkMode
import androidx.compose.material.icons.sharp.Description
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.LightMode
import androidx.compose.material.icons.sharp.Mail
import androidx.compose.material.icons.sharp.Save
import androidx.compose.material.icons.sharp.Share
import androidx.compose.material.icons.sharp.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.AudioQueryRecorder
import com.example.audio.KafkaVoiceSpeaker
import com.example.core.WorkspaceSessionStore
import com.example.core.attemptAutoSignIn
import com.example.core.checkPendingGoogleOAuthResult
import com.example.core.signOutUser
import com.example.data.KafkaAiEngine
import com.example.data.KafkaFirestoreRepository
import com.example.document.DocumentGenerator
import com.example.model.AgentMode
import com.example.model.GeneratedArtifact
import com.example.model.OutputFormat
import com.example.model.UserProfile
import com.example.network.GoogleWorkspaceClient
import com.example.network.LiveResearchClient
import com.example.research.ui.ResearchModuleDialog
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_AGENT = "agent_workspace"
private const val ROUTE_RESEARCH = "research_verify"
private const val ROUTE_SKILLS = "skill_hub"
private const val ROUTE_STUDIO = "doc_studio"

private data class KafkaNavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val KafkaTabs = listOf(
    KafkaNavTab(ROUTE_HOME, R.string.nav_home, Icons.Sharp.Home, Icons.Outlined.Home, "nav_home"),
    KafkaNavTab(ROUTE_AGENT, R.string.nav_agent, Icons.Sharp.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_agent"),
    KafkaNavTab(ROUTE_RESEARCH, R.string.nav_research, Icons.Sharp.TravelExplore, Icons.Outlined.TravelExplore, "nav_research"),
    KafkaNavTab(ROUTE_SKILLS, R.string.nav_skills, Icons.Sharp.AccountTree, Icons.Outlined.AccountTree, "nav_skills"),
    KafkaNavTab(ROUTE_STUDIO, R.string.nav_studio, Icons.Sharp.Description, Icons.Outlined.Description, "nav_studio"),
)

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    auth: FirebaseAuth? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var resolvedAuth by remember(auth) {
        mutableStateOf(
            auth ?: runCatching {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth else null
            }.getOrNull()
        )
    }

    val activeAuth = resolvedAuth
    if (activeAuth == null) {
        FirebaseConfigMissingScreen(
            onRetryInit = {
                resolvedAuth = runCatching {
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        FirebaseApp.initializeApp(context)
                    }
                    if (FirebaseApp.getApps(context).isNotEmpty()) Firebase.auth else null
                }.getOrNull()
            }
        )
        return
    }

    var currentUser by remember(activeAuth) { mutableStateOf(activeAuth.currentUser) }
    var autoSignInJob by remember(activeAuth) { mutableStateOf<Job?>(null) }

    DisposableEffect(activeAuth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        activeAuth.addAuthStateListener(listener)
        currentUser = activeAuth.currentUser
        onDispose {
            activeAuth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(activeAuth) {
        val pending = checkPendingGoogleOAuthResult(activeAuth)
        if (pending?.first != null) {
            WorkspaceSessionStore.setAccessToken(pending.second)
            currentUser = pending.first
            return@LaunchedEffect
        }
        if (activeAuth.currentUser == null) {
            autoSignInJob = attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                auth = activeAuth,
                onAuthSuccess = { signedUser ->
                    currentUser = signedUser ?: activeAuth.currentUser
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            auth = activeAuth,
            authScope = scope,
            onBeforeInteractiveSignIn = {
                autoSignInJob?.cancel()
                autoSignInJob = null
            },
            onAuthSuccess = { signedUser ->
                currentUser = signedUser ?: activeAuth.currentUser
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedKafkaShell(
                user = user,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onSignOut = {
                    signOutUser(credentialManager, scope, auth = activeAuth) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun FirebaseConfigMissingScreen(
    onRetryInit: () -> Unit,
) {
    var showCyberGame by remember { mutableStateOf(false) }
    var showResearchModule by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kafka_anime_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimePulseAvatar(
                            drawableRes = R.drawable.img_kafka_anime_icon,
                            contentDescription = stringResource(R.string.cd_kafka_logo),
                            isActive = false,
                            size = 58.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Kafka v1.2",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                SuccubusBatWingCrest(size = 16.dp)
                            }
                            Text(
                                text = "Konfigurasi Firebase belum terpasang",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Mengapa layar ini muncul?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "APK ini dibangun tanpa file app/google-services.json di repositori GitHub, sehingga layanan Google Sign-In, Firestore, dan Firebase AI Logic belum memiliki Project ID & App ID.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cara mengaktifkan fitur cloud penuh pada build GitHub:\n1. Encode app/google-services.json dari AI Studio ke Base64.\n2. Simpan di GitHub Repo → Settings → Secrets and variables → Actions dengan nama GOOGLE_SERVICES_JSON_BASE64.\n3. Jalankan ulang workflow Build Android APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = { showResearchModule = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("standalone_research_module_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Buka Modul Live Search, Jurnal & Verifikasi")
            }

            OutlinedButton(
                onClick = onRetryInit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retry_firebase_init_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Coba Inisialisasi Ulang Firebase")
            }

            OutlinedButton(
                onClick = { showCyberGame = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("offline_cyber_game_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Buka Mini-Game Offline (Cyber Grid 2D)")
            }
        }
    }

    if (showResearchModule) {
        ResearchModuleDialog(onDismiss = { showResearchModule = false })
    }

    if (showCyberGame) {
        KafkaCyberGameDialog(onDismiss = { showCyberGame = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AuthenticatedKafkaShell(
    user: FirebaseUser,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val uid = user.uid
    val viewModel: KafkaViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repo = KafkaFirestoreRepository(app)
                val research = LiveResearchClient()
                val ai = KafkaAiEngine()
                val docGen = DocumentGenerator(app)
                val audioRec = AudioQueryRecorder(app)
                val voiceSpk = KafkaVoiceSpeaker(app)
                KafkaViewModel(
                    repository = repo,
                    researchClient = research,
                    aiEngine = ai,
                    documentGenerator = docGen,
                    audioRecorder = audioRec,
                    voiceSpeaker = voiceSpk,
                    currentUserId = uid,
                    currentUserDisplayName = user.displayName.orEmpty(),
                    currentUserEmail = user.email.orEmpty()
                )
            }
        }
    )

    val workspaceViewModel: WorkspaceViewModel = viewModel(
        key = "workspace_$uid",
        factory = viewModelFactory {
            initializer {
                WorkspaceViewModel(
                    workspaceClient = GoogleWorkspaceClient(),
                    aiEngine = KafkaAiEngine()
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val artifactsState by viewModel.artifactsState.collectAsStateWithLifecycle()
    val execution by viewModel.agentExecution.collectAsStateWithLifecycle()
    val imageStudioState by viewModel.imageStudioState.collectAsStateWithLifecycle()
    val workspaceState by workspaceViewModel.state.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()

    val userProfile = (profileState as? UiState.Success)?.data
    val artifacts = (artifactsState as? UiState.Success)?.data ?: emptyList()
    val latestResearchSummary = remember(messagesState) {
        val msgs = (messagesState as? UiState.Success)?.data.orEmpty()
        msgs.lastOrNull { it.role == "AGENT" && it.content.isNotBlank() }?.content.orEmpty()
    }

    LaunchedEffect(userProfile?.darkTheme) {
        userProfile?.let { onToggleTheme(it.darkTheme) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showProfileSheet by remember { mutableStateOf(false) }
    var showImageStudioDialog by remember { mutableStateOf(false) }
    var showCyberGameDialog by remember { mutableStateOf(false) }
    var showWorkspaceHub by remember { mutableStateOf(false) }
    var workspaceInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var gmailPrefillSubject by remember { mutableStateOf("") }
    var gmailPrefillBody by remember { mutableStateOf("") }
    var researchInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var agentSubView by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }
    LaunchedEffect(workspaceViewModel) {
        workspaceViewModel.toastEvents.collect { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(ROUTE_HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val imeVisible = WindowInsets.isImeVisible

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { navigateToTab(ROUTE_HOME) }
                                    .padding(end = 8.dp)
                            ) {
                                AnimePulseAvatar(
                                    drawableRes = kafkaExpressionDrawable(isRunning = execution.isRunning),
                                    contentDescription = stringResource(R.string.cd_kafka_logo),
                                    isActive = execution.isRunning,
                                    size = 38.dp
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Kafka",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        SuccubusBatWingCrest(size = 14.dp)
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                                            shape = MaterialTheme.shapes.extraSmall,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "v1.2",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (execution.isRunning) "Sedang Meriset..." else "Succubus Dark-Fantasy Studio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (execution.isRunning) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    workspaceInitialTab = 0
                                    gmailPrefillSubject = ""
                                    gmailPrefillBody = ""
                                    showWorkspaceHub = true
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("workspace_hub_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Sharp.Mail,
                                    contentDescription = "Google Workspace Hub",
                                    tint = if (workspaceState.isConnected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(
                                onClick = { showProfileSheet = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("profile_button")
                            ) {
                                val photo = user.photoUrl
                                if (photo != null) {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = "Profil pengguna",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (userProfile?.displayName?.firstOrNull()
                                                    ?: user.displayName?.firstOrNull()
                                                    ?: 'K').toString(),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                }
            },
            bottomBar = {
                if (!useRail && !imeVisible) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            KafkaTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.14f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "nav_icon_scale_${tab.route}"
                                )
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes),
                                                modifier = Modifier.graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                            )
                                            if (selected) {
                                                SuccubusFourPointStar(
                                                    size = 7.dp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.graphicsLayer {
                                                        translationX = 4.dp.toPx()
                                                        translationY = (-2).dp.toPx()
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        KafkaTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    enterTransition = { fadeIn(animationSpec = tween(220)) },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            viewModel = viewModel,
                            userDisplayName = userProfile?.displayName?.ifBlank { null }
                                ?: user.displayName
                                ?: "Peneliti",
                            onOpenChat = { mode ->
                                mode?.let { viewModel.setMode(it) }
                                agentSubView = 0
                                navigateToTab(ROUTE_AGENT)
                            },
                            onOpenResearch = { tabIndex ->
                                researchInitialTab = tabIndex
                                navigateToTab(ROUTE_RESEARCH)
                            },
                            onOpenSkills = { navigateToTab(ROUTE_SKILLS) },
                            onOpenStudio = { navigateToTab(ROUTE_STUDIO) },
                            onOpenWorkspaceDashboard = {
                                agentSubView = 1
                                navigateToTab(ROUTE_AGENT)
                            },
                            onOpenWorkspaceLibrary = {
                                agentSubView = 2
                                navigateToTab(ROUTE_AGENT)
                            },
                            workspaceConnected = workspaceState.isConnected,
                            onOpenGoogleWorkspace = { tab ->
                                workspaceInitialTab = tab
                                gmailPrefillSubject = ""
                                gmailPrefillBody = ""
                                showWorkspaceHub = true
                            }
                        )
                    }
                    composable(ROUTE_AGENT) {
                        AgentWorkspaceScreen(
                            viewModel = viewModel,
                            workspaceSubView = agentSubView,
                            onWorkspaceSubViewChange = { agentSubView = it },
                            onNavigateToDocStudio = { navigateToTab(ROUTE_STUDIO) },
                            onNavigateToVerify = {
                                researchInitialTab = 2
                                navigateToTab(ROUTE_RESEARCH)
                            }
                        )
                    }
                    composable(ROUTE_RESEARCH) {
                        ResearchVerifyScreen(
                            viewModel = viewModel,
                            initialTab = researchInitialTab,
                            onAskAgent = { prompt, mode ->
                                agentSubView = 0
                                navigateToTab(ROUTE_AGENT)
                                viewModel.sendAgentPrompt(prompt, overrideMode = mode)
                            },
                            onNavigateToDocStudio = { navigateToTab(ROUTE_STUDIO) }
                        )
                    }
                    composable(ROUTE_SKILLS) {
                        SkillHubScreen(
                            viewModel = viewModel,
                            onTestSkillInAgent = { skill ->
                                if (!skill.enabled) viewModel.toggleSkill(skill)
                                when (skill.slug) {
                                    "nano-banana-image-studio" -> showImageStudioDialog = true
                                    "cyber-grid-2d-game" -> showCyberGameDialog = true
                                    else -> {
                                        if (skill.slug == "voice-conversation" && !viewModel.autoSpeakEnabled.value) {
                                            viewModel.toggleAutoSpeak()
                                        }
                                        agentSubView = 0
                                        navigateToTab(ROUTE_AGENT)
                                        viewModel.sendAgentPrompt(
                                            "Gunakan skill '${skill.name}' untuk membantu saya. Jelaskan rencana singkatnya dan minta topik spesifik saya.",
                                            overrideMode = AgentMode.AGENT
                                        )
                                    }
                                }
                            },
                            onOpenImageStudio = { showImageStudioDialog = true },
                            onOpenCyberGame = { showCyberGameDialog = true }
                        )
                    }
                    composable(ROUTE_STUDIO) {
                        DocStudioScreen(
                            viewModel = viewModel,
                            onOpenSession = { sessionId ->
                                viewModel.selectSession(sessionId)
                                agentSubView = 0
                                navigateToTab(ROUTE_AGENT)
                            },
                            onUploadToDrive = { artifact ->
                                if (workspaceState.isConnected) {
                                    workspaceViewModel.uploadArtifactToDrive(artifact)
                                } else {
                                    workspaceInitialTab = 1
                                    showWorkspaceHub = true
                                }
                            },
                            onSendViaGmail = { artifact ->
                                workspaceInitialTab = 0
                                gmailPrefillSubject = "Dokumen Kafka: ${artifact.title}"
                                gmailPrefillBody = buildString {
                                    appendLine("Berikut ringkasan dokumen '${artifact.title}' (${artifact.format}):")
                                    appendLine()
                                    appendLine(artifact.summary.ifBlank { "Disusun oleh Kafka AI Research Agent." })
                                }
                                showWorkspaceHub = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ProfileManagementSheetContent(
                user = user,
                profile = userProfile,
                artifacts = artifacts,
                currentDarkTheme = isDarkTheme,
                onSaveProfile = { name, bio, dark, format, autoVerify, lang ->
                    onToggleTheme(dark)
                    viewModel.updateUserProfile(
                        displayName = name,
                        bio = bio,
                        darkTheme = dark,
                        defaultFormat = format,
                        autoVerify = autoVerify,
                        preferredLanguage = lang
                    )
                },
                onShareArtifact = { viewModel.shareArtifact(it) },
                onSignOut = {
                    showProfileSheet = false
                    onSignOut()
                }
            )
        }
    }

    if (showImageStudioDialog) {
        ImageStudioDialog(
            state = imageStudioState,
            onGenerate = { prompt -> viewModel.generateAiImage(prompt) },
            onEdit = { bmp, inst -> viewModel.editAiImage(bmp, inst) },
            onShareBitmap = { bmp -> viewModel.shareGeneratedBitmap(bmp) },
            onDismiss = { showImageStudioDialog = false }
        )
    }

    if (showCyberGameDialog) {
        KafkaCyberGameDialog(
            onDismiss = { showCyberGameDialog = false }
        )
    }

    if (showWorkspaceHub) {
        GoogleWorkspaceHubDialog(
            workspaceViewModel = workspaceViewModel,
            artifacts = artifacts,
            latestResearchSummary = latestResearchSummary,
            initialTab = workspaceInitialTab,
            prefillSendSubject = gmailPrefillSubject,
            prefillSendBody = gmailPrefillBody,
            onDismiss = { showWorkspaceHub = false }
        )
    }
}

@Composable
private fun ProfileManagementSheetContent(
    user: FirebaseUser,
    profile: UserProfile?,
    artifacts: List<GeneratedArtifact>,
    currentDarkTheme: Boolean,
    onSaveProfile: (String, String, Boolean, OutputFormat, Boolean, String) -> Unit,
    onShareArtifact: (GeneratedArtifact) -> Unit,
    onSignOut: () -> Unit,
) {
    var displayName by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName?.ifBlank { null } ?: user.displayName ?: "Peneliti Kafka")
    }
    var bio by remember(profile?.bio) {
        mutableStateOf(profile?.bio ?: "Peneliti & Analis Dokumen Kafka")
    }
    var darkTheme by remember(profile?.darkTheme, currentDarkTheme) {
        mutableStateOf(profile?.darkTheme ?: currentDarkTheme)
    }
    var defaultFormat by remember(profile?.defaultFormat) {
        mutableStateOf(OutputFormat.fromRaw(profile?.defaultFormat ?: OutputFormat.DOCX.name))
    }
    var autoVerify by remember(profile?.autoVerify) {
        mutableStateOf(profile?.autoVerify ?: true)
    }
    var preferredLanguage by remember(profile?.preferredLanguage) {
        mutableStateOf(profile?.preferredLanguage ?: "Indonesia")
    }

    val docxCount = artifacts.count { it.format == OutputFormat.DOCX.name }
    val pdfCount = artifacts.count { it.format == OutputFormat.PDF.name }
    val pptxCount = artifacts.count { it.format == OutputFormat.PPTX.name }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimePulseAvatar(
                drawableRes = R.drawable.img_kafka_succubus_smile,
                contentDescription = stringResource(R.string.cd_anime_mascot),
                isActive = false,
                size = 46.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Profil & Preferensi Kafka",
                        style = MaterialTheme.typography.titleLarge
                    )
                    SuccubusHornCrest(size = 15.dp)
                }
                Text(
                    text = "${user.email.orEmpty()} • v1.2 Debug",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SuccubusGothicDivider()

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nama Tampilan") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_name_input"),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Peran / Bidang Riset") },
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_bio_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Format Dokumen Default",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutputFormat.exportableFormats.forEach { fmt ->
                FilterChip(
                    selected = defaultFormat == fmt,
                    onClick = { defaultFormat = fmt },
                    label = { Text(fmt.extension.uppercase()) }
                )
            }
        }

        Text(
            text = "Bahasa Agen Default",
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Indonesia", "English").forEach { lang ->
                FilterChip(
                    selected = preferredLanguage == lang,
                    onClick = { preferredLanguage = lang },
                    label = { Text(lang) }
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Sharp.DarkMode else Icons.Sharp.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Mode Gelap Succubus Obsidian", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Verifikasi sebelum Ekspor", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoVerify, onCheckedChange = { autoVerify = it })
                }
            }
        }

        Button(
            onClick = {
                onSaveProfile(displayName, bio, darkTheme, defaultFormat, autoVerify, preferredLanguage)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_profile_button"),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Sharp.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Simpan Profil & Preferensi")
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = "RIWAYAT DOKUMEN (${artifacts.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBadge("Word", docxCount, Modifier.weight(1f))
            StatBadge("PDF", pdfCount, Modifier.weight(1f))
            StatBadge("PPTX", pptxCount, Modifier.weight(1f))
        }

        artifacts.take(3).forEach { art ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = art.title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${art.format} • ${art.wordCount} kata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onShareArtifact(art) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Sharp.Share, contentDescription = "Bagikan")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_button"),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.AutoMirrored.Sharp.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sign_out))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatBadge(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
