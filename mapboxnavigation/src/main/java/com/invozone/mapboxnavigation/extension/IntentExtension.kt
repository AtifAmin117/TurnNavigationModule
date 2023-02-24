package com.invozone.mapboxnavigation.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract


class ActivityResult {
    var resultCode: Int = Activity.RESULT_CANCELED
    var intent: Intent? = null
}

class CustomStartActivityResult : ActivityResultContract<Intent, ActivityResult>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        return input!!
    }
    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult().apply {
            this.resultCode = resultCode
            this.intent = intent
        }
    }
}