package com.qvendor.momo.data

object TransactionRecorder {

    @Volatile
    var pendingId: Long = -1L
        private set

    suspend fun record(
        dao: TransactionDao,
        profile: VendorProfile,
        action: ProfileAction,
        phone: String,
        amount: Double
    ): Long {
        val t = Transaction(
            profileId = profile.id,
            actionName = action.name,
            ispName = profile.ispName,
            phone = phone,
            amount = amount,
            status = TransactionStatus.PENDING
        )
        val id = dao.insert(t)
        pendingId = id
        return id
    }

    suspend fun resolve(dao: TransactionDao, id: Long, success: Boolean) {
        if (id <= 0) return
        val existing = dao.getById(id) ?: return
        val updated = existing.copy(
            status = if (success) TransactionStatus.SUCCESSFUL else TransactionStatus.FAILED
        )
        dao.update(updated)
        if (pendingId == id) pendingId = -1L
    }
}
