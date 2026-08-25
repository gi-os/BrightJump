# v1.0.0 — first release

One toolbox icon that opens the LightOS **Chats** tool. No UI, no window, no delay.

**What's in it**

- Four-strategy resolver: `sms:` scheme → `CATEGORY_APP_MESSAGING` → launcher-activity ranking →
  default SMS package. First one that resolves wins.
- No hardcoded component name. A LightOS update that renames its classes does not break this.
- Our own apps are excluded by package, so BrightChat can never be picked by mistake.
- `--ez dump true` prints the full ranking to logcat without opening anything.
- ~20 kB APK: no Compose, no androidx, no light-common.

**Known unknown**

Which of the four strategies actually fires on LightOS hasn't been confirmed on a device yet.
Run the dump command in the README after installing and the logcat line says which one won.
