package com.neorefraction.htm1_aiassistant.dictation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


const val DICTATION_REQUEST_CODE = 100
const val DICTATION_ACTION = "com.realwear.keyboard.intent.action.DICTATION"
const val DICTATION_PACKAGE = "com.realwear.wearhf.intent.extra.SOURCE_PACKAGE"

class DictationManager {

    fun start(context: Context) {
        val intent = Intent(DICTATION_ACTION).apply {
            putExtra(DICTATION_PACKAGE, context.packageName)
        }
        if (context is Activity) {
            context.startActivityForResult(intent, DICTATION_REQUEST_CODE)
        }
    }

    fun isResult(requestCode: Int, resultCode: Int): Boolean {
        return requestCode == DICTATION_REQUEST_CODE && resultCode == Activity.RESULT_OK
    }

    fun extractText(data: Intent?): String? {
        return data?.getStringExtra("result")
    }

    fun receiver(onCommand: (String?) -> Unit): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val command = intent.getStringExtra("command")
                onCommand(command)
            }
        }
    }
}