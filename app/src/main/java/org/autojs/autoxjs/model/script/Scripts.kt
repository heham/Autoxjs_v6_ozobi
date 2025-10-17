package org.autojs.autoxjs.model.script

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.Nullable
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.SimpleScriptExecutionListener
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.autojs.script.ScriptSource
import com.stardust.util.IntentUtil
import org.autojs.autoxjs.Pref
import org.autojs.autoxjs.R
import org.autojs.autoxjs.autojs.AutoJs
import org.autojs.autoxjs.external.ScriptIntents
import org.autojs.autoxjs.external.fileprovider.AppFileProvider
import org.autojs.autoxjs.external.shortcut.Shortcut
import org.autojs.autoxjs.external.shortcut.ShortcutActivity
import org.mozilla.javascript.RhinoException
import java.io.File
import java.io.FileFilter
import org.autojs.autoxjs.ui.edit.EditActivity as EditActivity1

/**
 * Created by Stardust on 2017/5/3.
 */

object Scripts {

    const val ACTION_ON_EXECUTION_FINISHED = "ACTION_ON_EXECUTION_FINISHED"
    const val EXTRA_EXCEPTION_MESSAGE = "message"
    const val EXTRA_EXCEPTION_LINE_NUMBER = "lineNumber"
    const val EXTRA_EXCEPTION_COLUMN_NUMBER = "columnNumber"

    val FILE_FILTER = FileFilter { file ->
        file.isDirectory || file.name.endsWith(".js")
                || file.name.endsWith(".auto")
    }

    private val BROADCAST_SENDER_SCRIPT_EXECUTION_LISTENER =
        object : SimpleScriptExecutionListener() {

            override fun onSuccess(execution: ScriptExecution, result: Any?) {
                GlobalAppContext.get()?.sendBroadcast(Intent(ACTION_ON_EXECUTION_FINISHED))
            }

            override fun onException(execution: ScriptExecution, e: Throwable) {
                val rhinoException = getRhinoException(e)
                var line = -1
                var col = 0
                if (rhinoException != null) {
                    line = rhinoException.lineNumber()
                    col = rhinoException.columnNumber()
                }
                if (ScriptInterruptedException.causedByInterrupted(e)) {
                    GlobalAppContext.get()?.sendBroadcast(
                        Intent(ACTION_ON_EXECUTION_FINISHED)
                            .putExtra(EXTRA_EXCEPTION_LINE_NUMBER, line)
                            .putExtra(EXTRA_EXCEPTION_COLUMN_NUMBER, col)
                    )
                } else {
                    GlobalAppContext.get()?.sendBroadcast(
                        Intent(ACTION_ON_EXECUTION_FINISHED)
                            .putExtra(EXTRA_EXCEPTION_MESSAGE, e.message)
                            .putExtra(EXTRA_EXCEPTION_LINE_NUMBER, line)
                            .putExtra(EXTRA_EXCEPTION_COLUMN_NUMBER, col)
                    )
                }
            }

        }

    fun openByOtherApps(uri: Uri) {
        openByOtherApps(uri,"text/plain")
    }

    fun openByOtherApps(file: File) {
        openByOtherApps(Uri.fromFile(file))
    }

    fun openByOtherApps(uri: Uri, mime:String = "text/plain") {
        IntentUtil.viewFile(GlobalAppContext.get(), uri, mime, AppFileProvider.AUTHORITY)
    }

    // 创建快捷方式
    fun createShortcut(scriptFile: ScriptFile) {
        GlobalAppContext.get().let {
            Shortcut(it).name(scriptFile.simplifiedName)
                .targetClass(ShortcutActivity::class.java)
                .iconRes(R.drawable.ic_node_js_black)
                .extras(Intent().putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptFile.path))
                .send()
        }
    }


    fun edit(context: Context, file: ScriptFile) {
//        if (Pref.getEditor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            EditActivity2.editFile(context, file)
//        } else {
        EditActivity1.editFile(context, file.simplifiedName, file.path, false)
//        }
    }

    fun edit(context: Context, path: String) {
        edit(context, ScriptFile(path))
    }

    fun run(file: ScriptFile): ScriptExecution? {
        return try {
            AutoJs.getInstance().scriptEngineService.get()?.execute(
                file.toSource(),
                file.parent?.let { ExecutionConfig(workingDirectory = it) }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalAppContext.get().let {
                Toast.makeText(it, e.message, Toast.LENGTH_LONG).show()
            }
            null
        }

    }


    fun run(source: ScriptSource): ScriptExecution? {
        return try {
            AutoJs.getInstance().scriptEngineService.get()?.execute(
                source,
                ExecutionConfig(workingDirectory = Pref.getScriptDirPath())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalAppContext.get().let {
                Toast.makeText(it, e.message, Toast.LENGTH_LONG).show()
            }
            null
        }

    }

    fun runWithBroadcastSender(file: File): ScriptExecution? {
        return AutoJs.getInstance().scriptEngineService.get()?.execute(
            ScriptFile(file).toSource(), BROADCAST_SENDER_SCRIPT_EXECUTION_LISTENER,
            file.parent?.let { ExecutionConfig(workingDirectory = it) }
        )
    }


    fun runRepeatedly(
        scriptFile: ScriptFile,
        loopTimes: Int,
        delay: Long,
        interval: Long
    ): ScriptExecution? {
        val source = scriptFile.toSource()
        val directoryPath = scriptFile.parent
        return AutoJs.getInstance().scriptEngineService.get()?.execute(
            source, directoryPath?.let {
                ExecutionConfig(
                    workingDirectory = it,
                    delay = delay, loopTimes = loopTimes, interval = interval
                )
            }
        )
    }

    @Nullable
    fun getRhinoException(throwable: Throwable?): RhinoException? {
        var e = throwable
        while (e != null) {
            if (e is RhinoException) {
                return e
            }
            e = e.cause
        }
        return null
    }

    fun send(file: ScriptFile) {
        val context = GlobalAppContext.get()
        context?.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(
                        Intent.EXTRA_STREAM,
                        IntentUtil.getUriOfFile(context, file.path, AppFileProvider.AUTHORITY)
                    ),
                GlobalAppContext.getString(R.string.text_send)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    }
}
