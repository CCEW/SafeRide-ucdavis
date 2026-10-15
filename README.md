# UC Davis Safe Ride: Transparent Queue Prototype

A native Android prototype of a redesigned **UC Davis Safe Ride** app, built for ECS 191.

Safe Ride gives students a free, safe ride at night in marked vans, from one campus location to another or from campus to a home in Davis. The current app has two trust problems. Its wait-time countdown jumps around (20 minutes, then 5), and its "Driver Arrived" alert often fires before the van has actually stopped.

This prototype focuses on one core loop and on fixing those two problems:

> **Request a ride → watch your place in line go down → get alerted only when your van is really there.**

| Problem in the current app | What this prototype does instead |
|---|---|
| Countdown timers that jump around | A **queue position** ("Position in Line: #3") with a pickup estimate that only ever goes down |
| "Driver Arrived" before the van stops | The alert fires **only inside a strict 100 ft (30.48 m) radius** of the pickup spot, with vibration, the van's license plate and the exact stop |
| No idea where vans are before booking | **Live van locations** on the home map, a "3 Vans Active • Estimated Campus Wait: ~12 mins" banner, and the nearest van's ETA on the review screen |

The layout follows screenshots of the current app: Welcome, Home, Where to?, Where are you?, How Many Passengers?, Review your trip, and Ride Not Available.

---

## Tech stack and native requirement

- **Native Android only:** Kotlin, Jetpack Compose and Material 3. No React Native, Flutter, JavaScript, Expo, Ionic or WebViews.
- **Map:** [osmdroid](https://github.com/osmdroid/osmdroid) 6.1.20, a native Android `MapView` that shows OpenStreetMap tiles. It needs **no API key**. The tiles are color-filtered to match the current app's muted grey map.
- **State:** a single `ViewModel` holding a `StateFlow`, plus Kotlin coroutines.
- **Backend:** none. There is no public UC Davis Safe Ride API, so the app uses a mock service (see [Mock API](#mock-api)).

| | Version |
|---|---|
| Package | `edu.ucdavis.saferide` |
| Min SDK / Target SDK | 26 (Android 8.0) / 37 |
| Kotlin | 2.2.10 |
| Android Gradle Plugin / Gradle | 9.4.1 / 9.6.0 |
| Compose BOM | 2026.02.01 |

---

## Prerequisites

- **Android Studio 2026.2.1 or newer.** The project was built and tested with 2026.2.1. Android Gradle Plugin 9.4 needs a recent Studio.
- **JDK:** the JDK bundled with Android Studio (JetBrains Runtime 25) works. Nothing extra to install.
- **Android SDK Platform 37.** Studio offers to install it on first sync if it's missing.
- **An emulator or phone running Android 8.0 (API 26) or newer.** Tested on a `Medium_Phone_API_37.0` emulator.
- **Internet access** on the emulator or phone, for map tiles. The rest of the app works offline.

---

## How To Build and run

### In Android Studio

1. **File → Open…** and select the **`android-test`** folder (not the repository root).
2. Wait for Gradle sync to finish. If Studio asks to install SDK Platform 37 or other components, accept.
3. Pick an emulator from the device menu, or create one in **Device Manager**, e.g. *Medium Phone, API 37*.
4. Press **Run ▶**.

### From the command line

From `android-test/`:

```bash
# macOS / Linux / Git Bash
./gradlew assembleDebug          # build the APK
./gradlew installDebug           # install on the running emulator or connected phone
./gradlew testDebugUnitTest      # run the unit tests

# Windows (cmd / PowerShell)
gradlew.bat assembleDebug installDebug testDebugUnitTest
```

If Gradle can't find a JDK on the command line, point `JAVA_HOME` at Studio's bundled one. On Windows that's `C:\Program Files\Android\Android Studio\jbr`; on macOS it's `/Applications/Android Studio.app/Contents/jbr/Contents/Home`.

The debug APK is written to `android-test/app/build/outputs/apk/debug/app-debug.apk`.

---

## Core features

The whole loop takes about a minute. The rider is always **Alex (UC Davis Student)**; there is no real account.

### 1. Sign in and look before you book
1. On the **Welcome** screen, tap **Get Started**.
2. The sign-in screen is a simulated version of the UC Davis sign-in page. Tap **Sign In** (no password).
3. **Home** shows the live banner **"3 Vans Active • Estimated Campus Wait: ~12 mins"**, the gold service area, and Vans **#1, #2 and #4** on the map. The blue dot is your (simulated) location.

### 2. Choose the trip
1. Tap **Where to?** and pick **West Village**. Either type "West" or go to **Explore → Off Campus Housing**.
2. On **Where are you?**, pick **Shields Library**, either by typing it or from **Explore → Campus Locations**. You can also tap **Current Location / Set on Map** and drag the map; the app finds the nearest campus stop and shows how far you'd walk.
3. Enter **1** passenger and tap the **→** button.
4. **Review your trip** shows the pickup spot on the map, the trip timeline, and **"Nearest van: Van #… • ~… from your pickup spot"**. You can edit passengers or add a note for the driver here.

### 3. Request and watch the queue
Tap **Continue**. Timings below are counted from that tap.

| Time | What you see |
|---|---|
| ~0 s | **Calculating your place in line…** |
| ~0.6 s | **You're #3 in line • Estimated pickup ~12 mins** |
| ~2.6 s | Live tracking. Card: **"Request Received • Position in Line: #3"**. The map highlights the pickup spot with the gold 100 ft circle. |
| ~7.6 s | **"Request Received • Position in Line: #2"**, and the estimate drops to ~8 mins |
| ~12.6 s | **"Van #4 Assigned • Position in Line: #1 • Driver ~2 mins away"**. Van #4 starts driving its blue route toward you. |

### 4. Proximity arrival alert
- When Van #4 enters the **100 ft (30.48 m)** radius, which takes about 10 more seconds, the green **Driver Arrived** banner slides in: **"Van #4 is at the Shields Library Bus Turnaround"** with license plate **8UCD404**.
- The phone vibrates three times. The alert never fires early: the van slows down near the stop, and the alert waits until it's inside the circle.

### 5. Ride and reset
1. Tap **I'm in the Van**. Van #4 drives to West Village (about 12 seconds).
2. **Trip Complete** shows a summary. Tap **End Trip** to return to the Home map.

### Also try
- **Ride Not Available:** pick **Sol at Parkside** (off campus) as the *pickup*. After **Continue** you get **"Ride Not Available… Rides must start on-campus."** **Go Back** returns to the review.
- **Cancel:** while waiting in line, tap **Cancel Ride** or press Back. Confirming takes you Home.

---

## Mock API

There's no public UC Davis API, so all data comes from a decoupled mock behind an interface:

- [`SafeRideService`](android-test/app/src/main/java/edu/ucdavis/saferide/data/SafeRideService.kt): the interface. A real backend would implement this.
- [`MockSafeRideService`](android-test/app/src/main/java/edu/ucdavis/saferide/data/MockSafeRideService.kt): the simulation. It uses `MockSafeRideService.shared` as the app-wide singleton.

| Method | What it does |
|---|---|
| `fetchCampusStatus()` | After simulated network latency, returns 3 active vans and a ~12 min campus wait |
| `requestRide(pickup, dropoff, …)` | Returns a `RideRequest` with `queuePosition = 3`. It throws `RideUnavailableException` if the pickup is off campus or the drop-off is outside the service area. |
| `startSimulatedQueueUpdates(ride)` | A timer-driven `Flow` that emits updates every 5 s: queue position 2 → position 1 with Van #4 assigned → van positions every 0.5 s → `status = ARRIVED` once inside 30.48 m |
| `startSimulatedTrip(ride)` | After boarding: the van drives to the drop-off, then `status = COMPLETED` |

Timings live in `SimulationConfig`. The 100 ft rule is in [`Proximity.kt`](android-test/app/src/main/java/edu/ucdavis/saferide/domain/Proximity.kt), and the booking rules are in [`TripRules.kt`](android-test/app/src/main/java/edu/ucdavis/saferide/domain/TripRules.kt).

---

## Project structure

```
android-test/app/src/main/java/edu/ucdavis/saferide/
  data/        Models, campus places, SafeRideService + MockSafeRideService
  domain/      Pure logic: distances, routes, 100 ft rule, trip rules, queue estimate, card text
  ui/
    SafeRideViewModel.kt   App state, screen stack, ride simulation
    SafeRideApp.kt         Picks the screen, handles Back and transitions
    welcome/   Welcome + simulated sign-in
    home/      Home (map, banner, service hours), Settings, full-screen map
    places/    Where to?, Where are you?, Explore lists, Set on Map
    trip/      Passengers, Review your trip, Ride Not Available
    ride/      Calculating, live tracking + arrival alert, Trip Complete, haptics
    map/       osmdroid map wrapper and marker drawing
    components/ Shared header, list rows, buttons
    theme/     UC Davis Aggie Blue (#022851) and Aggie Gold (#FFBF00)
```

## Tests

`./gradlew testDebugUnitTest` runs 41 JVM unit tests. They cover:
- The distance math and the 100 ft rule. From every pickup spot, no update before arrival is inside the radius, and the arrival update is.
- The queue sequence and its timing.
- The trip rules.
- The full screen flow through the ViewModel: booking, Ride Not Available, live tracking, boarding, End Trip and cancel.

---

## Known limitations

- **Simulated data:**
  - There's no real dispatch: Van #4 is always the one assigned.
  - "Current Location" is a fixed spot on the Quad, since emulators have no real GPS fix.
- **Approximate geography:** campus coordinates, the campus boundary and the service-area outline are hand-placed approximations.
- **Out of scope:** service hours aren't enforced, so rides are available at any time for demos, and "Now" is a label rather than a scheduling menu.
- **Map:**
  - Tiles come from OpenStreetMap's public servers, which is fine for a class demo; a production app would need its own tile provider.
  - In dark mode, the map tiles are color-inverted rather than a purpose-made dark map style.
- **Portrait only:** the app is locked to portrait, like the current Safe Ride app.
- **Emulator keyboard:** the passengers field asks for the number keyboard, but some emulator keyboards still show letters.
