Change Log
==========

Version 1.3.0-SNAPSHOT

Version 1.2.2
* Fixed visibility of `UUIDUtil`. Deprecated it. Introduced `AdvertisedServiceUUIDExtractor` helper, thanks marciogranzotto! (https://github.com/Polidea/RxAndroidBle/pull/184)

Version 1.2.1
* Added `ByteArrayBatchObservable` helper for splitting long byte arrays
* Fixed behaviour in non-Bluetooth environments. (https://github.com/Polidea/RxAndroidBle/issues/158)
* Fixed `RxBleConnectionMock` overwriting `BluetoothCharacteristic` value on setting notification. (https://github.com/Polidea/RxAndroidBle/issues/160)
* Fixed default payload size of Long Write operation when non-standard MTU was negotiated (https://github.com/Polidea/RxAndroidBle/issues/161)
* Added possibility to get the current MTU value of `RxBleConnection` (https://github.com/Polidea/RxAndroidBle/issues/166)
* Fixed retrying of `RxBleClient.scanBleDevices(UUID...)`, thanks BharathMG! (https://github.com/Polidea/RxAndroidBle/pull/174)
* Fixed connection not being noticed as established due to a race condition (https://github.com/Polidea/RxAndroidBle/issues/178)
* Fixed `BleBluetoothGattCallbackTimeout` macAddress being null on connection (https://github.com/Polidea/RxAndroidBle/issues/178)
* Fixed disconnect operation behaviour in an edge case situation (https://github.com/Polidea/RxAndroidBle/issues/178)

Version 1.2.0
* Added Proguard rules for the library. (https://github.com/Polidea/RxAndroidBle/issues/104)
* Added support for MTU negotiation, thanks pregno!
* Fixed connecting with autoConnect = true on Android 7.0.0+, thanks JIUgia!
* Fixed notifications for characteristics with the same UUID
* Adjusted scan location services check for various scenarios (https://github.com/Polidea/RxAndroidBle/issues/106)
* Fixed occasional out-of-order emissions from notifications (https://github.com/Polidea/RxAndroidBle/issues/75)
* Fixed stalled operations by adding timeouts (https://github.com/Polidea/RxAndroidBle/issues/118)
* Added `LocationServicesOkObservable` helper for observing if scan can be successfully started
* Added Jack compatibility (https://github.com/Polidea/RxAndroidBle/issues/123)
* Added compatibility mode for notifications on characteristic that do not contain a Client Characteristic Config descriptor
* Fixed logic of UUID filtering during scan
* Added long write support (https://github.com/Polidea/RxAndroidBle/issues/68)
* Fixed issue with a stalled library when write operations were too quick (https://github.com/Polidea/RxAndroidBle/issues/135)
* Optimised logging
* Added support for custom `BluetoothGatt` operations (https://github.com/Polidea/RxAndroidBle/issues/137)
* Updated `RxJava` to 1.2.9
* Added support for scanning on Android Wear
* Internal refactoring introducing Dagger2 support

Version 1.1.0
* Fixed issue that sometimes happened where `RxBleRadioOperationConnect` was not yet subscribed while running. (https://github.com/Polidea/RxAndroidBle/issues/94)
* Fixed issue with descriptor writing using parent characteristic write type. (https://github.com/Polidea/RxAndroidBle/issues/93)
* Added `BleScanException.toString()` for a more descriptive stacktrace.
* Added a workaround for a bug while discovering services. (https://github.com/Polidea/RxAndroidBle/issues/86)
* Added a timeout for discovering services. (https://github.com/Polidea/RxAndroidBle/issues/86)
* Fixed calling `BluetoothGatt.disconnect()` on a correct thread. (https://github.com/Polidea/RxAndroidBle/issues/84)
* Fixed library stuck if disconnection happened during operation execution. (https://github.com/Polidea/RxAndroidBle/issues/81)
* Removed reflection call to `BluetoothGatt.connect()` on Android 7.0.0+. (https://github.com/Polidea/RxAndroidBle/issues/83)
* Removed android.support.v4 dependency.
* Added cancelling of connection establishing process. 
* Reduced method count.
* Fixed `RejectedExecutionException` when processing `BluetoothGattCallback`. (https://github.com/Polidea/RxAndroidBle/issues/25) (https://github.com/Polidea/RxAndroidBle/issues/75)

Version 1.0.2

* Added Mock RxAndroidBle to the repository
* Added indications handling on RxBleConnection
* Fixed scan operation concurrency issue (https://github.com/Polidea/RxAndroidBle/issues/17)
* Exposed android.bluetooth.BluetoothDevice in RxBleDevice (https://github.com/Polidea/RxAndroidBle/issues/23)
* Fixed stale RxBleRadio on RxBleOperation unhandled throw (https://github.com/Polidea/RxAndroidBle/issues/18)
* Fixed possible BluetoothCharacteristic value overwrites with multiple writes (https://github.com/Polidea/RxAndroidBle/issues/27)
* Updated rxJava (1.1.0 -> 1.1.7) and rxAndroid (1.1.0 -> 1.2.1) libraries dependency
* Added interface methods for usage with BluetoothCharacteristic object (https://github.com/Polidea/RxAndroidBle/issues/38)
* Fixed lost connection when BluetoothAdapter disabled before the connection established (https://github.com/Polidea/RxAndroidBle/issues/45)
* Added RxBleClient.getBondedDevices() method, thanks fracturedpsyche! (https://github.com/Polidea/RxAndroidBle/pull/46)

Version 1.0.1

* Fixed scan operation concurrency issue, thanks artem-zinnatullin! (https://github.com/Polidea/RxAndroidBle/issues/5)
* Fixed location permission requirement check (Android >=6.0)

Version 1.0.0

* Changed RxBleClient factory method name.
* After this version the public API will be maintained to avoid conflicts.

Version 0.0.4

* Removed duplicated API for connection state from RxBleConnection
* Renamed API for connection state observation in RxBleDevice
* Renamed API for notification setup, not it is RxBleConnection#setupNotification(UUID)
* Added convenience method to check current connection state
* Added ability to filter scan results with one service more easily
* Reject establishConnection calls if connection is already established
* Added adapter for sharing connections

Version 0.0.3

* Added location permission for APIs >=23
* Check if location permission is granted and location services are enabled on Android 6.0
* Fixed error callback notifying about disconnects

Version 0.0.2

* Bugfixes
* Changed API for instantiation of the client
* Added caches in sensitive places

Version 0.0.1

Initial release
* Support for main bluetooth operations (discovery, connection, read, write, notifications)