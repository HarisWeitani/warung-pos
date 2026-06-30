package com.wfx.warungpos.core.navigation

import kotlinx.serialization.Serializable

// ── Top-level destinations (bottom nav) ──────────────────────────────────────
@Serializable data object OrderRoute
@Serializable data object TablesRoute
@Serializable data object ReportsRoute   // owner-only
@Serializable data object MoreRoute
@Serializable data object DashboardRoute // owner-only

// ── Auth ─────────────────────────────────────────────────────────────────────
@Serializable data object LoginRoute
@Serializable data object UpdateRequiredRoute

// ── Shift ────────────────────────────────────────────────────────────────────
@Serializable data object ShiftOpenRoute
@Serializable data object ShiftCloseRoute
@Serializable data class ZReportRoute(val shiftId: String)

// ── Bills & Order ─────────────────────────────────────────────────────────────
@Serializable data class BillDetailRoute(val billId: String)
@Serializable data class PaymentRoute(val billId: String)

// ── Expense ──────────────────────────────────────────────────────────────────
@Serializable data object ExpenseLogRoute

// ── Menu management ──────────────────────────────────────────────────────────
@Serializable data object MenuManagementRoute
@Serializable data class MenuItemEditRoute(val itemId: String? = null)

// ── Settings ─────────────────────────────────────────────────────────────────
@Serializable data object SettingsRoute
@Serializable data object TableSettingsRoute
@Serializable data object PaymentMethodSettingsRoute
@Serializable data object ExpenseCategorySettingsRoute
@Serializable data object LanguageSettingsRoute
@Serializable data object AboutRoute
