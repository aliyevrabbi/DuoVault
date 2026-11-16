package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Deposit
import com.example.data.Vault
import com.example.data.VaultRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class May2028ProgressSummary(
    val daysRemainingUntilMay2028: Int = 563,
    val totalDaysInPlan: Int = 600,
    val elapsedDays: Int = 1,
    val timelineElapsedFraction: Float = 0.08f,
    val goalProgressFraction: Float = 0f,
    val remainingToGoal: Double = 20000.0,
    val totalDepositsCount: Int = 0
)

sealed interface VaultUiState {
    data object Loading : VaultUiState
    data class Success(
        val vaults: List<Vault>,
        val activeVault: Vault?,
        val deposits: List<Deposit>,
        val progressSummary: May2028ProgressSummary,
        val isBalanceHidden: Boolean,
        val selectedWalletCardIndex: Int,
        val lastDepositAnimationTrigger: Long,
        val actionMessage: String?,
        val errorBanner: String?
    ) : VaultUiState
    data class Error(val message: String) : VaultUiState
}

class VaultViewModel(
    private val repository: VaultRepository,
    private val currentUserId: String,
    private val currentUserDisplayName: String
) : ViewModel() {

    private val selectedVaultId = MutableStateFlow<String?>(null)
    private val isBalanceHidden = MutableStateFlow(false)
    private val selectedWalletCardIndex = MutableStateFlow(0)
    private val lastDepositAnimationTrigger = MutableStateFlow(0L)
    private val actionMessage = MutableStateFlow<String?>(null)
    private val errorBanner = MutableStateFlow<String?>(null)
    private var autoCreatedDefaultVault = false

    private val localControlFlow = combine(
        selectedVaultId,
        isBalanceHidden,
        selectedWalletCardIndex,
        lastDepositAnimationTrigger,
        combine(actionMessage, errorBanner) { msg, err -> msg to err }
    ) { vaultId, hidden, cardIndex, animTrigger, (msg, err) ->
        LocalControls(vaultId, hidden, cardIndex, animTrigger, msg, err)
    }

    val uiState: StateFlow<VaultUiState> = combine(
        repository.observeUserVaults(currentUserId),
        repository.observeUserDeposits(currentUserId),
        localControlFlow
    ) { vaults, allDeposits, controls ->
        if (vaults.isEmpty() && !autoCreatedDefaultVault) {
            autoCreatedDefaultVault = true
            createDefaultSharedVault()
        }

        val sortedVaults = vaults.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        val activeVault = sortedVaults.firstOrNull { it.id == controls.vaultId } ?: sortedVaults.firstOrNull()

        val vaultDeposits = if (activeVault != null) {
            allDeposits
                .filter { it.vaultId == activeVault.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        } else {
            emptyList()
        }

        val summary = computeMay2028Summary(activeVault, vaultDeposits)

        VaultUiState.Success(
            vaults = sortedVaults,
            activeVault = activeVault,
            deposits = vaultDeposits,
            progressSummary = summary,
            isBalanceHidden = controls.hidden,
            selectedWalletCardIndex = controls.cardIndex,
            lastDepositAnimationTrigger = controls.animTrigger,
            actionMessage = controls.msg,
            errorBanner = controls.err
        ) as VaultUiState
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing vault streams", rootError)
            emit(VaultUiState.Error(rootError.message ?: "Məlumatları yükləmək mümkün olmadı"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = VaultUiState.Loading
        )

    private fun createDefaultSharedVault() {
        viewModelScope.launch {
            val shortSuffix = currentUserId.take(4).uppercase(Locale.US)
                .replace(Regex("[^A-Z0-9]"), "8")
                .padEnd(4, '8')
            val defaultCode = "DUO$shortSuffix"
            val ownerLabel = currentUserDisplayName.ifBlank { "Mən" }
            repository.createVault(
                vaultId = defaultCode,
                title = "May 2028 Ortaq Hədəf",
                currency = "₼",
                leatherTheme = "black",
                targetAmount = 20000.0,
                dailyTarget = 35.0,
                ownerName = ownerLabel
            )
        }
    }

    fun toggleBalanceVisibility() {
        isBalanceHidden.value = !isBalanceHidden.value
    }

    fun selectWalletCard(index: Int) {
        selectedWalletCardIndex.value = index
    }

    fun selectVault(vaultId: String) {
        selectedVaultId.value = vaultId
    }

    fun clearMessages() {
        actionMessage.value = null
        errorBanner.value = null
    }

    fun addDeposit(
        vault: Vault,
        amount: Double,
        note: String,
        forPartnerOnBehalf: Boolean = false
    ) {
        if (amount <= 0.0) {
            errorBanner.value = "Məbləğ 0-dan böyük olmalıdır"
            return
        }
        viewModelScope.launch {
            val senderName = if (forPartnerOnBehalf) {
                vault.partnerName.ifBlank { "Dostum" }
            } else if (currentUserId == vault.ownerId) {
                vault.ownerName.ifBlank { currentUserDisplayName.ifBlank { "Mən" } }
            } else {
                vault.partnerName.ifBlank { currentUserDisplayName.ifBlank { "Dostum" } }
            }

            val result = repository.addDeposit(
                vault = vault,
                amount = amount,
                note = note.ifBlank { "Ortaq yığım" },
                userName = senderName,
                forPartnerOnBehalf = forPartnerOnBehalf
            )
            result.onSuccess {
                lastDepositAnimationTrigger.value = System.currentTimeMillis()
                actionMessage.value = "+${vault.currency}${String.format(Locale.US, "%.2f", amount)} pulqabıya əlavə olundu!"
            }.onFailure { e ->
                errorBanner.value = e.localizedMessage ?: "Əmanət əlavə edilə bilmədi"
            }
        }
    }

    fun joinVaultByInviteCode(inviteCode: String, partnerName: String) {
        val clean = inviteCode.trim().uppercase(Locale.US)
        if (clean.length < 4) {
            errorBanner.value = "Dəvət kodunu düzgün daxil edin (məs: DUO2028)"
            return
        }
        viewModelScope.launch {
            val myName = partnerName.trim().ifBlank { currentUserDisplayName.ifBlank { "Dost 2" } }
            val result = repository.joinVaultByCode(clean, myName)
            result.onSuccess { joinedId ->
                selectedVaultId.value = joinedId
                actionMessage.value = "$joinedId ortaq pulqabına qoşuldunuz!"
            }.onFailure {
                errorBanner.value = "Qoşulmaq mümkün olmadı: kodu yoxlayın və ya pulqabı artıq 2 nəfərlikdir."
            }
        }
    }

    fun updateVaultSettings(
        vault: Vault,
        title: String,
        currency: String,
        targetAmount: Double,
        ownerName: String,
        partnerName: String
    ) {
        viewModelScope.launch {
            val result = repository.updateVaultSettings(
                vaultId = vault.id,
                title = title,
                currency = currency,
                leatherTheme = "black",
                targetAmount = targetAmount,
                dailyTarget = vault.dailyTarget.coerceAtLeast(1.0),
                ownerName = ownerName,
                partnerName = partnerName
            )
            result.onSuccess {
                actionMessage.value = "Pulqabı ayarları yeniləndi"
            }.onFailure { e ->
                errorBanner.value = e.localizedMessage ?: "Ayarlar yenilənmədi"
            }
        }
    }

    private fun computeMay2028Summary(
        vault: Vault?,
        deposits: List<Deposit>
    ): May2028ProgressSummary {
        val now = Calendar.getInstance()
        val may2028 = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2028, Calendar.MAY, 31, 23, 59, 59)
        }
        val startJourney = Calendar.getInstance(TimeZone.getDefault()).apply {
            val createdDate = vault?.createdAt?.toDate()
            if (createdDate != null) {
                time = createdDate
            } else {
                set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
            }
        }

        val diffMillis = (may2028.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt().coerceAtLeast(1)

        val totalPlanMillis = (may2028.timeInMillis - startJourney.timeInMillis).coerceAtLeast(86400000L)
        val totalPlanDays = TimeUnit.MILLISECONDS.toDays(totalPlanMillis).toInt().coerceAtLeast(daysRemaining)
        val elapsedDays = (totalPlanDays - daysRemaining).coerceAtLeast(1)
        val timelineElapsedFraction = (elapsedDays.toFloat() / totalPlanDays.toFloat()).coerceIn(0.03f, 1f)

        val targetAmount = (vault?.targetAmount ?: 20000.0).coerceAtLeast(1.0)
        val totalSaved = vault?.totalSaved ?: 0.0
        val goalProgressFraction = (totalSaved / targetAmount).toFloat().coerceIn(0f, 1f)
        val remainingMoney = (targetAmount - totalSaved).coerceAtLeast(0.0)

        return May2028ProgressSummary(
            daysRemainingUntilMay2028 = daysRemaining,
            totalDaysInPlan = totalPlanDays,
            elapsedDays = elapsedDays,
            timelineElapsedFraction = timelineElapsedFraction,
            goalProgressFraction = goalProgressFraction,
            remainingToGoal = remainingMoney,
            totalDepositsCount = deposits.size
        )
    }

    private data class LocalControls(
        val vaultId: String?,
        val hidden: Boolean,
        val cardIndex: Int,
        val animTrigger: Long,
        val msg: String?,
        val err: String?
    )

    private companion object {
        const val TAG = "VaultViewModel"
    }
}
