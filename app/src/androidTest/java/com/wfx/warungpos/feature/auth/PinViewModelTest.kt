package com.wfx.warungpos.feature.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wfx.warungpos.core.common.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** DEFECT-001: PinViewModel is Activity-scoped and survives across Lock App cycles within a
 * process, so its mode must not stay pinned to whatever was true at first construction. */
@RunWith(AndroidJUnit4::class)
class PinViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clearSessionState() {
        // EncryptedSharedPreferences persists on-device across test runs (and across this
        // session's manual UI testing) — each test needs a clean pre-registration slate.
        context.deleteSharedPreferences("session_prefs")
    }

    @Test
    fun refreshMode_afterRegistration_switchesFromRegisterToUnlock() {
        val sessionManager = SessionManager(context)
        val viewModel = PinViewModel(sessionManager)

        // Constructed before registration exists, exactly like the Activity-scoped instance
        // that's first built on a fresh install, before the user has set up a PIN.
        assertEquals(PinViewModel.Mode.REGISTER, viewModel.uiState.value.mode)

        sessionManager.register("Budi", "1234")
        sessionManager.lock()

        // Simulates re-entering the PIN gate after Lock App: the cached ViewModel instance must
        // re-derive its mode from the now-registered state, not reuse REGISTER from construction.
        viewModel.refreshMode()

        assertEquals(PinViewModel.Mode.UNLOCK, viewModel.uiState.value.mode)
    }

    @Test
    fun refreshMode_clearsStaleFormFields() {
        val sessionManager = SessionManager(context)
        val viewModel = PinViewModel(sessionManager)

        viewModel.onUsernameChange("Hacker")
        viewModel.onPinChange("0000")
        viewModel.onConfirmPinChange("0000")

        sessionManager.register("Budi", "1234")
        sessionManager.lock()
        viewModel.refreshMode()

        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.pin)
        assertEquals("", state.confirmPin)
        assertEquals("Budi", state.existingUsername)
    }
}
