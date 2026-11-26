DAXWARE - GitHub Actions build

1. Extract this ZIP on your phone.
2. Create/sign in to a GitHub account.
3. Create a NEW repository named DAXWARE. For simplest setup, choose Public.
4. Upload the CONTENTS of this folder (not the outer DAXWARE_GitHub folder) to the repository root. The hidden .github/workflows/android.yml file must also be uploaded. GitHub's website may hide dot-folders in some mobile file pickers; if it does, use GitHub's web upload or desktop-site mode and verify the workflow file exists in the repo.
5. Open the repository's Actions tab.
6. Select "Build DAXWARE APK" and tap "Run workflow" (or push to main).
7. Wait for the workflow to finish with a green check.
8. Open the completed workflow run, scroll to Artifacts, and download DAXWARE-debug-apk. Extract it to get app-debug.apk.

Notes:
- This workflow builds a DEBUG APK, not a Play Store release.
- The APK is unsigned for store distribution but Android debug builds are installable for testing.
- Build has not been executed or device-tested in this chat.
- Shizuku must be installed/running and permission granted on the phone.
- DPI/resolution overrides can affect usability; use Restore in the app if needed.
- Vulkan is detected, not forced globally.
