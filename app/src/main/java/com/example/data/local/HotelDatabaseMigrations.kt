package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // System & Auth
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `users` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `username` TEXT NOT NULL,
                `passwordHash` TEXT NOT NULL,
                `fullName` TEXT NOT NULL,
                `email` TEXT,
                `phone` TEXT,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `roles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `roleCode` TEXT NOT NULL,
                `roleNameAr` TEXT NOT NULL,
                `description` TEXT,
                `isSystemRole` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_roles_roleCode` ON `roles` (`roleCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `permissions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `permissionCode` TEXT NOT NULL,
                `moduleName` TEXT NOT NULL,
                `descriptionAr` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_permissions_permissionCode` ON `permissions` (`permissionCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `role_permissions` (
                `roleId` INTEGER NOT NULL,
                `permissionId` INTEGER NOT NULL,
                PRIMARY KEY(`roleId`, `permissionId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_role_permissions_roleId` ON `role_permissions` (`roleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_role_permissions_permissionId` ON `role_permissions` (`permissionId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `user_roles` (
                `userId` INTEGER NOT NULL,
                `roleId` INTEGER NOT NULL,
                PRIMARY KEY(`userId`, `roleId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_roles_userId` ON `user_roles` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_roles_roleId` ON `user_roles` (`roleId`)")

        // Customers
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `customers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `customerCode` TEXT NOT NULL,
                `fullName` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `email` TEXT,
                `nationalIdType` TEXT NOT NULL,
                `nationalIdNumber` TEXT,
                `nationality` TEXT NOT NULL,
                `address` TEXT,
                `companyName` TEXT,
                `taxNumber` TEXT,
                `currentBalanceMinor` INTEGER NOT NULL,
                `creditLimitMinor` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_customerCode` ON `customers` (`customerCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_nationalIdNumber` ON `customers` (`nationalIdNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_isDeleted` ON `customers` (`isDeleted`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `customer_notes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `noteText` TEXT NOT NULL,
                `noteType` TEXT NOT NULL,
                `createdBy` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_notes_customerId` ON `customer_notes` (`customerId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `customer_documents` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `docType` TEXT NOT NULL,
                `docNumber` TEXT NOT NULL,
                `fileUri` TEXT,
                `issueDate` TEXT,
                `expiryDate` TEXT,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_documents_customerId` ON `customer_documents` (`customerId`)")

        // Hotel Units
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `unit_types` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `typeCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `nameEn` TEXT NOT NULL,
                `basePriceMinor` INTEGER NOT NULL,
                `defaultMaxGuests` INTEGER NOT NULL,
                `description` TEXT,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_unit_types_typeCode` ON `unit_types` (`typeCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `units` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `unitNumber` TEXT NOT NULL,
                `unitTypeId` INTEGER NOT NULL,
                `floorNumber` INTEGER NOT NULL,
                `buildingWing` TEXT,
                `status` TEXT NOT NULL,
                `customNightlyPriceMinor` INTEGER,
                `capacityAdults` INTEGER NOT NULL,
                `capacityChildren` INTEGER NOT NULL,
                `notes` TEXT,
                `isSmoking` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`unitTypeId`) REFERENCES `unit_types`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_units_unitNumber` ON `units` (`unitNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_units_unitTypeId` ON `units` (`unitTypeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_units_status` ON `units` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_units_floorNumber` ON `units` (`floorNumber`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `unit_features` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `featureCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `iconName` TEXT,
                `additionalChargeMinor` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_unit_features_featureCode` ON `unit_features` (`featureCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `unit_type_features` (
                `unitTypeId` INTEGER NOT NULL,
                `featureId` INTEGER NOT NULL,
                PRIMARY KEY(`unitTypeId`, `featureId`),
                FOREIGN KEY(`unitTypeId`) REFERENCES `unit_types`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`featureId`) REFERENCES `unit_features`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_type_features_unitTypeId` ON `unit_type_features` (`unitTypeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_type_features_featureId` ON `unit_type_features` (`featureId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `unit_status_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `unitId` INTEGER NOT NULL,
                `previousStatus` TEXT NOT NULL,
                `newStatus` TEXT NOT NULL,
                `reasonOrNotes` TEXT,
                `changedBy` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`unitId`) REFERENCES `units`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_status_history_unitId` ON `unit_status_history` (`unitId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_status_history_timestamp` ON `unit_status_history` (`timestamp`)")

        // Reservations & Stays
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reservations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `reservationNumber` TEXT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `expectedCheckIn` INTEGER NOT NULL,
                `expectedCheckOut` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `totalAmountMinor` INTEGER NOT NULL,
                `depositRequiredMinor` INTEGER NOT NULL,
                `depositPaidMinor` INTEGER NOT NULL,
                `adultsCount` INTEGER NOT NULL,
                `childrenCount` INTEGER NOT NULL,
                `sourceChannel` TEXT NOT NULL,
                `specialRequests` TEXT,
                `cancellationReason` TEXT,
                `createdBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reservations_reservationNumber` ON `reservations` (`reservationNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservations_customerId` ON `reservations` (`customerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservations_status` ON `reservations` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservations_expectedCheckIn` ON `reservations` (`expectedCheckIn`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservations_expectedCheckOut` ON `reservations` (`expectedCheckOut`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reservation_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `reservationId` INTEGER NOT NULL,
                `unitTypeId` INTEGER NOT NULL,
                `assignedUnitId` INTEGER,
                `checkInDate` INTEGER NOT NULL,
                `checkOutDate` INTEGER NOT NULL,
                `ratePerNightMinor` INTEGER NOT NULL,
                `nightsCount` INTEGER NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalMinor` INTEGER NOT NULL,
                FOREIGN KEY(`reservationId`) REFERENCES `reservations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`unitTypeId`) REFERENCES `unit_types`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`assignedUnitId`) REFERENCES `units`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservation_items_reservationId` ON `reservation_items` (`reservationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservation_items_unitTypeId` ON `reservation_items` (`unitTypeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservation_items_assignedUnitId` ON `reservation_items` (`assignedUnitId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `guests` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `fullName` TEXT NOT NULL,
                `nationalIdType` TEXT NOT NULL,
                `nationalIdNumber` TEXT,
                `nationality` TEXT NOT NULL,
                `phone` TEXT,
                `email` TEXT,
                `gender` TEXT NOT NULL,
                `isBlacklisted` INTEGER NOT NULL,
                `blacklistReason` TEXT,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_guests_nationalIdNumber` ON `guests` (`nationalIdNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_guests_phone` ON `guests` (`phone`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stays` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stayNumber` TEXT NOT NULL,
                `reservationId` INTEGER,
                `customerId` INTEGER NOT NULL,
                `actualCheckIn` INTEGER NOT NULL,
                `actualCheckOut` INTEGER,
                `status` TEXT NOT NULL,
                `totalFolioChargesMinor` INTEGER NOT NULL,
                `totalPaidMinor` INTEGER NOT NULL,
                `balanceDueMinor` INTEGER NOT NULL,
                `notes` TEXT,
                `createdBy` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`reservationId`) REFERENCES `reservations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stays_stayNumber` ON `stays` (`stayNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stays_reservationId` ON `stays` (`reservationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stays_customerId` ON `stays` (`customerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stays_status` ON `stays` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stays_actualCheckIn` ON `stays` (`actualCheckIn`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stays_actualCheckOut` ON `stays` (`actualCheckOut`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stay_guests` (
                `stayId` INTEGER NOT NULL,
                `guestId` INTEGER NOT NULL,
                `isPrimaryGuest` INTEGER NOT NULL,
                PRIMARY KEY(`stayId`, `guestId`),
                FOREIGN KEY(`stayId`) REFERENCES `stays`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`guestId`) REFERENCES `guests`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stay_guests_stayId` ON `stay_guests` (`stayId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stay_guests_guestId` ON `stay_guests` (`guestId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `unit_assignments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stayId` INTEGER NOT NULL,
                `unitId` INTEGER NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER NOT NULL,
                `ratePerNightMinor` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                FOREIGN KEY(`stayId`) REFERENCES `stays`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`unitId`) REFERENCES `units`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_assignments_stayId` ON `unit_assignments` (`stayId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_assignments_unitId` ON `unit_assignments` (`unitId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unit_assignments_unitId_startDate_endDate` ON `unit_assignments` (`unitId`, `startDate`, `endDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `check_in_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stayId` INTEGER NOT NULL,
                `checkInTime` INTEGER NOT NULL,
                `operatorName` TEXT NOT NULL,
                `depositCollectedMinor` INTEGER NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`stayId`) REFERENCES `stays`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_in_records_stayId` ON `check_in_records` (`stayId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `check_out_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stayId` INTEGER NOT NULL,
                `checkOutTime` INTEGER NOT NULL,
                `operatorName` TEXT NOT NULL,
                `settlementTotalMinor` INTEGER NOT NULL,
                `refundAmountMinor` INTEGER NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`stayId`) REFERENCES `stays`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_out_records_stayId` ON `check_out_records` (`stayId`)")

        // Services
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `service_categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `nameEn` TEXT,
                `isActive` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_service_categories_categoryCode` ON `service_categories` (`categoryCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `services` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serviceCode` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `nameAr` TEXT NOT NULL,
                `nameEn` TEXT,
                `defaultPriceMinor` INTEGER NOT NULL,
                `taxPercentage` REAL NOT NULL,
                `isAvailable` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                FOREIGN KEY(`categoryId`) REFERENCES `service_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_services_serviceCode` ON `services` (`serviceCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_services_categoryId` ON `services` (`categoryId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `service_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `stayId` INTEGER NOT NULL,
                `serviceId` INTEGER NOT NULL,
                `quantity` INTEGER NOT NULL,
                `unitPriceMinor` INTEGER NOT NULL,
                `totalAmountMinor` INTEGER NOT NULL,
                `notes` TEXT,
                `servedBy` TEXT,
                `transactionTimestamp` INTEGER NOT NULL,
                FOREIGN KEY(`stayId`) REFERENCES `stays`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`serviceId`) REFERENCES `services`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_transactions_stayId` ON `service_transactions` (`stayId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_transactions_serviceId` ON `service_transactions` (`serviceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_transactions_transactionTimestamp` ON `service_transactions` (`transactionTimestamp`)")

        // Accounting
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `account_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `groupCode` TEXT NOT NULL,
                `groupNameAr` TEXT NOT NULL,
                `groupNameEn` TEXT NOT NULL,
                `normalBalance` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_groups_groupCode` ON `account_groups` (`groupCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountCode` TEXT NOT NULL,
                `accountNameAr` TEXT NOT NULL,
                `accountNameEn` TEXT,
                `accountGroupId` INTEGER NOT NULL,
                `parentAccountId` INTEGER,
                `accountType` TEXT NOT NULL,
                `isHeader` INTEGER NOT NULL,
                `currentBalanceMinor` INTEGER NOT NULL,
                `currencyCode` TEXT NOT NULL,
                `isSystemAccount` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`accountGroupId`) REFERENCES `account_groups`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`parentAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_accountCode` ON `accounts` (`accountCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_accountGroupId` ON `accounts` (`accountGroupId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_parentAccountId` ON `accounts` (`parentAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isDeleted` ON `accounts` (`isDeleted`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `fiscal_periods` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `periodCode` TEXT NOT NULL,
                `periodNameAr` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER NOT NULL,
                `isClosed` INTEGER NOT NULL,
                `closedAt` INTEGER,
                `closedBy` TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fiscal_periods_periodCode` ON `fiscal_periods` (`periodCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fiscal_periods_startDate_endDate` ON `fiscal_periods` (`startDate`, `endDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `journal_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entryNumber` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `referenceType` TEXT,
                `referenceId` TEXT,
                `status` TEXT NOT NULL,
                `totalDebitMinor` INTEGER NOT NULL,
                `totalCreditMinor` INTEGER NOT NULL,
                `createdBy` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_entries_entryNumber` ON `journal_entries` (`entryNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_date` ON `journal_entries` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_status` ON `journal_entries` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_referenceType_referenceId` ON `journal_entries` (`referenceType`, `referenceId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `journal_entry_lines` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entryId` INTEGER NOT NULL,
                `accountId` INTEGER NOT NULL,
                `debit` INTEGER NOT NULL,
                `credit` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                FOREIGN KEY(`entryId`) REFERENCES `journal_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_lines_entryId` ON `journal_entry_lines` (`entryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_lines_accountId` ON `journal_entry_lines` (`accountId`)")

        // Cashboxes & Shifts
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cashboxes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `cashboxCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `glAccountId` INTEGER NOT NULL,
                `currencyCode` TEXT NOT NULL,
                `currentBalanceMinor` INTEGER NOT NULL,
                `isPrimary` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`glAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cashboxes_cashboxCode` ON `cashboxes` (`cashboxCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cashboxes_glAccountId` ON `cashboxes` (`glAccountId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cashbox_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transactionNumber` TEXT NOT NULL,
                `cashboxId` INTEGER NOT NULL,
                `transactionType` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `direction` TEXT NOT NULL,
                `balanceAfterMinor` INTEGER NOT NULL,
                `referenceType` TEXT,
                `referenceId` TEXT,
                `description` TEXT NOT NULL,
                `operatorName` TEXT NOT NULL,
                `transactionDate` INTEGER NOT NULL,
                FOREIGN KEY(`cashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cashbox_transactions_transactionNumber` ON `cashbox_transactions` (`transactionNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cashbox_transactions_cashboxId` ON `cashbox_transactions` (`cashboxId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cashbox_transactions_transactionDate` ON `cashbox_transactions` (`transactionDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cashbox_transactions_referenceType_referenceId` ON `cashbox_transactions` (`referenceType`, `referenceId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cashbox_transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transferNumber` TEXT NOT NULL,
                `fromCashboxId` INTEGER NOT NULL,
                `toCashboxId` INTEGER NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `transferredBy` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`fromCashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`toCashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cashbox_transfers_transferNumber` ON `cashbox_transfers` (`transferNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cashbox_transfers_fromCashboxId` ON `cashbox_transfers` (`fromCashboxId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cashbox_transfers_toCashboxId` ON `cashbox_transfers` (`toCashboxId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `shifts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shiftNumber` TEXT NOT NULL,
                `cashboxId` INTEGER NOT NULL,
                `operatorName` TEXT NOT NULL,
                `startTime` INTEGER NOT NULL,
                `endTime` INTEGER,
                `openingBalanceMinor` INTEGER NOT NULL,
                `closingBalanceMinor` INTEGER,
                `systemExpectedBalanceMinor` INTEGER,
                `differenceMinor` INTEGER,
                `status` TEXT NOT NULL,
                FOREIGN KEY(`cashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shifts_shiftNumber` ON `shifts` (`shiftNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shifts_cashboxId` ON `shifts` (`cashboxId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shifts_status` ON `shifts` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `shift_transactions` (
                `shiftId` INTEGER NOT NULL,
                `transactionId` INTEGER NOT NULL,
                PRIMARY KEY(`shiftId`, `transactionId`),
                FOREIGN KEY(`shiftId`) REFERENCES `shifts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`transactionId`) REFERENCES `cashbox_transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_transactions_shiftId` ON `shift_transactions` (`shiftId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_transactions_transactionId` ON `shift_transactions` (`transactionId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `shift_closures` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shiftId` INTEGER NOT NULL,
                `actualCashCountMinor` INTEGER NOT NULL,
                `systemCalculatedMinor` INTEGER NOT NULL,
                `varianceMinor` INTEGER NOT NULL,
                `closureNotes` TEXT,
                `closedBy` TEXT NOT NULL,
                `closureTimestamp` INTEGER NOT NULL,
                FOREIGN KEY(`shiftId`) REFERENCES `shifts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_closures_shiftId` ON `shift_closures` (`shiftId`)")

        // Invoices
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoices` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `invoiceNumber` TEXT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `stayId` INTEGER,
                `issueDate` INTEGER NOT NULL,
                `dueDate` INTEGER NOT NULL,
                `currencyCode` TEXT NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalAmountMinor` INTEGER NOT NULL,
                `paidAmountMinor` INTEGER NOT NULL,
                `balanceDueMinor` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `createdBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`stayId`) REFERENCES `stays`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_invoiceNumber` ON `invoices` (`invoiceNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_customerId` ON `invoices` (`customerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_stayId` ON `invoices` (`stayId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_status` ON `invoices` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_issueDate` ON `invoices` (`issueDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoice_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `invoiceId` INTEGER NOT NULL,
                `itemType` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `unitPriceMinor` INTEGER NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalMinor` INTEGER NOT NULL,
                FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoiceId` ON `invoice_items` (`invoiceId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoice_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `invoiceId` INTEGER NOT NULL,
                `paymentReferenceNumber` TEXT NOT NULL,
                `amountPaidMinor` INTEGER NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `cashboxId` INTEGER,
                `paymentDate` INTEGER NOT NULL,
                `receivedBy` TEXT NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_payments_invoiceId` ON `invoice_payments` (`invoiceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_payments_paymentDate` ON `invoice_payments` (`paymentDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoice_adjustments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `adjustmentNumber` TEXT NOT NULL,
                `invoiceId` INTEGER NOT NULL,
                `adjustmentType` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `approvedBy` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoice_adjustments_adjustmentNumber` ON `invoice_adjustments` (`adjustmentNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_adjustments_invoiceId` ON `invoice_adjustments` (`invoiceId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoice_returns` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `returnNumber` TEXT NOT NULL,
                `invoiceId` INTEGER NOT NULL,
                `returnAmountMinor` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `processedBy` TEXT NOT NULL,
                `returnDate` INTEGER NOT NULL,
                FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoice_returns_returnNumber` ON `invoice_returns` (`returnNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_returns_invoiceId` ON `invoice_returns` (`invoiceId`)")

        // Suppliers
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `suppliers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `supplierCode` TEXT NOT NULL,
                `companyName` TEXT NOT NULL,
                `contactPerson` TEXT,
                `phone` TEXT NOT NULL,
                `email` TEXT,
                `address` TEXT,
                `taxNumber` TEXT,
                `currentBalanceMinor` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_suppliers_supplierCode` ON `suppliers` (`supplierCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_phone` ON `suppliers` (`phone`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_isDeleted` ON `suppliers` (`isDeleted`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `supplier_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transactionNumber` TEXT NOT NULL,
                `supplierId` INTEGER NOT NULL,
                `transactionType` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `balanceAfterMinor` INTEGER NOT NULL,
                `referenceType` TEXT,
                `referenceId` TEXT,
                `notes` TEXT,
                `createdBy` TEXT NOT NULL,
                `transactionDate` INTEGER NOT NULL,
                FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_supplier_transactions_transactionNumber` ON `supplier_transactions` (`transactionNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_transactions_supplierId` ON `supplier_transactions` (`supplierId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_transactions_transactionDate` ON `supplier_transactions` (`transactionDate`)")

        // Inventory
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `warehouses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `warehouseCode` TEXT NOT NULL,
                `warehouseNameAr` TEXT NOT NULL,
                `locationDescription` TEXT,
                `isPrimary` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_warehouses_warehouseCode` ON `warehouses` (`warehouseCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `parentCategoryId` INTEGER,
                `isActive` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_categoryCode` ON `categories` (`categoryCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `products` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `productCode` TEXT NOT NULL,
                `barcode` TEXT,
                `nameAr` TEXT NOT NULL,
                `nameEn` TEXT,
                `categoryId` INTEGER NOT NULL,
                `defaultCostPriceMinor` INTEGER NOT NULL,
                `defaultSalePriceMinor` INTEGER NOT NULL,
                `minReorderLevel` REAL NOT NULL,
                `trackInventory` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_productCode` ON `products` (`productCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_categoryId` ON `products` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_isDeleted` ON `products` (`isDeleted`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `units_of_measure` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uomCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `symbolAr` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_units_of_measure_uomCode` ON `units_of_measure` (`uomCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `product_units` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `productId` INTEGER NOT NULL,
                `uomId` INTEGER NOT NULL,
                `conversionFactor` REAL NOT NULL,
                `isBaseUnit` INTEGER NOT NULL,
                `barcode` TEXT,
                FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`uomId`) REFERENCES `units_of_measure`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_units_productId` ON `product_units` (`productId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_units_uomId` ON `product_units` (`uomId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transactionNumber` TEXT NOT NULL,
                `warehouseId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `transactionType` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unitCostMinor` INTEGER NOT NULL,
                `totalCostMinor` INTEGER NOT NULL,
                `balanceAfterQuantity` REAL NOT NULL,
                `referenceType` TEXT,
                `referenceId` TEXT,
                `notes` TEXT,
                `operatorName` TEXT NOT NULL,
                `transactionDate` INTEGER NOT NULL,
                FOREIGN KEY(`warehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_transactions_transactionNumber` ON `stock_transactions` (`transactionNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transactions_warehouseId` ON `stock_transactions` (`warehouseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transactions_productId` ON `stock_transactions` (`productId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transactions_transactionDate` ON `stock_transactions` (`transactionDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transactions_referenceType_referenceId` ON `stock_transactions` (`referenceType`, `referenceId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_balances` (
                `warehouseId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `currentQuantity` REAL NOT NULL,
                `reservedQuantity` REAL NOT NULL,
                `averageCostMinor` INTEGER NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                PRIMARY KEY(`warehouseId`, `productId`),
                FOREIGN KEY(`warehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_balances_warehouseId` ON `stock_balances` (`warehouseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_balances_productId` ON `stock_balances` (`productId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_adjustments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `adjustmentNumber` TEXT NOT NULL,
                `warehouseId` INTEGER NOT NULL,
                `adjustmentDate` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `approvedBy` TEXT NOT NULL,
                `totalValueMinor` INTEGER NOT NULL,
                FOREIGN KEY(`warehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_adjustments_adjustmentNumber` ON `stock_adjustments` (`adjustmentNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_warehouseId` ON `stock_adjustments` (`warehouseId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transferNumber` TEXT NOT NULL,
                `fromWarehouseId` INTEGER NOT NULL,
                `toWarehouseId` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `transferDate` INTEGER NOT NULL,
                `notes` TEXT,
                `transferredBy` TEXT NOT NULL,
                FOREIGN KEY(`fromWarehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`toWarehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_transfers_transferNumber` ON `stock_transfers` (`transferNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transfers_fromWarehouseId` ON `stock_transfers` (`fromWarehouseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transfers_toWarehouseId` ON `stock_transfers` (`toWarehouseId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `inventory_counts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `countNumber` TEXT NOT NULL,
                `warehouseId` INTEGER NOT NULL,
                `countDate` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `countedBy` TEXT NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`warehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_counts_countNumber` ON `inventory_counts` (`countNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_counts_warehouseId` ON `inventory_counts` (`warehouseId`)")

        // Purchasing
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `purchase_invoices` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `purchaseNumber` TEXT NOT NULL,
                `supplierId` INTEGER NOT NULL,
                `warehouseId` INTEGER NOT NULL,
                `supplierInvoiceRef` TEXT,
                `invoiceDate` INTEGER NOT NULL,
                `dueDate` INTEGER NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalAmountMinor` INTEGER NOT NULL,
                `paidAmountMinor` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `createdBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`warehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_invoices_purchaseNumber` ON `purchase_invoices` (`purchaseNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_supplierId` ON `purchase_invoices` (`supplierId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_warehouseId` ON `purchase_invoices` (`warehouseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_status` ON `purchase_invoices` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `purchase_invoice_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `purchaseInvoiceId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `quantity` REAL NOT NULL,
                `unitCostMinor` INTEGER NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalMinor` INTEGER NOT NULL,
                FOREIGN KEY(`purchaseInvoiceId`) REFERENCES `purchase_invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoice_items_purchaseInvoiceId` ON `purchase_invoice_items` (`purchaseInvoiceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoice_items_productId` ON `purchase_invoice_items` (`productId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `purchase_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `purchaseInvoiceId` INTEGER NOT NULL,
                `paymentReferenceNumber` TEXT NOT NULL,
                `amountPaidMinor` INTEGER NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `cashboxId` INTEGER,
                `paymentDate` INTEGER NOT NULL,
                `paidBy` TEXT NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`purchaseInvoiceId`) REFERENCES `purchase_invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_payments_purchaseInvoiceId` ON `purchase_payments` (`purchaseInvoiceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_payments_paymentDate` ON `purchase_payments` (`paymentDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `purchase_returns` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `returnNumber` TEXT NOT NULL,
                `purchaseInvoiceId` INTEGER NOT NULL,
                `returnAmountMinor` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `processedBy` TEXT NOT NULL,
                `returnDate` INTEGER NOT NULL,
                FOREIGN KEY(`purchaseInvoiceId`) REFERENCES `purchase_invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_returns_returnNumber` ON `purchase_returns` (`returnNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_returns_purchaseInvoiceId` ON `purchase_returns` (`purchaseInvoiceId`)")

        // Sales
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sales_invoices` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `saleNumber` TEXT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `warehouseId` INTEGER NOT NULL,
                `saleDate` INTEGER NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalAmountMinor` INTEGER NOT NULL,
                `paidAmountMinor` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `createdBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`warehouseId`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_invoices_saleNumber` ON `sales_invoices` (`saleNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoices_customerId` ON `sales_invoices` (`customerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoices_warehouseId` ON `sales_invoices` (`warehouseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoices_status` ON `sales_invoices` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sales_invoice_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `salesInvoiceId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `quantity` REAL NOT NULL,
                `unitPriceMinor` INTEGER NOT NULL,
                `subtotalMinor` INTEGER NOT NULL,
                `discountMinor` INTEGER NOT NULL,
                `taxMinor` INTEGER NOT NULL,
                `totalMinor` INTEGER NOT NULL,
                FOREIGN KEY(`salesInvoiceId`) REFERENCES `sales_invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoice_items_salesInvoiceId` ON `sales_invoice_items` (`salesInvoiceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoice_items_productId` ON `sales_invoice_items` (`productId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sales_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `salesInvoiceId` INTEGER NOT NULL,
                `paymentReferenceNumber` TEXT NOT NULL,
                `amountPaidMinor` INTEGER NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `cashboxId` INTEGER,
                `paymentDate` INTEGER NOT NULL,
                `receivedBy` TEXT NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`salesInvoiceId`) REFERENCES `sales_invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_payments_salesInvoiceId` ON `sales_payments` (`salesInvoiceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_payments_paymentDate` ON `sales_payments` (`paymentDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sales_returns` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `returnNumber` TEXT NOT NULL,
                `salesInvoiceId` INTEGER NOT NULL,
                `returnAmountMinor` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `processedBy` TEXT NOT NULL,
                `returnDate` INTEGER NOT NULL,
                FOREIGN KEY(`salesInvoiceId`) REFERENCES `sales_invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_returns_returnNumber` ON `sales_returns` (`returnNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_returns_salesInvoiceId` ON `sales_returns` (`salesInvoiceId`)")

        // Expenses
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expense_categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `glAccountId` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                FOREIGN KEY(`glAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_categories_categoryCode` ON `expense_categories` (`categoryCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_categories_glAccountId` ON `expense_categories` (`glAccountId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `expenseNumber` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `cashboxId` INTEGER NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `expenseDate` INTEGER NOT NULL,
                `paidTo` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `referenceInvoiceNo` TEXT,
                `paymentMethod` TEXT NOT NULL,
                `approvedBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `expense_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`cashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_expenseNumber` ON `expenses` (`expenseNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_cashboxId` ON `expenses` (`cashboxId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_expenseDate` ON `expenses` (`expenseDate`)")

        // Payments & Receipts
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `payment_methods` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `methodCode` TEXT NOT NULL,
                `nameAr` TEXT NOT NULL,
                `requiresReferenceNumber` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_methods_methodCode` ON `payment_methods` (`methodCode`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `receipts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `receiptNumber` TEXT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `cashboxId` INTEGER NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `paymentMethodCode` TEXT NOT NULL,
                `referenceNumber` TEXT,
                `receivedFrom` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `receiptDate` INTEGER NOT NULL,
                `receivedBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`cashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_receipts_receiptNumber` ON `receipts` (`receiptNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipts_customerId` ON `receipts` (`customerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipts_cashboxId` ON `receipts` (`cashboxId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipts_receiptDate` ON `receipts` (`receiptDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `receipt_allocations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `receiptId` INTEGER NOT NULL,
                `invoiceId` INTEGER NOT NULL,
                `allocatedAmountMinor` INTEGER NOT NULL,
                FOREIGN KEY(`receiptId`) REFERENCES `receipts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_allocations_receiptId` ON `receipt_allocations` (`receiptId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_allocations_invoiceId` ON `receipt_allocations` (`invoiceId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `paymentNumber` TEXT NOT NULL,
                `supplierId` INTEGER,
                `cashboxId` INTEGER NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `paymentMethodCode` TEXT NOT NULL,
                `referenceNumber` TEXT,
                `paidTo` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `paymentDate` INTEGER NOT NULL,
                `paidBy` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`cashboxId`) REFERENCES `cashboxes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payments_paymentNumber` ON `payments` (`paymentNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_supplierId` ON `payments` (`supplierId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_cashboxId` ON `payments` (`cashboxId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_paymentDate` ON `payments` (`paymentDate`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `payment_allocations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `paymentId` INTEGER NOT NULL,
                `purchaseInvoiceId` INTEGER NOT NULL,
                `allocatedAmountMinor` INTEGER NOT NULL,
                FOREIGN KEY(`paymentId`) REFERENCES `payments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`purchaseInvoiceId`) REFERENCES `purchase_invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_allocations_paymentId` ON `payment_allocations` (`paymentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_allocations_purchaseInvoiceId` ON `payment_allocations` (`purchaseInvoiceId`)")

        // Backup
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `backup_metadata` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `backupFileName` TEXT NOT NULL,
                `backupSizeBytes` INTEGER NOT NULL,
                `totalRecordsArchived` INTEGER NOT NULL,
                `checksumSha256` TEXT NOT NULL,
                `backupType` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `createdBy` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_backup_metadata_backupFileName` ON `backup_metadata` (`backupFileName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_backup_metadata_createdAt` ON `backup_metadata` (`createdAt`)")
    }
}
