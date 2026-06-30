package com.wfx.warungpos.domain.exception

import com.wfx.warungpos.domain.model.Bill

class EmptyCartException : Exception("Cart is empty")
class ShiftNotOpenException : Exception("No open shift")
class MissingRequiredVariantException(val itemName: String, val groupName: String) :
    Exception("Required variant group '$groupName' not satisfied for '$itemName'")
class BillAlreadyPaidException : Exception("Bill is already paid")
class InsufficientTenderedAmountException : Exception("Tendered amount is less than row amount")
class InsufficientPaymentException : Exception("Total payment rows do not cover the grand total")
class InsufficientPermissionsException : Exception("Insufficient permissions for this action")
class BillNotVoidableException : Exception("Bill cannot be voided in its current state")
class OpenBillsBlockShiftCloseException(val openBills: List<Bill>) :
    Exception("Cannot close shift: ${openBills.size} open bill(s) remain")
