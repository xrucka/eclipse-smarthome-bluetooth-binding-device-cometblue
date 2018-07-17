# eclipse-smarthome-bluetooth-binding-device-cometblue
Plugin for OpenHab, implementing support for CometBlue devices made by Eurotronic.
See fork of original opensource project [cometblue](https://github.com/xrucka/cometblue).

## Prerequisites
1. bluetooth-gatt-parser (build locally with latest pullrequests and set version to 1.9.3-SNAPSHOT)
2. eclipse-smarthome-bluetooth-binding (with above mentined gatt parser and latest pullrequests)
3. maven

## Setup
In OpenHab Karaf console, run
`
bundle:install file:///path/to/jarfile
`
(replace /path/to/jarfile with absolute path to your build)

---
## Contribution
Feel free to contact me, however I suggest to perhaps use original issue:
https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/14

### Building

Then build the project with maven:
```bash
mvn clean install
```
