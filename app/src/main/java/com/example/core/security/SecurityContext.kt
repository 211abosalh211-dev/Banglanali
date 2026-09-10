package com.example.core.security

import java.security.MessageDigest

/**
 * Enterprise permission codes for granular Role-Based Access Control (RBAC).
 * Supports View, Create, Update, Delete, Approve, Cancel, Print, Export.
 */
object Permissions {
    // Modules
    const val DASHBOARD_VIEW = "DASHBOARD_VIEW"

    const val USERS_VIEW = "USERS_VIEW"
    const val USERS_CREATE = "USERS_CREATE"
    const val USERS_UPDATE = "USERS_UPDATE"
    const val USERS_DELETE = "USERS_DELETE"

    const val UNITS_VIEW = "UNITS_VIEW"
    const val UNITS_CREATE = "UNITS_CREATE"
    const val UNITS_UPDATE = "UNITS_UPDATE"
    const val UNITS_DELETE = "UNITS_DELETE"

    const val CUSTOMERS_VIEW = "CUSTOMERS_VIEW"
    const val CUSTOMERS_CREATE = "CUSTOMERS_CREATE"
    const val CUSTOMERS_UPDATE = "CUSTOMERS_UPDATE"
    const val CUSTOMERS_DELETE = "CUSTOMERS_DELETE"

    const val RESERVATIONS_VIEW = "RESERVATIONS_VIEW"
    const val RESERVATIONS_CREATE = "RESERVATIONS_CREATE"
    const val RESERVATIONS_UPDATE = "RESERVATIONS_UPDATE"
    const val RESERVATIONS_CANCEL = "RESERVATIONS_CANCEL"

    const val STAYS_VIEW = "STAYS_VIEW"
    const val STAYS_CHECKIN = "STAYS_CHECKIN"
    const val STAYS_CHECKOUT = "STAYS_CHECKOUT"
    const val STAYS_TRANSFER = "STAYS_TRANSFER"

    const val ACCOUNTING_VIEW = "ACCOUNTING_VIEW"
    const val ACCOUNTING_CREATE = "ACCOUNTING_CREATE"
    const val ACCOUNTING_POST = "ACCOUNTING_POST"

    const val CASHBOXES_VIEW = "CASHBOXES_VIEW"
    const val CASHBOXES_TRANSFER = "CASHBOXES_TRANSFER"
    const val SHIFTS_MANAGE = "SHIFTS_MANAGE"

    const val RECEIPTS_CREATE = "RECEIPTS_CREATE"
    const val PAYMENTS_CREATE = "PAYMENTS_CREATE"

    const val INVENTORY_VIEW = "INVENTORY_VIEW"
    const val INVENTORY_MANAGE = "INVENTORY_MANAGE"

    const val PURCHASING_VIEW = "PURCHASING_VIEW"
    const val PURCHASING_CREATE = "PURCHASING_CREATE"

    const val POS_VIEW = "POS_VIEW"
    const val POS_SELL = "POS_SELL"
    const val POS_RETURN = "POS_RETURN"

    const val REPORTS_VIEW = "REPORTS_VIEW"
    const val REPORTS_EXPORT = "REPORTS_EXPORT"
    const val REPORTS_PRINT = "REPORTS_PRINT"

    const val SETTINGS_VIEW = "SETTINGS_VIEW"
    const val SETTINGS_UPDATE = "SETTINGS_UPDATE"
}

/**
 * Standard SHA-256 password hasher with salt support.
 */
object PasswordHasher {
    fun hash(password: String, salt: String = "HotelErpPro2026"): String {
        val input = "$salt:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String, salt: String = "HotelErpPro2026"): Boolean {
        return hash(password, salt) == hash
    }
}
