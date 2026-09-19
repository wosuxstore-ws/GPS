# Fake GPS

A simple learning app: type in a latitude/longitude, hit Start, and other apps
on the phone (Google Maps included) will read that point as your current
location instead of your real one. Hit Stop and your real GPS location comes
back immediately.

## How it works

Android has a built-in "mock location provider" system, meant for developers
to test location-based apps without physically moving around. This app plugs
into that system (`LocationManager.addTestProvider` /
`setTestProviderLocation`) and feeds it the coordinates you type in, once a
second, for as long as the service is running.

## One-time setup on your phone (required)

Android will not let *any* app fake location until you explicitly allow it:

1. Go to **Settings → About phone**, and tap **Build number** 7 times to
   unlock Developer options.
2. Go to **Settings → System → Developer options**.
3. Find **Select mock location app** and choose **Fake GPS** (this app).

Without this step, tapping Start will just show a toast asking you to do it —
Android blocks the mock-location API entirely otherwise, there's no way
around that from inside the app.

## Building the APK (GitHub Actions)

1. Create a new GitHub repo and upload everything in this folder to it,
   `.github` folder included (GitHub's drag-and-drop upload can miss hidden
   folders — if so, use **Add file → Create new file**, type
   `.github/workflows/build-apk.yml` as the filename, and paste in that
   file's contents).
2. Commit. The **Actions** tab will start a build automatically.
3. Once it's green, open the run, scroll to **Artifacts**, and download
   `FakeGPS-debug-apk`. Unzip it to get `app-debug.apk`.
4. Copy that to your phone and install it (you'll need "install unknown
   apps" allowed for whichever app you use to open the file).

## Notes

- This only affects apps that read location through Android's normal
  location APIs. Some apps (banking, ride-share, some games) specifically
  check `Location.isFromMockProvider()` and will refuse to trust a mock
  location — that's the app protecting itself, not a bug here.
- The "direction" arrow you see on Google Maps' blue dot when you're
  standing still comes from the phone's real compass, not from GPS — this
  app can't fake that part, only the position itself.
