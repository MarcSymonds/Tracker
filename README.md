# Tracker
Application to send requests to and receive responses from a tracking device (TK103A/B) and show the location on a map.

The TK103A/B is a GPS/GSM tracking device that uses SMS messaging to control and receive location data from. You can send it an SMS message requesting it's current location, and it will respond with an SMS message containing it current location.

This can be done using your phones normal SMS capabilities.

To actually see the location on a map, the response does also contain a link to Google Maps with the coordinates defined and you can click on the link to see the location. But if you want to continuously track the device you have to keep sending the request, waiting for the response and opening the map manually for each request.

The purpose of this application is to allow you to send the request to the device, and when the response is received it will automatically indicate on the map the location.

![](docs/Tracker-Map-sm.png?raw=true "Tracker Map Screen") 
![](docs/Tracked-Item-Details-sm.png?raw=true "Tracked Item Details")
