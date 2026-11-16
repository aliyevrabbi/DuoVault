package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VaultRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserVaults(userId: String = requireUserId()): Flow<List<Vault>> = flow {
        val path = "vaults"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Vault::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserDeposits(userId: String = requireUserId()): Flow<List<Deposit>> = flow {
        val path = "deposits"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Deposit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getVaultById(vaultId: String): Result<Vault?> = runCatching {
        val docRef = db.collection("vaults").document(vaultId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(Vault::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createVault(
        vaultId: String,
        title: String = "May 2028 Hədəfi",
        currency: String = "₼",
        leatherTheme: String = "black",
        targetAmount: Double = 20000.0,
        dailyTarget: Double = 35.0,
        ownerName: String = "Mən"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanCode = vaultId.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_-]"), "")
            .ifEmpty { "DUO2028" }
        val docRef = db.collection("vaults").document(cleanCode)
        val payload = mapOf(
            "ownerId" to uid,
            "memberUids" to listOf(uid),
            "inviteCode" to cleanCode,
            "title" to title.trim().ifEmpty { "May 2028 Hədəfi" }.take(60),
            "currency" to currency.trim().ifEmpty { "₼" }.take(5),
            "leatherTheme" to if (leatherTheme == "brown") "brown" else "black",
            "targetAmount" to targetAmount.coerceIn(1.0, 100000000.0),
            "dailyTarget" to dailyTarget.coerceIn(1.0, 10000000.0),
            "totalSaved" to 0.0,
            "ownerSaved" to 0.0,
            "partnerSaved" to 0.0,
            "ownerName" to ownerName.trim().ifEmpty { "Dost 1" }.take(60),
            "partnerName" to "",
            "partnerUid" to "",
            "targetDateIso" to "2028-05-31",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            cleanCode
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun joinVaultByCode(
        inviteCode: String,
        partnerName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanCode = inviteCode.trim().uppercase(Locale.US)
        require(cleanCode.length in 4..32) { "Dəvət kodu 4-32 simvol olmalıdır" }
        val docRef = db.collection("vaults").document(cleanCode)
        val updates = mapOf(
            "memberUids" to FieldValue.arrayUnion(uid),
            "partnerUid" to uid,
            "partnerName" to partnerName.trim().ifEmpty { "Dost 2" }.take(60),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
            cleanCode
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addDeposit(
        vault: Vault,
        amount: Double,
        note: String,
        userName: String,
        forPartnerOnBehalf: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(vault.id.isNotBlank()) { "Pulqabı ID boş ola bilməz" }
        val validAmount = amount.coerceIn(0.01, 10000000.0)
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val depositRef = db.collection("deposits").document()
        val depositPayload = mapOf(
            "vaultId" to vault.id,
            "userId" to uid,
            "userName" to userName.trim().ifEmpty { "Dost" }.take(60),
            "memberUids" to vault.memberUids,
            "amount" to validAmount,
            "note" to note.trim().take(120),
            "dayKey" to dayKey,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val vaultRef = db.collection("vaults").document(vault.id)
        val isOwner = (uid == vault.ownerId) && !forPartnerOnBehalf
        val newOwnerSaved = if (isOwner) vault.ownerSaved + validAmount else vault.ownerSaved
        val newPartnerSaved = if (!isOwner) vault.partnerSaved + validAmount else vault.partnerSaved
        val newTotalSaved = newOwnerSaved + newPartnerSaved

        val vaultUpdates = mapOf(
            "totalSaved" to newTotalSaved,
            "ownerSaved" to newOwnerSaved,
            "partnerSaved" to newPartnerSaved,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            depositRef.set(depositPayload).await()
            vaultRef.update(vaultUpdates).await()
            depositRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, depositRef.path)
            throw e
        }
    }

    suspend fun updateVaultSettings(
        vaultId: String,
        title: String,
        currency: String,
        leatherTheme: String,
        targetAmount: Double,
        dailyTarget: Double,
        ownerName: String,
        partnerName: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("vaults").document(vaultId)
        val updates = mapOf(
            "title" to title.trim().ifEmpty { "May 2028 Hədəfi" }.take(60),
            "currency" to currency.trim().ifEmpty { "₼" }.take(5),
            "leatherTheme" to if (leatherTheme == "brown") "brown" else "black",
            "targetAmount" to targetAmount.coerceIn(1.0, 100000000.0),
            "dailyTarget" to dailyTarget.coerceIn(1.0, 10000000.0),
            "ownerName" to ownerName.trim().ifEmpty { "Mən" }.take(60),
            "partnerName" to partnerName.trim().take(60),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
