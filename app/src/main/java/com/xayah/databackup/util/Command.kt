package com.xayah.databackup.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import com.google.gson.Gson
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.data.AppInfo
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class Command {
    companion object {
        fun ls(path: String): Boolean {
            Shell.cmd("ls -i $path").exec().apply {
                if (!this.isSuccess)
                    App.log.add(this.out.joinToString(separator = "\n"))
                return this.isSuccess
            }
        }

        fun rm(path: String): Boolean {
            Shell.cmd("rm -rf $path").exec().apply {
                if (!this.isSuccess)
                    App.log.add(this.out.joinToString(separator = "\n"))
                return this.isSuccess
            }
        }

        fun mkdir(path: String): Boolean {
            Shell.cmd("mkdir -p $path").exec().apply {
                if (!this.isSuccess)
                    App.log.add(this.out.joinToString(separator = "\n"))
                return this.isSuccess
            }
        }

        fun unzip(filePath: String, outPath: String) {
            if (mkdir(outPath)) Shell.cmd("unzip $filePath -d $outPath").exec()
        }

        private fun cp(src: String, dst: String): Boolean {
            return Shell.cmd("cp $src $dst").exec().isSuccess
        }

        fun unzipByZip4j(filePath: String, outPath: String) {
            try {
                ZipFile(filePath).extractAll(outPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getAppList(context: Context, room: Room?): MutableList<AppEntity> {
            val appList: MutableList<AppEntity> = mutableListOf()
            room?.let {
                val packageManager = context.packageManager
                val userId = context.readBackupUser()
                val packages = packageManager.getInstalledPackages(0)
                val listPackages = Bashrc.listPackages(userId).second
                for ((index, j) in listPackages.withIndex())
                    listPackages[index] = j.replace("package:", "")
                for (i in packages) {
                    try {
                        if (i.packageName == "com.xayah.databackup" || listPackages.indexOf(i.packageName) == -1)
                            continue
                        if (!context.readIsSupportSystemApp())
                            if ((i.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                                continue
                        val appIcon = i.applicationInfo.loadIcon(packageManager)
                        val appName = i.applicationInfo.loadLabel(packageManager).toString()
                        val packageName = i.packageName
                        var appEntity = room.findByPackage(packageName)
                        if (appEntity == null) {
                            appEntity =
                                AppEntity(0, appName, packageName, getAppVersion(packageName))
                        } else {
                            appEntity.appName = appName
                        }
                        room.insertOrUpdate(appEntity)
                        appEntity.icon = appIcon
                        try {
                            val backupPath = context.readBackupSavePath() + "/$userId"
                            Shell.cmd("cat ${backupPath}/${packageName}/info").exec().apply {
                                if (this.isSuccess) {
                                    try {
                                        val appInfo =
                                            Gson().fromJson(
                                                this.out.joinToString(),
                                                AppInfo::class.java
                                            )
                                        appEntity.appInfo = appInfo
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        appList.add(appEntity)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        break
                    }
                }
            }
            return appList
        }

        fun getAppList(context: Context, path: String): MutableList<AppEntity> {
            val appList: MutableList<AppEntity> = mutableListOf()
            val packages = Shell.cmd("ls $path").exec().out
            for (i in packages) {
                val exec = Shell.cmd("cat ${path}/${i}/info").exec()
                if (!exec.isSuccess)
                    continue
                try {
                    val appInfoLocal = Gson().fromJson(exec.out.joinToString(), AppInfo::class.java)
                    val appEntity =
                        AppEntity(
                            0,
                            appInfoLocal.appName,
                            appInfoLocal.packageName,
                            backupApp = false,
                            backupData = false
                        ).apply {
                            icon = AppCompatResources.getDrawable(
                                context, R.drawable.ic_round_android
                            )
                            backupPath = "${path}/${i}"
                            appEnabled = ls("${path}/${i}/apk.tar*")
                            dataEnabled = ls("${path}/${i}/user.tar*") ||
                                    ls("${path}/${i}/data.tar*") ||
                                    ls("${path}/${i}/obb.tar*")
                            appInfo = appInfoLocal
                        }
                    appList.add(appEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return appList
        }

        fun extractAssets(mContext: Context, assetsPath: String, outName: String) {
            try {
                val assets = File(Path.getFilesDir(mContext), outName)
                if (!assets.exists()) {
                    val outStream = FileOutputStream(assets)
                    val inputStream = mContext.resources.assets.open(assetsPath)
                    inputStream.copyTo(outStream)
                    assets.setExecutable(true)
                    assets.setReadable(true)
                    assets.setWritable(true)
                    outStream.flush()
                    inputStream.close()
                    outStream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getABI(): String {
            val mABIs = Build.SUPPORTED_ABIS
            if (mABIs.isNotEmpty()) {
                return mABIs[0]
            }
            return ""
        }

        fun compress(
            compressionType: String,
            dataType: String,
            packageName: String,
            outPut: String,
            dataPath: String,
            dataSize: String? = null
        ): Boolean {
            if (dataType == "media") {
                countSize(dataPath, 1).apply {
                    if (this == dataSize) {
                        App.log.add(GlobalString.noUpdateAndSkip)
                        return true
                    }
                }
            } else {
                countSize(
                    "${dataPath}/${packageName}",
                    1
                ).apply {
                    if (this == dataSize) {
                        App.log.add(GlobalString.noUpdateAndSkip)
                        return true
                    }
                }
            }
            Bashrc.compress(compressionType, dataType, packageName, outPut, dataPath).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.compressFailed)
                    return false
                }
            }
            return true
        }

        fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String,
            userId: String,
            apkSize: String? = null
        ): Boolean {
            val apkPathPair = Bashrc.getAPKPath(packageName, userId).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add("${packageName}: ${GlobalString.pathNotExist}")
                    return false
                }
            }
            countSize(
                apkPathPair.second,
                1
            ).apply {
                if (this == apkSize) {
                    App.log.add(GlobalString.noUpdateAndSkip)
                    return true
                }
            }
            Bashrc.cd(apkPathPair.second).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add("${apkPathPair.second}: ${GlobalString.pathNotExist}")
                    return false
                }
            }
            Bashrc.compressAPK(compressionType, outPut).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.compressApkFailed)
                    return false
                }
            }
            Bashrc.cd("~").apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add("~: ${GlobalString.pathNotExist}")
                    return false
                }
            }
            return true
        }

        private fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String,
            dataPath: String
        ): Boolean {
            Bashrc.decompress(compressionType, dataType, inputPath, packageName, dataPath).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.decompressFailed)
                    return false
                }
            }
            return true
        }

        private fun getCompressionTypeByName(name: String): String {
            val item = name.split(".")
            when (item.size) {
                2 -> {
                    when (item[1]) {
                        "tar" -> {
                            return "tar"
                        }
                    }
                }
                3 -> {
                    when ("${item[1]}.${item[2]}") {
                        "tar.zst" -> {
                            return "zstd"
                        }
                        "tar.lz4" -> {
                            return "lz4"
                        }
                    }
                }
            }
            return ""
        }

        fun installAPK(
            inPath: String,
            packageName: String,
            userId: String,
            versionCode: String
        ): Boolean {
            if (versionCode <= getAppVersionCode(userId, packageName)) {
                App.log.add(GlobalString.noUpdateAndSkip)
                return true
            }

            // 禁止APK验证
            Bashrc.setInstallEnv()

            Bashrc.installAPK(inPath, packageName, userId).apply {
                when (this.first) {
                    0 -> return true
                    else -> {
                        App.log.add(this.second)
                        App.log.add(GlobalString.installApkFailed)
                        return false
                    }
                }
            }
        }

        private fun setOwnerAndSELinux(
            dataType: String,
            packageName: String,
            path: String,
            userId: String
        ) {
            Bashrc.setOwnerAndSELinux(dataType, packageName, path, userId).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.setSELinuxFailed)
                    return
                }
            }
        }

        fun restoreData(
            packageName: String,
            inPath: String,
            userId: String
        ): Boolean {
            var result = true
            val fileList = Shell.cmd("ls $inPath | grep -v apk.* | grep .tar").exec().out
            for (i in fileList) {
                val item = i.split(".")
                val dataType = item[0]
                var dataPath = ""
                val compressionType = getCompressionTypeByName(i)
                if (compressionType.isNotEmpty()) {
                    when (dataType) {
                        "user" -> {
                            dataPath = Path.getUserPath(userId)
                        }
                        "data" -> {
                            dataPath = Path.getDataPath(userId)
                        }
                        "obb" -> {
                            dataPath = Path.getObbPath(userId)
                        }
                    }
                    decompress(
                        compressionType,
                        dataType,
                        "${inPath}/${i}",
                        packageName,
                        dataPath
                    ).apply {
                        if (!this)
                            result = false
                    }
                    if (dataPath.isNotEmpty())
                        setOwnerAndSELinux(
                            dataType,
                            packageName,
                            "${dataPath}/${packageName}",
                            userId
                        )
                }
            }
            return result
        }

        private fun getAppVersion(packageName: String): String {
            Bashrc.getAppVersion(packageName).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.getAppVersionFailed)
                    return ""
                }
                return this.second
            }
        }

        private fun getAppVersionCode(userId: String, packageName: String): String {
            Bashrc.getAppVersionCode(userId, packageName).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.getAppVersionCodeFailed)
                    return ""
                }
                return this.second
            }
        }

        fun generateAppInfo(
            appName: String,
            userId: String,
            packageName: String,
            apkSize: String,
            userSize: String,
            dataSize: String,
            obbSize: String,
            outPut: String
        ): Boolean {
            val appInfo = AppInfo(
                appName, packageName,
                getAppVersion(packageName),
                getAppVersionCode(userId, packageName),
                apkSize,
                userSize,
                dataSize,
                obbSize
            )
            return object2JSONFile(appInfo, "${outPut}/info")
        }

        fun decompressMedia(inputPath: String, name: String, dataPath: String): Boolean {
            Bashrc.decompress(
                getCompressionTypeByName(name),
                "media",
                "${inputPath}/$name",
                "media",
                dataPath
            )
                .apply {
                    if (!this.first) {
                        App.log.add(this.second)
                        App.log.add(GlobalString.decompressFailed)
                        return false
                    }
                }
            return true
        }

        fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Boolean {
            Bashrc.testArchive(compressionType, inputPath).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add(GlobalString.broken)
                    return false
                }
            }
            return true
        }

        fun testArchiveForEach(inPath: String): Boolean {
            var result = true
            val fileList = Shell.cmd("ls $inPath | grep .tar").exec().out
            for (i in fileList) {
                val compressionType = getCompressionTypeByName(i)
                if (compressionType.isNotEmpty()) {
                    testArchive(compressionType, "${inPath}/${i}").apply {
                        if (!this)
                            result = false
                    }
                }
            }
            return result
        }

        fun backupItself(
            packageName: String,
            outPut: String,
            userId: String
        ): Boolean {
            mkdir(outPut)
            val apkPath = Bashrc.getAPKPath(packageName, userId)
            val apkPathPair = apkPath.apply {
                if (!this.first) {
                    App.log.add(this.second)
                    App.log.add("${packageName}: ${GlobalString.pathNotExist}")
                    return false
                }
            }
            val apkSize = countSize("${outPut}/DataBackup.apk", 1)
            countSize(apkPathPair.second, 1).apply {
                if (this == apkSize) {
                    App.log.add(GlobalString.noUpdateAndSkip)
                    return true
                }
            }
            cp("${apkPathPair.second}/base.apk", "${outPut}/DataBackup.apk").apply {
                if (!this) {
                    App.log.add("${packageName}: ${GlobalString.pathNotExist}")
                    return false
                }
            }
            return true
        }

        fun object2JSONFile(src: Any, outPut: String): Boolean {
            try {
                val json = Gson().toJson(src)
                Bashrc.writeToFile(json, outPut).apply {
                    if (!this.first) {
                        App.log.add(this.second)
                        return false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }

        fun countSize(path: String, type: Int = 0): String {
            Bashrc.countSize(path, type).apply {
                if (!this.first) {
                    App.log.add(this.second)
                    return ""
                }
                return this.second
            }
        }
    }
}