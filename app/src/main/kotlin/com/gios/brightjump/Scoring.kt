package com.gios.brightjump

/**
 * One launchable activity as the package manager described it.
 *
 * Deliberately free of Android types so the ranking below can be unit tested on the JVM. The
 * only thing this app really does is pick the right row out of a list like this, and picking the
 * wrong one is silent: the icon opens *an* app and you don't notice it was the wrong one until
 * you go looking for a message that isn't there.
 */
data class Candidate(
    val packageName: String,
    val className: String,
    val label: String,
)

object Scoring {

    /**
     * Our own sideloaded apps. BrightChat is labelled close enough to "Chats" that a naive label
     * match opens it instead of the LightOS tool — and it is the one wrong answer that looks
     * most like the right one. Excluded by package, not by label, because the label is exactly
     * what makes it a false positive.
     */
    val SELF_FLEET_PREFIXES = listOf("com.gios.", "com.lightphone.")

    /** Label → score. Checked as a whole-string match first, lowercased and trimmed. */
    private val EXACT_LABEL_SCORES = mapOf(
        "chats" to 100,
        "chat" to 80,
        "messages" to 70,
        "messaging" to 65,
        "texts" to 50,
        "text" to 45,
        "sms" to 40,
    )

    /** Substring fallbacks, for a label like "Chats (beta)" or a localised variant. */
    private val PARTIAL_LABEL_SCORES = listOf(
        "chat" to 30,
        "messag" to 25,
        "sms" to 20,
    )

    /**
     * Package → bonus. A LightOS component beats a third-party messaging app with the same
     * label, which is what makes this safe to ship without hardcoding a class name that a
     * LightOS update can rename underneath us.
     */
    private val PACKAGE_BONUSES = listOf(
        "com.lightos" to 60,
        "lightos" to 40,
        "lightphone" to 30,
    )

    fun labelScore(label: String): Int {
        val l = label.trim().lowercase()
        EXACT_LABEL_SCORES[l]?.let { return it }
        for ((needle, score) in PARTIAL_LABEL_SCORES) {
            if (l.contains(needle)) return score
        }
        return 0
    }

    fun packageBonus(packageName: String): Int {
        val p = packageName.lowercase()
        if (p == "com.lightos") return 60
        for ((needle, bonus) in PACKAGE_BONUSES) {
            if (p.contains(needle)) return bonus
        }
        return 0
    }

    fun isExcluded(packageName: String, selfPackage: String): Boolean {
        if (packageName == selfPackage) return true
        return SELF_FLEET_PREFIXES.any { packageName.startsWith(it) }
    }

    fun score(candidate: Candidate, selfPackage: String): Int {
        if (isExcluded(candidate.packageName, selfPackage)) return 0
        val label = labelScore(candidate.label)
        if (label == 0) return 0
        return label + packageBonus(candidate.packageName)
    }

    /**
     * Highest score wins; ties break on package then class so the same phone always opens the
     * same thing. Returns null when nothing scored — an honest "not found" beats opening
     * whatever happened to be first.
     */
    fun best(candidates: List<Candidate>, selfPackage: String): Candidate? =
        candidates
            .map { it to score(it, selfPackage) }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<Candidate, Int>> { it.second }
                    .thenBy { it.first.packageName }
                    .thenBy { it.first.className }
            )
            .firstOrNull()
            ?.first
}
