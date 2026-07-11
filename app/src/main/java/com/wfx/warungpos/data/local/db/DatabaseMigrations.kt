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

// FR-OPNAME-7: sales during an active opname are queued here instead of touching StockItem
// directly, then applied on top of the counted baseline when the opname commits.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_stock_deductions` (
                `id` TEXT NOT NULL,
                `opnameId` TEXT NOT NULL,
                `stockItemId` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`opnameId`) REFERENCES `stock_opnames`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`stockItemId`) REFERENCES `stock_items`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_stock_deductions_opnameId` ON `pending_stock_deductions` (`opnameId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_stock_deductions_stockItemId` ON `pending_stock_deductions` (`stockItemId`)")
    }
}

// DEFECT-003/008: a client-side check-then-act race let multiple `shifts` rows end up
// simultaneously OPEN (fixed going forward by ShiftDao.openIfNoneOpen's transactional guard).
// This is a pure data repair for installs that already have that corruption: keep only the
// most-recently-opened OPEN shift, force-close the rest. No schema/column change, so no Room
// schema-validation risk — just a version bump to run the repair exactly once on upgrade.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE `shifts`
            SET status = 'CLOSED',
                closedAt = COALESCE(closedAt, (strftime('%s','now') * 1000)),
                syncStatus = 'PENDING'
            WHERE status = 'OPEN'
            AND id NOT IN (
                SELECT id FROM `shifts` WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1
            )
            """.trimIndent()
        )
    }
}

// DEFECT-006: the "Other" void reason's required note was validated at submit time but had no
// column to be written to — silently discarded. Adds the missing column.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `order_items` ADD COLUMN `voidNote` TEXT")
    }
}
