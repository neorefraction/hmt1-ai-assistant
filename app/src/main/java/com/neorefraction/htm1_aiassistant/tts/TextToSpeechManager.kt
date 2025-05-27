package com.neorefraction.htm1_aiassistant.tts

import android.content.Context
import android.content.Intent

const val ACTION_TTS = "com.realwear.wearhf.intent.action.TTS"
const val EXTRA_TEXT = "text_to_speak"
const val EXTRA_ID = "tts_id"
const val EXTRA_PAUSE = "pause_speech_recognizer"
const val TTS_REQUEST_CODE = 34

class TextToSpeechManager {
    fun speak(context: Context, text: String) {
        val intent = Intent(ACTION_TTS).apply {
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_ID, TTS_REQUEST_CODE)
            putExtra(EXTRA_PAUSE, false)
        }
        context.sendBroadcast(intent)
    }
}
