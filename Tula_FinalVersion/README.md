## TULA

Welcome to our mobile application!

After the user logs in or creates an account, the user is navigated to the home page. From there, the user has the option to check their heart rate, the pressure that is being applied to their leg by each of the five compression bands, adjust the circumference of the bands, or log out.

# Heart rate
We are sending heart rate readings from a smartwatch to a MQTT broker. Heart rate is read and sent every minute. The application subscribes to the heart rate topic and records heart rate readings from the past 10 minutes. If there is a heart rate increase by more than 30 beats per minute within the 10 minute range, then a notification is sent to the user.

# Pressure
We are measuring pressure by using a Raspberry Pi Zero and stretch sensors that are attached to the compression stocking. The pressure readings published to a topic and sent to AWS IoT Core. The mobile application receives these pressure readings, calibrates the raw data to mm Hg, and displays them to the user.