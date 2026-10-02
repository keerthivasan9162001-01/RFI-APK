# Sharvesh Field – RFI Photo Proof v2

Android field-evidence app for NH-44 maintenance RFIs.

## Added in v2
- Offline automatic chainage detection from the user-supplied project KML/KMZ.
- Package auto-detection:
  - L&T – Krishnagiri to Thumbipadi, Km 94+000 to Km 180+000
  - MVR – Thumbipadi to Namakkal, Km 180+000 to Km 248+625
- Uses 861 chainage points from the first KML and 688 points from the second KMZ.
- Interpolates between 100 m markers, so the live result can be e.g. Km 212+483.
- Shows distance from the mapped alignment and GPS accuracy warnings.
- Separates `Current photo chainage` from the RFI `Work From–To chainage`.
- Burns current chainage, package, coordinates, date/time and stage into the saved photo.
- CSV evidence register now stores package, auto chainage and alignment offset.
- Offline after install; no account/login required.

## Existing workflow
Enter RFI/activity, select side and BEFORE/DURING/AFTER, capture, permanently stamp, save and share.

## Important field rule
For final evidence, wait for good GPS accuracy. The app warns when GPS accuracy is weaker than ±20 m or the phone is more than 250 m from the mapped corridor.

## Build
The included `.github/workflows/build-apk.yml` can build a debug APK using GitHub Actions.
