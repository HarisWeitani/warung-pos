package com.wfx.warungpos.data.local.db

import androidx.room.TypeConverter
import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.core.common.VoidReason

class Converters {
    @TypeConverter fun fromSyncStatus(v: SyncStatus): String = v.name
    @TypeConverter fun toSyncStatus(v: String): SyncStatus = SyncStatus.valueOf(v)

    @TypeConverter fun fromBillType(v: BillType): String = v.name
    @TypeConverter fun toBillType(v: String): BillType = BillType.valueOf(v)

    @TypeConverter fun fromBillStatus(v: BillStatus): String = v.name
    @TypeConverter fun toBillStatus(v: String): BillStatus = BillStatus.valueOf(v)

    @TypeConverter fun fromOrderItemStatus(v: OrderItemStatus): String = v.name
    @TypeConverter fun toOrderItemStatus(v: String): OrderItemStatus = OrderItemStatus.valueOf(v)

    @TypeConverter fun fromShiftStatus(v: ShiftStatus): String = v.name
    @TypeConverter fun toShiftStatus(v: String): ShiftStatus = ShiftStatus.valueOf(v)

    @TypeConverter fun fromOpnameStatus(v: OpnameStatus): String = v.name
    @TypeConverter fun toOpnameStatus(v: String): OpnameStatus = OpnameStatus.valueOf(v)

    @TypeConverter fun fromVariantSelectionType(v: VariantSelectionType): String = v.name
    @TypeConverter fun toVariantSelectionType(v: String): VariantSelectionType = VariantSelectionType.valueOf(v)

    @TypeConverter fun fromVoidReason(v: VoidReason): String = v.name
    @TypeConverter fun toVoidReason(v: String): VoidReason = VoidReason.valueOf(v)

    @TypeConverter fun fromExpenseCategory(v: ExpenseCategory): String = v.name
    @TypeConverter fun toExpenseCategory(v: String): ExpenseCategory = ExpenseCategory.valueOf(v)

    @TypeConverter fun fromVarianceReason(v: VarianceReason): String = v.name
    @TypeConverter fun toVarianceReason(v: String): VarianceReason = VarianceReason.valueOf(v)
}
