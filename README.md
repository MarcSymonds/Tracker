# Tracker
The primary purpose of this application is for requesting the location of a tracker device and showing the location on a map. This has been (initially) written for the TK103A/B type tracker device.

The TK103A/B is a GPS/GSM tracking device that uses SMS messaging to send commands to the device, and receive responses back. For example, you can send it an SMS message requesting it's current location, and it will respond with an SMS message containing its current GPS, or last known GPS, location. The device can also send details of the current tower it is connected to, but there isn't a method to lookup the actual location of the tower.

To actually see the location on a map, the response does also contain a link to Google Maps with the coordinates defined and you can click on the link to see the location. But if you want to continuously track the device you have to keep sending the request, waiting for the response and opening the map manually for each request.

The purpose of this application is to allow you to send the request to the device, and when the response is received it will automatically indicate the location on the map.

![](docs/Tracker-Map-sm.png?raw=true "Tracker Map Screen") 
![](docs/Tracked-Item-Details-sm.png?raw=true "Tracked Item Details")
![](docs/Tracker-Map-CtxMenu-sm.png?raw=true "Tracked Item Context Menu")

Pressing one of the tracker buttons above the map will center the map on the last known location of that device. The first button is your current location.

Long pressing a tracker button will send the location request command to the device. When the response is received, the position of the marker for that tracker will be updated with the new location.

Double tapping a tracker button will bring up a menu of options to manage/control the device.

