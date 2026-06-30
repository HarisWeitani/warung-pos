package com.wfx.warungpos.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE payment_methods ADD COLUMN isCash INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE payment_methods SET isCash = 1 WHERE id = 'pm_tunai'")
    }
}
