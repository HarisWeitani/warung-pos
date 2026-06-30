package com.wfx.warungpos.fake

import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.UserRole

class FakeSessionProvider(
    override var currentUserId: String? = "user-1",
    override var currentUserRole: UserRole = UserRole.OWNER,
    override var deviceId: String = "device-1",
) : SessionProvider
