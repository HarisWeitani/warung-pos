package com.wfx.warungpos.core.common

interface SessionProvider {
    val currentUserId: String?
    val currentUserRole: UserRole
    val deviceId: String
}
