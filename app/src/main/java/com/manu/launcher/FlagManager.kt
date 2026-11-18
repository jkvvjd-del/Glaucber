package com.manu.launcher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import rikka.shizuku.Shizuku
import java.io.File
import java.lang.reflect.Method

/**
 * Multi-path FastFlag writer + anti-reset re-apply.
 *
 * 1) No full root: Shizuku is the real path; direct write + all-files is best-effort.
 * 2) Anti-reset: last JSON in prefs, reapplyLast() before every Roblox launch.
 * 3) Multi-target + chmod 666 so client is more likely to read the file.
 */
object FlagManager {

    private const val PREFS = "manu_flags"
    private const val KEY_JSON = "last_json"

    private val TARGETS = listOf(
        "/data/local/tmp/ClientAppSettings.json",
        "/sdcard/Android/data/com.roblox.client/files/ClientSettings/ClientAppSettings.json",
        "/sdcard/Android/data/com.roblox.client.vnggames/files/ClientSettings/ClientAppSettings.json",
        "/storage/emulated/0/Android/data/com.roblox.client/files/ClientSettings/ClientAppSettings.json",
        "/storage/emulated/0/Android/data/com.roblox.client.vnggames/files/ClientSettings/ClientAppSettings.json"
    )

    fun hasRoot(): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor() == 0
    } catch (_: Exception) {
        false
    }

    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    fun accessMode(): String = when {
        hasRoot() -> "Root ✓"
        shizukuReady() -> "Shizuku ✓"
        else -> "ограничен (нужен Shizuku)"
    }

    fun requestShizukuPermission(requestCode: Int = 0) {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
            ) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) {
        }
    }

    fun saveLastJson(ctx: Context, json: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_JSON, json).apply()
    }

    fun loadLastJson(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_JSON, "") ?: ""

    private fun newShizukuProcess(cmd: Array<String>): Process? = try {
        val method: Method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(null, cmd, null, null) as Process
    } catch (_: Exception) {
        null
    }

    fun runElevated(cmd: String, log: (String) -> Unit = {}): Boolean {
        return try {
            val proc = when {
                hasRoot() -> Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                shizukuReady() -> newShizukuProcess(arrayOf("sh", "-c", cmd))
                else -> {
                    log("нет Root/Shizuku — elevated skip")
                    return false
                }
            } ?: return false
            val out = proc.inputStream.bufferedReader().readText().trim()
            val err = proc.errorStream.bufferedReader().readText().trim()
            val code = proc.waitFor()
            log("elevated exit=$code out=$out err=$err")
            code == 0
        } catch (e: Exception) {
            log("elevated: ${e.message}")
            false
        }
    }

    private fun tryDirectWrite(json: String, log: (String) -> Unit): Int {
        var ok = 0
        for (path in TARGETS) {
            try {
                val f = File(path)
                f.parentFile?.mkdirs()
                f.writeText(json)
                f.setReadable(true, false)
                f.setWritable(true, false)
                if (f.exists() && f.length() > 0) {
                    ok++
                    log("direct OK: $path")
                }
            } catch (e: Exception) {
                log("direct fail: $path (${e.message})")
            }
        }
        return ok
    }

    private fun elevatedWrite(localPath: String, log: (String) -> Unit): Boolean {
        val copies = TARGETS.joinToString(" ; ") { t ->
            val dir = t.substringBeforeLast('/')
            "mkdir -p '$dir' 2>/dev/null ; cp '$localPath' '$t' 2>/dev/null ; chmod 666 '$t' 2>/dev/null"
        }
        return runElevated("$copies ; echo OK", log)
    }

    fun writeFlags(ctx: Context, json: String, log: (String) -> Unit = {}): Boolean {
        saveLastJson(ctx, json)
        val cache = ctx.externalCacheDir ?: ctx.cacheDir
        val local = File(cache, "ClientAppSettings.json")
        return try {
            local.writeText(json)
            log("local: ${local.absolutePath} (${local.length()} bytes)")
            val direct = tryDirectWrite(json, log)
            val elev = elevatedWrite(local.absolutePath, log)
            val ok = elev || direct > 0
            log(if (ok) "APPLY OK (elev=$elev direct=$direct)" else "APPLY FAIL — нужен Shizuku/Root")
            ok
        } catch (e: Exception) {
            log("writeFlags: ${e.message}")
            false
        }
    }

    fun reapplyLast(ctx: Context, log: (String) -> Unit = {}): Boolean {
        val json = loadLastJson(ctx)
        if (json.isBlank()) {
            log("reapply: нет сохранённых флагов")
            return false
        }
        log("reapply last JSON…")
        return writeFlags(ctx, json, log)
    }

    fun clearFlags(ctx: Context, log: (String) -> Unit = {}): Boolean {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_JSON).apply()
        val rm = TARGETS.joinToString(" ; ") { "rm -f '$it' 2>/dev/null" }
        val elev = runElevated("$rm ; echo OK", log)
        var direct = 0
        for (p in TARGETS) {
            try {
                if (File(p).delete()) direct++
            } catch (_: Exception) {
            }
        }
        log("clear elev=$elev directDeleted=$direct")
        return elev || direct > 0
    }

    fun killBackground(log: (String) -> Unit = {}): Boolean {
        return runElevated("am kill-all 2>/dev/null; echo OK", log)
    }

    fun needsAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()
}
