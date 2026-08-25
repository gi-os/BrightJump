package com.gios.brightjump

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.util.Log

/**
 * Finds a way into the LightOS Chats tool.
 *
 * Four strategies, tried in order, each returning a launchable intent or null. They are ordered
 * by how much they assume: the first asks the platform a question every Android phone answers,
 * the last asks LightOS by name.
 *
 * The reason for a ladder rather than one hardcoded component: reading Luma's source
 * (github.com/vandamd/Luma) shows it never names a LightOS class anywhere. It enumerates
 * LauncherApps.getActivityList() and starts whatever comes back by ComponentName — its handling
 * of packages with several activities is generic. So there is no secret component name to copy;
 * there is only "ask the system what is installed, then pick". This does the same thing, with
 * three cheaper questions in front of it.
 */
object ChatsResolver {

    const val TAG = "BrightJump"

    /** What the resolver did, so a failure can say something more useful than "no". */
    data class Result(val intent: Intent?, val how: String, val candidates: List<Candidate>)

    fun resolve(context: Context): Result {
        val seen = mutableListOf<Candidate>()

        // 1. The sms: scheme. The Chats tool is the phone's SMS app, and an SMS app that cannot
        //    open its own thread list from a bare `sms:` is broken in ways that would be widely
        //    known. Cheapest question, most likely answer, and immune to a class rename.
        smsSchemeIntent(context)?.let { return Result(it, "sms: scheme", seen) }

        // 2. CATEGORY_APP_MESSAGING. The standard "take me to the messaging app" category.
        //    Distinct from 1 because a handler for sms: is not required to declare this, and an
        //    app that declares this is not required to handle sms:.
        messagingCategoryIntent(context)?.let { return Result(it, "CATEGORY_APP_MESSAGING", seen) }

        // 3. The Luma route: every launcher activity on the phone, ranked by label and package.
        //    This is what catches a Chats tool that is a plain launcher entry with no messaging
        //    intent filters at all.
        val candidates = launcherCandidates(context)
        seen += candidates
        Scoring.best(candidates, context.packageName)?.let { c ->
            return Result(componentIntent(c.packageName, c.className), "launcher label: ${c.label}", seen)
        }

        // 4. Ask the platform which package owns SMS and launch it whole. Last because
        //    getLaunchIntentForPackage returns null for a launcher, and on this phone the SMS
        //    owner and the launcher are plausibly the same com.lightos.
        defaultSmsPackageIntent(context)?.let { return Result(it, "default SMS package", seen) }

        return Result(null, "nothing resolved", seen)
    }

    private fun smsSchemeIntent(context: Context): Intent? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    private fun messagingCategoryIntent(context: Context): Intent? {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_MESSAGING)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    private fun defaultSmsPackageIntent(context: Context): Intent? {
        val pkg = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull() ?: return null
        return context.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Every launcher activity the manifest's <queries> block lets us see.
     *
     * No MATCH_ALL. That flag turns this into one binder call carrying every activity on the
     * phone and it has thrown TransactionTooLargeException on a loaded boot before; the default
     * flags answer the same question for our purposes. Wrapped anyway — a resolver that crashes
     * is worse than one that returns an empty list and says so.
     */
    fun launcherCandidates(context: Context): List<Candidate> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching {
            pm.queryIntentActivities(intent, 0).map {
                Candidate(
                    packageName = it.activityInfo.packageName,
                    className = it.activityInfo.name,
                    label = runCatching { it.loadLabel(pm).toString() }.getOrDefault(""),
                )
            }
        }.onFailure { Log.w(TAG, "queryIntentActivities failed", it) }.getOrDefault(emptyList())
    }

    private fun componentIntent(pkg: String, cls: String): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(pkg, cls)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Dumped to logcat on failure, and on demand via `--ez dump true`. */
    fun describe(context: Context, result: Result): String {
        val rows = (result.candidates.ifEmpty { launcherCandidates(context) })
            .sortedByDescending { Scoring.score(it, context.packageName) }
            .joinToString("\n") {
                "  ${Scoring.score(it, context.packageName)}  ${it.label}  ${it.packageName}/${it.className}"
            }
        return "how=${result.how}\ntarget=${result.intent?.component ?: result.intent?.data ?: "none"}\n$rows"
    }
}
