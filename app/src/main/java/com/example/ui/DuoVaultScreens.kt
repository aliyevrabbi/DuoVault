package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Deposit
import com.example.data.Vault
import com.example.ui.theme.FloatingNavDark
import com.example.ui.theme.FloatingNavPill
import com.example.ui.theme.IosBackground
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosCardSurface
import com.example.ui.theme.IosDivider
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosSubtleSurface
import com.example.ui.theme.IosTextMuted
import com.example.ui.theme.IosTextPrimary
import com.example.ui.theme.IosTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyCardBlueEnd
import com.example.ui.theme.MyCardBlueStart
import com.example.ui.theme.PartnerCardCrimsonEnd
import com.example.ui.theme.PartnerCardCrimsonStart
import java.text.SimpleDateFormat
import java.util.Locale

private val DepositTimeFormatter = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())

@Composable
fun MainVaultScreen(
    viewModel: VaultViewModel,
    currentUserId: String,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedBottomTab by rememberSaveable { mutableIntStateOf(0) }

    var showDepositDialog by rememberSaveable { mutableStateOf(false) }
    var showPartnerJoinDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    if (selectedBottomTab != 0) {
        BackHandler {
            selectedBottomTab = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
    ) {
        when (val currentState = state) {
            is VaultUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IosBlue)
                }
            }

            is VaultUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = IosCardSurface,
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Bağlantı xətası",
                                style = MaterialTheme.typography.titleLarge,
                                color = IosTextPrimary
                            )
                            Text(
                                text = currentState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IosTextSecondary
                            )
                        }
                    }
                }
            }

            is VaultUiState.Success -> {
                val activeVault = currentState.activeVault ?: Vault(
                    id = "DUO2028",
                    ownerId = currentUserId,
                    memberUids = listOf(currentUserId),
                    inviteCode = "DUO2028",
                    title = "May 2028 Ortaq Hədəf"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .widthIn(max = 600.dp)
                        .align(Alignment.TopCenter)
                ) {
                    IosTopBar(
                        vault = activeVault,
                        vaults = currentState.vaults,
                        onSelectVault = { viewModel.selectVault(it) },
                        onOpenPartnerModal = { showPartnerJoinDialog = true },
                        onSignOut = onSignOut
                    )

                    AnimatedVisibility(
                        visible = currentState.actionMessage != null || currentState.errorBanner != null
                    ) {
                        val isErr = currentState.errorBanner != null
                        val bannerText = currentState.errorBanner ?: currentState.actionMessage.orEmpty()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clickable { viewModel.clearMessages() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isErr) Color(0xFFFFEBEE) else Color(0xFFE8F8EE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isErr) Color(0xFFD32F2F) else Color(0xFF1B7F37),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Bağla",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IosTextSecondary
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedBottomTab) {
                            0 -> WalletHomeTabContent(
                                vault = activeVault,
                                deposits = currentState.deposits,
                                summary = currentState.progressSummary,
                                isBalanceHidden = currentState.isBalanceHidden,
                                selectedCardIndex = currentState.selectedWalletCardIndex,
                                depositAnimTrigger = currentState.lastDepositAnimationTrigger,
                                onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                                onSelectCard = { viewModel.selectWalletCard(it) },
                                onOpenDeposit = { showDepositDialog = true },
                                onOpenPartnerJoin = { showPartnerJoinDialog = true },
                                onOpenSettings = { showSettingsDialog = true }
                            )

                            else -> May2028TimelineTabContent(
                                vault = activeVault,
                                summary = currentState.progressSummary,
                                isBalanceHidden = currentState.isBalanceHidden,
                                onOpenSettings = { showSettingsDialog = true }
                            )
                        }
                    }
                }

                FloatingIosBottomBar(
                    selectedTab = selectedBottomTab,
                    onSelectTab = { selectedBottomTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 14.dp)
                )

                if (showDepositDialog) {
                    AddDepositModal(
                        vault = activeVault,
                        onDismiss = { showDepositDialog = false },
                        onConfirm = { amount, note, forPartner ->
                            viewModel.addDeposit(activeVault, amount, note, forPartner)
                            showDepositDialog = false
                        }
                    )
                }

                if (showPartnerJoinDialog) {
                    PartnerConnectModal(
                        vault = activeVault,
                        onDismiss = { showPartnerJoinDialog = false },
                        onJoinByCode = { code, partnerName ->
                            viewModel.joinVaultByInviteCode(code, partnerName)
                            showPartnerJoinDialog = false
                        }
                    )
                }

                if (showSettingsDialog) {
                    VaultSettingsModal(
                        vault = activeVault,
                        onDismiss = { showSettingsDialog = false },
                        onSave = { title, currency, target, ownerName, partnerName ->
                            viewModel.updateVaultSettings(
                                vault = activeVault,
                                title = title,
                                currency = currency,
                                targetAmount = target,
                                ownerName = ownerName,
                                partnerName = partnerName
                            )
                            showSettingsDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IosTopBar(
    vault: Vault,
    vaults: List<Vault>,
    onSelectVault: (String) -> Unit,
    onOpenPartnerModal: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(62.dp).height(40.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MyCardBlueStart, MyCardBlueEnd)))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vault.ownerName.take(1).uppercase(Locale.getDefault()).ifEmpty { "M" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .offset(x = 22.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PartnerCardCrimsonStart, PartnerCardCrimsonEnd)
                                )
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vault.partnerName.take(1).uppercase(Locale.getDefault()).ifEmpty { "D" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(IosGreen)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "DuoVault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IosTextPrimary
                    )
                    Text(
                        text = if (vault.partnerUid.isNotBlank() || vault.partnerName.isNotBlank()) {
                            "${vault.ownerName} & ${vault.partnerName.ifBlank { "Dostum" }} • Canlı"
                        } else {
                            "Ortaq yığım • Canlı"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = IosTextSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenPartnerModal,
                    shape = CircleShape,
                    color = IosCardSurface,
                    modifier = Modifier
                        .height(40.dp)
                        .border(1.dp, IosDivider, CircleShape)
                        .testTag("top_invite_code_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Dost kodu",
                            tint = IosBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = vault.inviteCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = IosTextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IosCardSurface)
                        .border(1.dp, IosDivider, CircleShape)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Çıxış",
                        tint = IosTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (vaults.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vaults.forEach { item ->
                    FilterChip(
                        selected = item.id == vault.id,
                        onClick = { onSelectVault(item.id) },
                        label = {
                            Text(
                                text = "${item.title} (${item.inviteCode})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletHomeTabContent(
    vault: Vault,
    deposits: List<Deposit>,
    summary: May2028ProgressSummary,
    isBalanceHidden: Boolean,
    selectedCardIndex: Int,
    depositAnimTrigger: Long,
    onToggleBalanceVisibility: () -> Unit,
    onSelectCard: (Int) -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenPartnerJoin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 3D Stitched Obsidian Black Leather Wallet
        item(key = "wallet_3d") {
            Wallet3DCard(
                vault = vault,
                isBalanceHidden = isBalanceHidden,
                selectedCardIndex = selectedCardIndex,
                depositAnimTrigger = depositAnimTrigger,
                onToggleBalanceVisibility = onToggleBalanceVisibility,
                onSelectCard = onSelectCard
            )
        }

        // 2. Clean 2-Button Action Row
        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenDeposit,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("action_add_money"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FloatingNavDark,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pul Əlavə Et",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    onClick = onOpenPartnerJoin,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("action_partner_sync"),
                    shape = RoundedCornerShape(18.dp),
                    color = IosCardSurface,
                    shadowElevation = 1.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = IosTextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dostunu Qoş",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. May 2028 Progress & Daily Countdown Bar (No daily limit pressure!)
        item(key = "may2028_progress_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("may2028_daily_progress_card"),
                shape = RoundedCornerShape(26.dp),
                color = IosCardSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "May 2028 Yığım Barı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IosTextPrimary
                        )
                        Text(
                            text = "${summary.daysRemainingUntilMay2028} gün qaldı",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(IosSubtleSurface)
                            .border(1.dp, IosDivider, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IosHatchedProgressBar(
                            progress = summary.goalProgressFraction,
                            activeColor = IosBlue
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Toplanan / Hədəf",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IosTextSecondary
                            )
                            Row {
                                Text(
                                    text = formatMoney(vault.totalSaved, vault.currency, isBalanceHidden),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = IosTextPrimary
                                )
                                Text(
                                    text = " / ${formatMoney(vault.targetAmount, vault.currency, isBalanceHidden)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = IosTextMuted
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(IosSubtleSurface)
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                            .testTag("open_vault_settings_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hədəfə qalan: ${formatMoney(summary.remainingToGoal, vault.currency, isBalanceHidden)} • Ayarla",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IosTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Hədəfi dəyiş",
                            tint = IosTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 4. Deposit History Header
        item(key = "history_header") {
            Text(
                text = "Yığım Tarixçəsi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IosTextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (deposits.isEmpty()) {
            item(key = "empty_deposits") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = IosCardSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Hələ əmanət yoxdur",
                            style = MaterialTheme.typography.titleMedium,
                            color = IosTextPrimary
                        )
                        Text(
                            text = "Əlinizə pul düşdükcə əlavə edin — dostunuzla canlı sinxronlaşacaq.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IosTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = deposits,
                key = { it.id },
                contentType = { "deposit_row" }
            ) { deposit ->
                DepositHistoryItemCard(
                    deposit = deposit,
                    currency = vault.currency,
                    isOwnerDeposit = deposit.userId == vault.ownerId,
                    isBalanceHidden = isBalanceHidden
                )
            }
        }
    }
}

@Composable
private fun May2028TimelineTabContent(
    vault: Vault,
    summary: May2028ProgressSummary,
    isBalanceHidden: Boolean,
    onOpenSettings: () -> Unit
) {
    val total = vault.totalSaved.coerceAtLeast(1.0)
    val ownerRatio = (vault.ownerSaved / total).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. May 2028 Daily Time Progress Bar & Countdown
        item(key = "may2028_overview") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = IosCardSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "31 MAY 2028 ZAMAN BARI",
                                style = MaterialTheme.typography.labelSmall,
                                color = IosBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${summary.daysRemainingUntilMay2028} Gün Qaldı",
                                style = MaterialTheme.typography.headlineMedium,
                                color = IosTextPrimary
                            )
                        }
                        TextButton(onClick = onOpenSettings) {
                            Text("Hədəfi Dəyiş", color = IosBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    IosHatchedProgressBar(
                        progress = summary.timelineElapsedFraction,
                        activeColor = IosBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            label = "Hədəfə Qalan",
                            value = formatMoney(summary.remainingToGoal, vault.currency, isBalanceHidden),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "Toplam Əmanət",
                            value = "${summary.totalDepositsCount} dəfə",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Friend vs Me Comparison Bar
        item(key = "partner_comparison") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = IosCardSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Dostumla Pay Bölgüsü",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = vault.ownerName.ifBlank { "Mən" },
                                style = MaterialTheme.typography.labelLarge,
                                color = IosBlue
                            )
                            Text(
                                text = formatMoney(vault.ownerSaved, vault.currency, isBalanceHidden),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = vault.partnerName.ifBlank { "Dostum" },
                                style = MaterialTheme.typography.labelLarge,
                                color = PartnerCardCrimsonStart
                            )
                            Text(
                                text = formatMoney(vault.partnerSaved, vault.currency, isBalanceHidden),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(PartnerCardCrimsonStart.copy(alpha = 0.85f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (vault.totalSaved == 0.0) 0.5f else ownerRatio)
                                .fillMaxHeight()
                                .background(IosBlue)
                        )
                    }
                }
            }
        }

        // 3. May 2028 Milestones
        item(key = "milestones") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = IosCardSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "May 2028 Mərhələləri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    listOf(0.25, 0.50, 0.75, 1.0).forEach { pct ->
                        val milestoneAmount = vault.targetAmount * pct
                        val done = vault.totalSaved >= milestoneAmount
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (done) Color(0xFFE8F8EE) else IosSubtleSurface)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (done) IosGreen else IosTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${(pct * 100).toInt()}% Mərhələsi",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IosTextPrimary
                                )
                            }
                            Text(
                                text = formatMoney(milestoneAmount, vault.currency, isBalanceHidden),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (done) IosGreen else IosTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(IosSubtleSurface)
            .border(1.dp, IosDivider, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = IosTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = IosTextPrimary
        )
    }
}

@Composable
private fun DepositHistoryItemCard(
    deposit: Deposit,
    currency: String,
    isOwnerDeposit: Boolean,
    isBalanceHidden: Boolean
) {
    val dateText = remember(deposit.createdAt, deposit.dayKey) {
        deposit.createdAt?.toDate()?.let { DepositTimeFormatter.format(it) } ?: deposit.dayKey
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deposit_item_${deposit.id}"),
        shape = RoundedCornerShape(20.dp),
        color = IosCardSurface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOwnerDeposit) Color(0xFFE8F2FF) else Color(0xFFF3E8FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = deposit.userName.take(1).uppercase(Locale.getDefault()).ifEmpty { "+" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOwnerDeposit) IosBlue else PartnerCardCrimsonStart
                    )
                }

                Column {
                    Text(
                        text = deposit.note.ifBlank { "Ortaq yığım" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IosTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${deposit.userName} • $dateText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = IosTextSecondary
                    )
                }
            }

            Text(
                text = "+${formatMoney(deposit.amount, currency, isBalanceHidden)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = IosGreen
            )
        }
    }
}

@Composable
private fun FloatingIosBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(16.dp, CircleShape)
            .clip(CircleShape),
        shape = CircleShape,
        color = FloatingNavDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavTabItem(
                selected = selectedTab == 0,
                activeIcon = Icons.Filled.AccountBalanceWallet,
                inactiveIcon = Icons.Outlined.AccountBalanceWallet,
                label = "Pulqabı",
                testTag = "nav_tab_wallet",
                onClick = { onSelectTab(0) }
            )
            FloatingNavTabItem(
                selected = selectedTab == 1,
                activeIcon = Icons.Filled.CalendarMonth,
                inactiveIcon = Icons.Outlined.CalendarMonth,
                label = "May 2028",
                testTag = "nav_tab_may2028",
                onClick = { onSelectTab(1) }
            )
        }
    }
}

@Composable
private fun FloatingNavTabItem(
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (selected) FloatingNavPill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun AddDepositModal(
    vault: Vault,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Boolean) -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var forPartner by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = IosCardSurface,
        title = {
            Text(
                text = "Pulqabıya Əmanət Əlavə Et",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 50, 100).forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { amountText = preset.toString() },
                            shape = RoundedCornerShape(12.dp),
                            color = IosSubtleSurface
                        ) {
                            Text(
                                text = "+${vault.currency}$preset",
                                style = MaterialTheme.typography.labelLarge,
                                color = IosBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Məbləğ (${vault.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Qeyd (istəyə bağlı)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_note_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !forPartner,
                        onClick = { forPartner = false },
                        label = { Text(vault.ownerName.ifBlank { "Mənim payım" }) }
                    )
                    FilterChip(
                        selected = forPartner,
                        onClick = { forPartner = true },
                        label = { Text(vault.partnerName.ifBlank { "Dostumun payı" }) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (parsed > 0.0) {
                        onConfirm(parsed, noteText, forPartner)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                modifier = Modifier.testTag("confirm_deposit_button")
            ) {
                Text("Əlavə Et", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = IosTextSecondary)
            }
        }
    )
}

@Composable
private fun PartnerConnectModal(
    vault: Vault,
    onDismiss: () -> Unit,
    onJoinByCode: (String, String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var friendCode by rememberSaveable { mutableStateOf("") }
    var myName by rememberSaveable { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = IosCardSurface,
        title = {
            Text(
                text = "Dostunla Canlı Bağlantı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IosSubtleSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sənin Pulqabı Kodun",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IosTextSecondary
                            )
                            Text(
                                text = vault.inviteCode,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(vault.inviteCode))
                                copied = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("copy_invite_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (copied) "Kopyalandı" else "Kopyala")
                        }
                    }
                }

                HorizontalDivider(color = IosDivider)

                Text(
                    text = "Dostunun kodunu daxil edib onun pulqabısına qoşul:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosTextSecondary
                )

                OutlinedTextField(
                    value = friendCode,
                    onValueChange = { friendCode = it.uppercase(Locale.US) },
                    label = { Text("Dostunun Kodu (məs: DUO2028)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_vault_code_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = myName,
                    onValueChange = { myName = it },
                    label = { Text("Sənin Adın") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_partner_name_input"),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (friendCode.isNotBlank()) {
                        onJoinByCode(friendCode, myName)
                    } else {
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FloatingNavDark),
                modifier = Modifier.testTag("join_vault_submit_button")
            ) {
                Text(if (friendCode.isNotBlank()) "Qoşul" else "Tamam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bağla", color = IosTextSecondary)
            }
        }
    )
}

@Composable
private fun VaultSettingsModal(
    vault: Vault,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(vault.title) }
    var currency by rememberSaveable { mutableStateOf(vault.currency) }
    var targetAmount by rememberSaveable { mutableStateOf(vault.targetAmount.toInt().toString()) }
    var ownerName by rememberSaveable { mutableStateOf(vault.ownerName) }
    var partnerName by rememberSaveable { mutableStateOf(vault.partnerName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = IosCardSurface,
        title = {
            Text(
                text = "May 2028 Hədəf Ayarları",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("₼", "$", "€").forEach { cur ->
                        FilterChip(
                            selected = currency == cur,
                            onClick = { currency = cur },
                            label = { Text("Valyuta: $cur") }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hədəfin Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("May 2028 Yekun Hədəf Məbləği") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Mənim Adım") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = partnerName,
                        onValueChange = { partnerName = it },
                        label = { Text("Dostumun Adı") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tAmt = targetAmount.toDoubleOrNull() ?: vault.targetAmount
                    onSave(title, currency, tAmt, ownerName, partnerName)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
            ) {
                Text("Yadda Saxla", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = IosTextSecondary)
            }
        }
    )
}
