# BrightJump

One icon in the LightOS toolbox that opens the built-in **Chats** tool, and nothing else.

Press it and Chats is on screen. BrightJump draws no window of its own — it resolves a target,
starts it, and finishes inside `onCreate`, so there is no frame of black in between.

## Why it exists

LightOS decides what the toolbox shows and in what order. BrightJump is a sideloaded entry, so
it sits wherever you want it: at the top of the list, bound to a hardware key through
[BrightControl](https://github.com/gi-os/BrightControl), or pinned by a launcher like
[Luma](https://github.com/vandamd/Luma).

## How it finds Chats

There is no hardcoded component name anywhere in this repo, and that is on purpose.

Reading Luma's source was the starting point — it is the launcher that does open LightOS tools,
so it seemed likely to know a secret. It doesn't. `MainViewModel.launchApp` enumerates
`LauncherApps.getActivityList()` and starts whatever comes back by `ComponentName`; its handling
of a package with several activities is generic (`activityInfo.last()`, or a stored activity
name if the user picked one). There is no LightOS-specific intent in it. So the real answer is
"ask the system what is installed, then pick" — and if you're going to ask, you may as well ask
three cheaper questions first.

`ChatsResolver` tries four strategies in order and stops at the first that resolves:

| # | Strategy | Assumes |
| --- | --- | --- |
| 1 | `ACTION_VIEW` on `sms:` | Chats handles the SMS scheme |
| 2 | `ACTION_MAIN` + `CATEGORY_APP_MESSAGING` | Chats declares itself the messaging app |
| 3 | Rank every launcher activity by label and package (the Luma route) | Chats is a launcher entry |
| 4 | `Telephony.Sms.getDefaultSmsPackage` → `getLaunchIntentForPackage` | LightOS owns SMS |

Strategy 4 is last because `getLaunchIntentForPackage` returns `null` for a launcher, and on
this phone the SMS owner and the launcher are plausibly the same `com.lightos`.

Ranking lives in `Scoring.kt`, which has no Android imports so the tests run on the JVM. The
rule that earns its keep: **our own apps are excluded by package**. BrightChat is labelled close
enough to "Chats" to outrank a LightOS tool called "Messages", and picking it would be a silent
failure — the icon opens *an* app, and you don't notice it was the wrong one until you go
looking for a message that isn't there.

## Finding out what Chats is actually called

The resolver prints its whole ranking to logcat on failure. To see it without failing:

```
adb shell am start -n com.gios.brightjump/.MainActivity --ez dump true
adb logcat -d -s BrightJump
```

That opens nothing and lists every launcher activity on the phone with its score, package and
class. If strategy 3 is picking the wrong row, this is the output to read.

## Package visibility

`minSdk` is 29 but `targetSdk` is 35, so API 30 package-visibility filtering applies: without a
`<queries>` block the resolver sees nothing but itself and every strategy returns `null` on a
phone where Chats is plainly installed. The manifest declares the three intents the ladder asks
about rather than taking `QUERY_ALL_PACKAGES` — same visibility, no policy exception needed.

## Build

```
./gradlew :app:assembleRelease
```

The signing keystore is committed with its password, as it is across the fleet, so a local build
and a CI build produce interchangeable APKs and either upgrades over the other. It is a public
key with a public password and protects nothing; treat it as an identity, not a secret. CI pins
its fingerprint (`signing-fingerprint.txt`) and fails the build if it drifts, because a changed
certificate turns every Obtainium update into an opaque `Failure: Invalid`.

A push to `main` builds, signs, tags and releases. Push a branch first — `check.yml` runs the
same compile and the tests while publishing nothing.

## Support

These apps are free, open, and built on my own time. Sponsorship pays the bills that don't go away: build servers, test hardware, and the crash reporter that keeps them shipping. Donation or not my code is always free for the world to use.

[Sponsor on GitHub](https://github.com/sponsors/gi-os)
