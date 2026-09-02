package com.savingcoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.savingcoach.app.navigation.NavGraph
import com.savingcoach.app.navigation.Routes
import com.savingcoach.app.ui.theme.SavingCoachTheme
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savingcoach.app.data.repository.ThemePreferences
import com.savingcoach.app.data.repository.AppThemeMode
import javax.inject.Inject
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size

import com.savingcoach.app.data.repository.LanguagePreferences
import com.savingcoach.app.data.repository.AppLanguage
import com.savingcoach.app.ui.localization.AppLocale
import com.savingcoach.app.ui.localization.LocalAppStrings
import androidx.compose.runtime.CompositionLocalProvider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var languagePreferences: LanguagePreferences

    @Inject
    lateinit var notificationHelper: com.savingcoach.app.core.notification.NotificationHelper

    @Inject
    lateinit var authRepository: com.savingcoach.app.data.repository.AuthRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // User denied permission - show explanation if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()
            val language by languagePreferences.language.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val strings = AppLocale.getStrings(language)
            CompositionLocalProvider(LocalAppStrings provides strings) {
                SavingCoachTheme(darkTheme = isDark) {
                    MainScreen(authRepository = authRepository, notificationHelper = notificationHelper)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Default.Dashboard, Routes.Dashboard.route),
    BottomNavItem("Expenses", Icons.Default.Receipt, Routes.Expenses.route),
    BottomNavItem("Challenges", Icons.Default.EmojiEvents, Routes.Challenges.route),
    BottomNavItem("Investment", Icons.Default.TrendingUp, Routes.Investment.route),
    BottomNavItem("Assistant", Icons.AutoMirrored.Filled.Chat, Routes.Chat.route)
)

class CradledBottomBarShape(
    private val cradleRadiusDp: Float = 34f,
    private val cradleDepthDp: Float = 18f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = cradleRadiusDp * density.density
        val d = cradleDepthDp * density.density
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val margin = r * 0.45f

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(centerX - r - margin, 0f)
            cubicTo(
                centerX - r * 0.75f, 0f,
                centerX - r * 0.5f, d,
                centerX, d
            )
            cubicTo(
                centerX + r * 0.5f, d,
                centerX + r * 0.75f, 0f,
                centerX + r + margin, 0f
            )
            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        return Outline.Generic(path)
    }
}

fun Modifier.cradledTopBorder(
    borderColor: Color,
    strokeWidthDp: Float = 1f,
    cradleRadiusDp: Float = 34f,
    cradleDepthDp: Float = 18f
): Modifier = this.drawBehind {
    val r = cradleRadiusDp * density
    val d = cradleDepthDp * density
    val width = size.width
    val centerX = width / 2f
    val margin = r * 0.45f

    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(centerX - r - margin, 0f)
        cubicTo(
            centerX - r * 0.75f, 0f,
            centerX - r * 0.5f, d,
            centerX, d
        )
        cubicTo(
            centerX + r * 0.5f, d,
            centerX + r * 0.75f, 0f,
            centerX + r + margin, 0f
        )
        lineTo(width, 0f)
    }
    drawPath(
        path = path,
        color = borderColor,
        style = Stroke(width = strokeWidthDp * density)
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    authRepository: com.savingcoach.app.data.repository.AuthRepository? = null,
    notificationHelper: com.savingcoach.app.core.notification.NotificationHelper? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val strings = AppLocale.current

    var isUserSignedIn by remember { mutableStateOf(authRepository?.isUserSignedIn() == true) }
    val initialStartDestination = if (isUserSignedIn) Routes.Dashboard.route else "${Routes.Auth.route}?mode=signin"

    var currentInAppNotif by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.savingcoach.app.core.notification.InAppNotification?>(null) }

    androidx.compose.runtime.LaunchedEffect(notificationHelper) {
        notificationHelper?.inAppNotificationFlow?.collect { notif ->
            currentInAppNotif = notif
        }
    }

    val navItems = remember(strings) {
        listOf(
            BottomNavItem(strings.navDashboard, Icons.Default.Dashboard, Routes.Dashboard.route),
            BottomNavItem(strings.navExpenses, Icons.Default.Receipt, Routes.Expenses.route),
            BottomNavItem(strings.navChallenges, Icons.Default.EmojiEvents, Routes.Challenges.route),
            BottomNavItem(strings.navInvestment, Icons.Default.TrendingUp, Routes.Investment.route),
            BottomNavItem(strings.navAssistant, Icons.AutoMirrored.Filled.Chat, Routes.Chat.route)
        )
    }

    val isKeyboardVisible = WindowInsets.isImeVisible
    val hiddenBarRoutes = remember {
        setOf(
            Routes.Auth.route,
            Routes.VerifyEmail.route,
            Routes.ForgotPassword.route,
            Routes.Camera.route,
            Routes.OnboardingAge.route,
            Routes.OnboardingGender.route,
            Routes.OnboardingSalary.route,
            Routes.OnboardingFieldOfWork.route,
            Routes.EditUsername.route,
            Routes.EditAge.route,
            Routes.EditGender.route,
            Routes.EditSalary.route,
            Routes.EditFieldOfWork.route,
            Routes.EditEmail.route,
            Routes.ChangePassword.route,
            Routes.ExportData.route,
            Routes.EditCurrency.route,
            Routes.EditLanguage.route,
            Routes.About.route
        )
    }
    // Show bottom bar for all app screens except full-screen auth/onboarding/camera flows
    val showBottomBar = (currentDestination?.route !in hiddenBarRoutes && currentDestination?.route?.substringBefore("?") !in hiddenBarRoutes)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    val isCenterSelected = currentDestination?.hierarchy?.any {
                        it.route?.startsWith("challenges") == true || it.route?.startsWith("challenge") == true
                    } == true

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.ime.union(NavigationBarDefaults.windowInsets)
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Base Bottom Navigation Bar Surface with Cradle Cutout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(CradledBottomBarShape())
                                .background(MaterialTheme.colorScheme.surface)
                                .cradledTopBorder(
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                    strokeWidthDp = 1f
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                navItems.forEachIndexed { index, item ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route?.substringBefore("?") == item.route || (item.route == Routes.Challenges.route && it.route?.startsWith("challenges") == true)
                                    } == true

                                    if (index == 2) {
                                        // Space reserved for the center cradle
                                        Spacer(modifier = Modifier.weight(1.2f))
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(22.dp),
                                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Center Action Button resting in the Cradle
                        Box(
                            modifier = Modifier
                                .offset(y = (-14).dp)
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isCenterSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    navController.navigate(Routes.Challenges.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = strings.navChallenges,
                                tint = if (isCenterSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                startDestination = initialStartDestination,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // On-screen floating notification banner overlay
        com.savingcoach.app.ui.components.InAppNotificationBanner(
            notification = currentInAppNotif,
            onDismiss = { currentInAppNotif = null },
            onClick = { notif ->
                when (notif.type) {
                    "BUDGET_BREACH" -> navController.navigate(Routes.Expenses.route)
                    "SAVING_MILESTONE", "ABANDONED_CHALLENGE" -> navController.navigate(Routes.Challenges.route)
                    "PORTFOLIO_RISK" -> navController.navigate(Routes.Investment.route)
                    else -> navController.navigate(Routes.Notifications.route)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
