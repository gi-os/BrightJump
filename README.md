<a href="https://github.com/sponsors/gi-os"><img src="https://raw.githubusercontent.com/gi-os/gi-os/main/images/bright-sponsors.svg" width="100%" alt="Thank you to my sponsors"></a>

# BrightJump

[**⬇ Download the latest APK**](https://github.com/gi-os/BrightJump/releases/latest) · free, open source.

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

Without signing credentials, the release APK is unsigned and debug builds use the standard Android
debug key. Neither can update an installed production APK.

### Release signing

Before merging the signing change, disable **Build & Release APK** so the merge does
not publish. In a private directory outside the checkout, use Android Build Tools
35.0.0 to generate the replacement key and create a proof of rotation from the original
release key. Use fresh passwords when prompted.

```sh
keytool -genkeypair -storetype JKS -keystore brightjump-new.jks -alias brightjump-release \
  -keyalg RSA -keysize 3072 -validity 10000

apksigner rotate --out brightjump.lineage \
  --old-signer --ks /path/to/original/brightjump.jks --ks-key-alias brightjump \
  --set-installed-data true --set-rollback false \
  --new-signer --ks brightjump-new.jks --ks-key-alias brightjump-release

apksigner lineage --in brightjump.lineage --print-certs
```

Verify that signer #1's SHA-256 digest matches `signing-lineage-root-fingerprint.txt`,
then replace `signing-fingerprint.txt` with signer #2's SHA-256 digest. Do not change
the original fingerprint. Keep the original private key out of CI; it is only needed
offline to create the lineage.

In [Settings → Environments](https://github.com/gi-os/BrightJump/settings/environments),
create an environment named `release`. Under **Deployment branches and tags**, choose
**Selected branches and tags** and add one **Branch** rule for `main`, with no other
branch or tag rules. Store the five signing secrets only in that environment. Remove
any repository-level copies and don't grant matching organization secrets to BrightJump:
the workflow's trigger filter alone does not protect repository-wide secrets.

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `brightjump-new.jks` |
| `SIGNING_LINEAGE_BASE64` | Base64-encoded `brightjump.lineage` |
| `KEYSTORE_PASSWORD` | New keystore password |
| `KEY_ALIAS` | `brightjump-release` |
| `KEY_PASSWORD` | New key password |

On Linux, upload the files without printing their contents and enter the other values when prompted:

```sh
base64 -w 0 brightjump-new.jks | gh secret set RELEASE_KEYSTORE_BASE64 --env release --repo gi-os/BrightJump
base64 -w 0 brightjump.lineage | gh secret set SIGNING_LINEAGE_BASE64 --env release --repo gi-os/BrightJump
gh secret set KEYSTORE_PASSWORD --env release --repo gi-os/BrightJump
gh secret set KEY_ALIAS --env release --repo gi-os/BrightJump --body brightjump-release
gh secret set KEY_PASSWORD --env release --repo gi-os/BrightJump
```

CI signs with the new key using APK Signature Scheme v3 and verifies the proof of rotation
from the original pin to the new pin. BrightJump requires Android API 29, so no supported
device needs a v1/v2 signature or the old private key. Android can accept an in-place update,
but a catalog that compares only the current certificate can still hide it from users on
the old signer. Coordinate the BrightMarket catalog/updater transition before re-enabling
publishing, and only re-enable it for an authorized release.

A push to `main` (other than ignored paths) or a manual run of `build.yml` on
`main` builds, signs, tags, and publishes. The `release` environment blocks the
job on other refs. Tag pushes do not trigger it. Branch and PR checks in
`check.yml` use no signing secrets and publish nothing.

## Support

These apps are free, open, and built on my own time. Sponsorship pays the bills that don't go away: build servers, test hardware, and the crash reporter that keeps them shipping. Donation or not my code is always free for the world to use.

[Sponsor on GitHub](https://github.com/sponsors/gi-os)
