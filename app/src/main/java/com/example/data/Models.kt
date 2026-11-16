package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Vault(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val inviteCode: String = "",
    val title: String = "May 2028 Hədəfi",
    val currency: String = "₼",
    val leatherTheme: String = "black",
    val targetAmount: Double = 20000.0,
    val dailyTarget: Double = 35.0,
    val totalSaved: Double = 0.0,
    val ownerSaved: Double = 0.0,
    val partnerSaved: Double = 0.0,
    val ownerName: String = "Mən",
    val partnerName: String = "",
    val partnerUid: String = "",
    val targetDateIso: String = "2028-05-31",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Deposit(
    @DocumentId val id: String = "",
    val vaultId: String = "",
    val userId: String = "",
    val userName: String = "",
    val memberUids: List<String> = emptyList(),
    val amount: Double = 0.0,
    val note: String = "",
    val dayKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
