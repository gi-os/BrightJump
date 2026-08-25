package com.gios.brightjump

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * The whole app. Resolve, start, finish — no layout is ever inflated and the window is
 * translucent, so the toolbox icon reads as Chats opening directly.
 *
 * Everything happens in onCreate rather than onResume so that finish() lands before the window
 * would have been drawn.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = ChatsResolver.resolve(this)
        val dumpOnly = intent?.getBooleanExtra("dump", false) == true

        // `adb shell am start -n com.gios.brightjump/.MainActivity --ez dump true` prints every
        // launcher activity with its score and opens nothing. This is how you find out what the
        // Chats tool is actually called on a given LightOS build without installing a debugger.
        if (dumpOnly) {
            Log.i(ChatsResolver.TAG, ChatsResolver.describe(this, result))
            Toast.makeText(this, "Dumped ${result.candidates.size} entries to logcat", Toast.LENGTH_SHORT).show()
            finishAndNoAnimation()
            return
        }

        val target = result.intent
        if (target == null) {
            Log.w(ChatsResolver.TAG, ChatsResolver.describe(this, result))
            Toast.makeText(this, "Couldn't find Chats", Toast.LENGTH_SHORT).show()
            finishAndNoAnimation()
            return
        }

        Log.i(ChatsResolver.TAG, "opening via ${result.how}: ${target.component ?: target.data}")
        try {
            startActivity(target)
        } catch (e: Exception) {
            // Resolvable and launchable are different things: a component can resolve and still
            // refuse the start on a permission or a disabled user.
            Log.w(ChatsResolver.TAG, "start failed\n" + ChatsResolver.describe(this, result), e)
            Toast.makeText(this, "Couldn't open Chats", Toast.LENGTH_SHORT).show()
        }
        finishAndNoAnimation()
    }

    @Suppress("DEPRECATION")
    private fun finishAndNoAnimation() {
        finish()
        // The transition is between two windows neither of which is ours; animating it just adds
        // a frame of nothing on a phone this slow.
        overridePendingTransition(0, 0)
    }
}
