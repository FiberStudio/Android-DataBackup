package com.xayah.databackup.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * All fields are defined here.
 *
 */
@Entity
data class PackageRestoreEntire(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var packageName: String,
    var label: String,
    @ColumnInfo(defaultValue = "0") var backupOpCode: Int, // Defined during the backup processing, limits the range of choices.
    @ColumnInfo(defaultValue = "0") var operationCode: Int,
    @ColumnInfo(defaultValue = "0") var timestamp: Long,
    var versionName: String,
    var versionCode: Long,
)