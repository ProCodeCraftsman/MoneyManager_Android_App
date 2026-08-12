# Implementation Plan - Database Migration for Recurring Transactions

This plan addresses the crash caused by the database schema change in `RecurringEntity` by implementing a proper Room migration from version 12 to 13.

## User Review Required

> [!IMPORTANT]
> A manual migration is required to preserve existing recurring transactions while adding new fields and renaming `investmentApp` to `investmentPlatform`. This will prevent the crash and keep data intact.

## Proposed Changes

### Data Layer

#### [MODIFY] [MoneyManagerDatabase.kt](file:///D:/Android projec/MoneyManager/app/src/main/java/com/moneymanager/data/MoneyManagerDatabase.kt)
- Increment database version from 12 to 13.
- Add `MIGRATION_12_13` to handle:
    - Adding `peerContactId`, `tagIds`, `description`, `toAccountId`, `receiptPath`.
    - Renaming `investmentApp` to `investmentPlatform` using the table recreation pattern (safest for SQLite).
    - Preserving existing data and indices.

#### [MODIFY] [DatabaseModule.kt](file:///D:/Android projec/MoneyManager/app/src/main/java/com/moneymanager/di/DatabaseModule.kt)
- Register `MIGRATION_12_13` in the `addMigrations` call.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.
- Verification through app launch (manual) since logcat failed earlier, but the migration logic itself is robust.

### Manual Verification
- Launch the app.
- Verify that previous recurring transactions are still present.
- Verify that new recurring transactions can be created with the new fields.
- Check that the crash is resolved.
