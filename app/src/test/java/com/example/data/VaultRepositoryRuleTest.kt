package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VaultRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createVaultAndDeposit_authenticatedMembers_succeedsAndEmitsUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = VaultRepository(firestore)
        val uniqueCode = "DUO_${UUID.randomUUID().toString().take(8).uppercase()}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createVault(
                vaultId = uniqueCode,
                title = "May 2028 Səyahət",
                currency = "₼",
                leatherTheme = "black",
                targetAmount = 15000.0,
                dailyTarget = 30.0,
                ownerName = "Əli"
            )
        }
        assertTrue("Vault creation should succeed", createResult.isSuccess)

        val emittedVaults = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserVaults(aliceUid).first { list -> list.any { it.id == uniqueCode } }
        }
        val vault = emittedVaults.first { it.id == uniqueCode }
        assertEquals("May 2028 Səyahət", vault.title)

        // Add deposit
        val depositResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addDeposit(
                vault = vault,
                amount = 45.0,
                note = "Günlük yığım",
                userName = "Əli"
            )
        }
        assertTrue("Deposit should succeed", depositResult.isSuccess)

        // Friend Bob joins by invite code
        val bobUid = signInTestUser(BOB_EMAIL)
        val joinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinVaultByCode(uniqueCode, "Rəşad")
        }
        assertTrue("Partner join should succeed", joinResult.isSuccess)

        val bobVaults = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserVaults(bobUid).first { list -> list.any { it.id == uniqueCode } }
        }
        assertTrue(bobVaults.any { it.id == uniqueCode && it.partnerUid == bobUid })
    }

    @Test
    fun getVaultById_crossUserNonMember_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = VaultRepository(firestore)
        val uniqueCode = "PRIV_${UUID.randomUUID().toString().take(8).uppercase()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createVault(vaultId = uniqueCode).getOrThrow()
        }

        // Mallory is not in memberUids
        signInTestUser(MALLORY_EMAIL)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getVaultById(uniqueCode)
        }
        assertTrue("Non-member getVaultById must fail", result.isFailure)
        val rootEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(rootEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, rootEx?.code)
    }

    @Test
    fun observeUserVaults_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = VaultRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserVaults("unauth_uid").first()
            }
            fail("Expected exception for unauthenticated query")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@duovault.test"
        const val BOB_EMAIL = "bob@duovault.test"
        const val MALLORY_EMAIL = "mallory@duovault.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
