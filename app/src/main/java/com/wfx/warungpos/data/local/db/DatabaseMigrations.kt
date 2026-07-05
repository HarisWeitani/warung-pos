package com.wfx.warungpos.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE payment_methods ADD COLUMN isCash INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE payment_methods SET isCash = 1 WHERE id = 'pm_tunai'")
    }
}

// AM-2: tables removed entirely; bills no longer reference a table or carry a type (always upfront).
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `tables`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bills_new` (
                `id` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `sessionLabel` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `paidAt` INTEGER,
                `subtotal` INTEGER NOT NULL,
                `discountTotal` INTEGER NOT NULL,
                `grandTotal` INTEGER NOT NULL,
                `note` TEXT,
                `shiftId` TEXT,
                `voidReason` TEXT,
                `voidedBy` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `syncStatus` TEXT NOT NULL,
                `deviceId` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`shiftId`) REFERENCES `shifts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `bills_new` (id, status, sessionLabel, createdAt, paidAt, subtotal, discountTotal, grandTotal, note, shiftId, voidReason, voidedBy, updatedAt, syncStatus, deviceId)
            SELECT id, status, sessionLabel, createdAt, paidAt, subtotal, discountTotal, grandTotal, note, shiftId, voidReason, voidedBy, updatedAt, syncStatus, deviceId FROM `bills`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `bills`")
        db.execSQL("ALTER TABLE `bills_new` RENAME TO `bills`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_shiftId` ON `bills` (`shiftId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_status` ON `bills` (`status`)")
    }
}
