# Build the APK automatically with GitHub Actions

The included `.github/workflows/build-apk.yml` can compile the Android app in the cloud.

1. Create a GitHub repository and upload the contents of this project.
2. Open the repository's **Actions** tab.
3. Run **Build RFI Photo Proof APK**.
4. When the run finishes, download the artifact named **RFI-Photo-Proof-APK**.
5. The artifact contains `app-debug.apk`, which can be installed on an Android phone after allowing installation from that source.

No code changes are required for the first build.
