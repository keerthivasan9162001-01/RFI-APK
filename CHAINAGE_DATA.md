# Embedded Chainage Data

## Package 1 – L&T
- Route: Krishnagiri to Thumbipadi
- Chainage: Km 94+000 to Km 180+000
- Source: user-supplied KML
- Embedded survey points: 861
- Typical interval: 100 m

## Package 2 – MVR
- Route: Thumbipadi to Namakkal
- Chainage: Km 180+000 to Km 248+625
- Source: user-supplied KMZ
- Embedded survey points: 688
- Typical interval: 100 m, with final marker at Km 248+625

## Detection
The app projects the phone GPS position to the nearest segment between adjacent chainage points and linearly interpolates the chainage. It also reports distance from the mapped alignment. The nearest of the two package alignments is used for package detection.

## Evidence fields saved
- Auto-detected package
- Current photo chainage
- Distance from mapped alignment
- GPS latitude/longitude
- GPS accuracy
- Work From/To chainage
- RFI number
- Activity
- Side/location
- Before/During/After stage
- Date/time
