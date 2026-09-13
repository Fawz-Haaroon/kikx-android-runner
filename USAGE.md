# Usage

This repo contains the Android runner for KIKX.

To run it on an Android device, you need the KIKX server running on the same device.

## 1. Get KIKX running

Clone the [KIKX](https://github.com/luvbyte/kikx) repository and start the server in Termux with the instructions mentioned in [KIKX](https://github.com/luvbyte/kikx).

Keep the KIKX running while using the Android runner.

## 2. Restore the Android project

Install [Sketchware Pro](https://github.com/Sketchware-Pro/Sketchware-Pro) on your Android device.

Download the `.swb` package from the release of this repository and import it into Sketchware Pro.

This will restore the complete KIKX Android Runner project.

## 3. Build and install the runner

Open the restored project in Sketchware Pro and compile it.

Install the generated APK on your Android device.

## 4. Run the Android runner

Make sure the KIKX server is still running in Termux.

Then open the KIKX Android Runner app.

The runner connects to the KIKX server at:

`http://127.0.0.1:1303`

With the server running, the KIKX interface should load inside the app.
