# openpyn-nordvpn-juiceSSH, a JuiceSSH plugin
An Android app written in C/C++, [Java](https://www.oracle.com/java/), and [Kotlin](https://kotlinlang.org/) to run [Openpyn](https://github.com/jotyGill/openpyn-nordvpn) remotely through JuiceSSH. Special thanks to [NvidiaGpuMonitor](https://github.com/sds100/NvidiaGpuMonitor), a JuiceSSH plugin written in Kotlin by [sds100](https://github.com/sds100) which served as a base for this JuiceSSH plugin.

>**Note**:
>This project is still in development, some UI changes and API changes will be made before first public release.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [How it works](#how-it-works)
- [How to use](#how-to-use)
- [Download](#download)
- [Requirements](#requirements)
- [Libraries used](#libraries-used)
- [Links used](#links-used)
- [Credits](#credits)
- [References](#references)
- [Feedback](#feedback)
- [Contributing](#contributing)
- [Built with](#built-with)
- [Attributions](#attributions)
- [Acknowledgments](#acknowledgments)
- [License](#license)

## Introduction

The idea all started when I wanted to connect to OpenVPN servers hosted by NordVPN on a [Asus RT-AC86U](https://www.asus.com/Networking/RT-AC86U/). By default this is possible, but the default firmware including third party firmware [Asuswrt-merlin](https://asuswrt.lostrealm.ca/) only allow for a maximum of 5 OpenVPN Clients to be saved. I then stumbled on [Openpyn](https://github.com/jotyGill/openpyn-nordvpn), quickly learned Python, and made a pull request, enabling support for Asuswrt-merlin. Openpyn is a python3 script which can be run on [Entware-ng-3x on Asuswrt-merlin](https://gist.github.com/1951FDG/3cada1211df8a59a95a8a71db6310299#file-asuswrt-merlin-md). The main feature of Openpyn, is that it automatically connects to the least busy, lowest latency OpenVPN server. NVRAM write support for Asuswrt-merlin in Openpyn is then able to save the least busy, lowest latency OpenVPN server to the NVRAM of a Entware-ng-3x enabled ASUS router. Now, I had achieved more or less what I desired, but this left me with one last struggle, having to resort to open a SSH connection to the ASUS router and supplying Openpyn with the desired arguments e.g., country, load threshold, server type, and number of pings to be sent to each server to determine quality. I thought that having the ability to do that on my phone instead of my computer would make this a lot easier, and that led me to discovering [JuiceSSH](https://juicessh.com/). JuiceSSH supports the use of plugins, which allowed me to create this fantastic app. This app runs on Android, it establishes a connection to a device that has a SSH server running on it, and it is then able to send a Openpyn command to the remote device.

>**Note**:
>This project is my very first Android project, and as such, it may not follow all the best coding practices yet, regardless of this, I'm committed to make this app a source of inspiration for other developers working on similar based Android apps, especially Google Maps based Android apps.

## Features

- Supports most arguments available in Openpyn with easy to use preferences
- Allows to use location based filtering in Openpyn
- Map view displays markers for every country supported by NordVPN
- Allows to hide countries in the map view
- Allows to star a country in the map view
- API keys stored in shared preferences are encrypted ("AES/GCM/NoPadding")
- On app start, it will use a smart location, to determine the closest country
- A floating action button (FAB), to determine current location based on the current public IP address
- Uses Geolocation APIs to determine the current location based on the current public IP address
  - http://ip-api.com/, https://ipdata.co/, https://ipinfo.io/, https://ipstack.com/

*Am I missing some essential feature?*

- Submit an [issue](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/issues/new) and let's make this app better together!

## How it works

A lot of components make this app work, I'll cover some of the basics here. Basically on app startup, the map is loaded asynchronously, an MBTile file (SQLite database) located in the assets resource folder within the APK is loaded and then stored and read in memory. The [world.mbtiles](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/assets/world.mbtiles) was generated using a custom python script, [generate_tiles_multiprocess.py](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/generate_tiles_multiprocess.py).

```
rm ./app/src/main/assets/world.mbtiles
./generate_tiles_multiprocess.py ./mapfile.xml ./app/src/main/assets/world.mbtiles 1 4 --format webp
```

After the map is done loading, the NordVPN API is invoked to query all the supported countries, filtering based on preferences such as server type is done here, markers are generated lazily for all the countries (markers are not placed on the map), all tiles (512x512 WebP images) are pre-loaded for the minimum zoom scale specified by the MBTile file. The closest country is determined (based on the current public IP address), if no connection is available, the last know location to Android is used instead. The map then animates to the marker closest to this specific location. After animation completes, only the "lazy" markers whose location are within the visible bounds of the map are made visible (markers are placed on the map once they are made visible for the first time).

## How to use

- Install [JuiceSSH](https://juicessh.com/)
- Add a new connection to Connections in JuiceSSH
- Install this app (download not available yet)
- When prompted, enable/allow the permissions required by this app
- Change any app settings as required
- Select a Country by selecting a marker on the map
- Click the colored floating action button (FAB), to send a Openpyn command to the remote device

>**Note**:
>You can use this flow with multiple remote devices, as long as that remote device has a SSH server running on it and is configured in Connections in JuiceSSH and is selected in the app toolbar of this app before the colored floating action button (FAB) is clicked!

## Download

No download available yet, [watch](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/subscription) this repository in the meanwhile...

## Requirements
To compile and run the project you'll need:

- [Android Studio](https://developer.android.com/studio/) `3.x`
- [Android SDK](https://developer.android.com/studio/releases/platforms) `9 (API level 28)`
- Android SDK Build-Tools
- CMake
- Android SDK Platform-Tools
- Android SDK Tools
- [Android NDK](https://developer.android.com/ndk/)
- ConstraintLayout for Android
- Solver for ConstraintLayout
- Android Support Repository
- Google repository
- Geolocation APIs

## Libraries used

* [SQLite](https://sqlite.org/android/doc/trunk/www/install.wiki)
* [AndroidX](https://developer.android.com/topic/libraries/support-library/androidx-rn)
* [Anko](https://github.com/Kotlin/anko)
* [Barista](https://github.com/SchibstedSpain/Barista)
* [Kotlin coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [emoji-java](https://github.com/vdurmont/emoji-java)
* [countryboundaries](https://github.com/westnordost/countryboundaries)
* [Moshi](https://github.com/square/moshi)
* [Kotlin](https://github.com/JetBrains/kotlin)
* [StaticLog](https://github.com/jupf/staticlog)
* [JuiceSSH Plugin Library](https://github.com/1951FDG/juicessh-pluginlibrary)
* [Fuel](https://github.com/kittinunf/Fuel)
* [Result](https://github.com/kittinunf/Result)
* [MultiSelectDialog](https://github.com/1951FDG/Android-Multi-Select-Dialog)
* [Morphing Material Dialogs](https://github.com/AdityaAnand1/Morphing-Material-Dialogs)
* [Minibar](https://github.com/mayuroks/minibar)
* [ProgressToolbar](https://github.com/1951FDG/ProgressToolbar)

## Links used

Mapnik
- https://github.com/mapnik/mapnik/wiki/Aspect-Fix-Mode
- https://github.com/mapnik/mapnik/wiki/MapnikRenderers
- https://github.com/mapnik/mapnik/wiki/Image-IO

Tiles
- https://wiki.openstreetmap.org/wiki/Zoom_levels
- https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
- http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/

## Credits
This app uses (modified) code from several open source projects.

- [SQLite-NDK](https://github.com/KrystianBigaj/sqlite-ndk)
  - Modified [sqlite3ndk.h](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/cpp/sqlite3ndk.h)
  - Modified [sqlite3ndk.cpp](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/cpp/sqlite3ndk.cpp)


- [Android Maps Extensions](https://github.com/mg6maciej/android-maps-extensions)
  - Modified
[LazyMarker.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/com/androidmapsextensions/lazy/LazyMarker.java)


- [Map Utils](https://github.com/antoniocarlon/MapUtils)
  - Modified [CameraUpdateAnimator.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/com/antoniocarlon/map/CameraUpdateAnimator.java)


- [Android Network Utility](https://github.com/evert-arias/android-network-utility)
  - Modified [NetworkInfo.kt](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/com/ariascode/networkutility/NetworkInfo.kt)


- [Android Google Maps API v2 Add-ons](https://github.com/cocoahero/android-gmaps-addons)
  - Modified [MapBoxOfflineTileProvider.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/com/cocoahero/android/gmaps/addons/mapbox/MapBoxOfflineTileProvider.java)


- [Mobile Export Script for Illustrator](https://github.com/austynmahoney/mobile-export-scripts-illustrator)
  - Modified [export_assets_to_android_ios.jsx](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/export_assets_to_android_ios.jsx)


- [Old XML format Mapnik stylesheets](https://github.com/openstreetmap/mapnik-stylesheets)
  - Modified [generate_tiles_multiprocess.py](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/generate_tiles_multiprocess.py)


- [Google Maps Android API utility library](https://github.com/kiddouk/CheckableFloatingActionButton)
  - [Point.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/com/google/maps/android/geometry/Point.java)
  - [SphericalMercatorProjection.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/com/google/maps/android/projection/SphericalMercatorProjection.java)


- [Checkable Floating Button](https://github.com/kiddouk/CheckableFloatingActionButton)
  - Modified [CheckableFloatingActionButton.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/io/errorlab/widget/CheckableFloatingActionButton.java)
  - Modified [CheckedSavedState.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/io/errorlab/widget/CheckedSavedState.java)

>**Note**:
>Special thanks to Yesy, author of [Read SQLite Database from Android Asset Resource](https://www.codeproject.com/Articles/1235533/Read-SQLite-Database-from-Android-Asset-Resource)

## References

- [MyStorage.kt](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/io/github/sdsstudios/nvidiagpumonitor/MyStorage.kt) inspired by blog post, [Save and retrieve ArrayList of Object in SharedPreference: Android](https://readyandroid.wordpress.com/save-and-retrieve-arraylist-of-object-in-sharedpreference-android/) from Ready Android.

- [PrintArray.kt](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/io/github/sdsstudios/nvidiagpumonitor/PrintArray.kt) inspired by Github repo, [PrintArray
](https://github.com/Tobibur/PrintArray) by Tobibur Rahman.

- [SecurityManager.java](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/blob/master/app/src/main/java/io/github/sdsstudios/nvidiagpumonitor/SecurityManager.java) inspired by blog post, [Making secured version of EditTextPreference](https://blog.nikitaog.me/2014/11/09/making-secured-edittextpreference/) by Nikita Ogorodnikov.
  - [How to make the perfect Singleton? – Exploring Code – Medium](https://medium.com/exploring-code/how-to-make-the-perfect-singleton-de6b951dfdb0)
  - [Basic Android Encryption Do’s and Don’ts – Vincent Huang – Medium](https://medium.com/@tiensinodev/basic-android-encryption-dos-and-don-ts-7bc2cd3335ff)
  - [Android Security: Beware of the default IV! – Dorian Cussen – SystemDotRun](https://doridori.github.io/Android-Security-Beware-of-the-default-IV/)

## Feedback

Feel free to send us feedback by submitting an [issue](https://github.com/1951FDG/openpyn-nordvpn-juiceSSH/issues/new). Bug reports, feature requests, patches, and well-wishes are always welcome.

## Contributing
Pull requests are welcome. For major changes, please submit an issue first to discuss what you would like to change.

## Built with

- [Adobe Illustrator](https://www.adobe.com/products/illustrator.html)
- [Android Studio](https://developer.android.com/studio/)
- [Atom](https://atom.io/)
- [AX2J](http://ax2j.sickworm.com/) - Android XML to Java
- [Crashlytics for Android](https://fabric.io/kits/android/crashlytics)
- [DB Browser for SQLite](http://sqlitebrowser.org/)
- [Detekt](https://github.com/arturbosch/detekt)
- [Fastlane](https://fastlane.tools/)
- [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/intro)
- [MBUtil](https://github.com/mapbox/mbutil) - Importer and Exporter of MBTiles
- [Python](https://www.python.org/downloads/)
- [QuickDemo](https://github.com/PSPDFKit-labs/QuickDemo)
- [Regex101](https://regex101.com/)
- [Sourcetree](https://www.sourcetreeapp.com/)
- [TileMill](https://tilemill.s3.amazonaws.com/dev/TileMill-v0.10.1-291-g31027ed.zip) - Exporter of Mapnik XML files

## Attributions
- [Natural Earth Map Data](https://www.naturalearthdata.com/downloads/50m-physical-vectors/)
- [Country Flags Icons](https://www.flaticon.com/packs/countrys-flags)
- [Global Logistics Icons](https://www.flaticon.com/packs/global-logistics-2)

## Acknowledgments

Many thanks to [Sonelli](https://github.com/Sonelli) who made this project possible and painless. Special thanks to [Krystian Bigaj](https://github.com/KrystianBigaj), author of [SQLite-NDK](https://github.com/KrystianBigaj/sqlite-ndk).

## License

Haven't decided or figured this out yet.
