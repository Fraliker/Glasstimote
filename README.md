Glasstimote
===========

Demo application for Google glass using Estimote SDK 

ok glass, scan beacons

The app starts a service to carry out various tasks:
- Starts Estimote beacons discovery
- If one of a specific set of beacons are discovered nearby, a live card is published with info about the location
- Tap on the live card to see more details about the nearby beacon
- Stop option on live card to stop the service
- If service is not stopped it will continue scanning and display info on the live card as and when beacons are in or out of range.
- Once a beacon is in range the scanning for other beacons is stopped until the nearby beacon becomes out of range.
