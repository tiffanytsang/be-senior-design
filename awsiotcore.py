#!/usr/bin/python

import spidev
import time
from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
# import RPi.GPIO as GPIO

#Define Variables
delay = 0.5
pad_channel = 0

#Create SPI
spi = spidev.SpiDev()
spi.open(0, 0)
spi.max_speed_hz=1000000

def helloworld(self, params, packet):
    print('Received Message from AWS IoT Core')
    print("Topic: " + packet.topic)
    print("Payload: ", (packet.payload))

def readadc(adcnum):
    # read SPI data from the MCP3008, 8 channels in total
    if adcnum > 7 or adcnum < 0:
        return -1
    r = spi.xfer2([1, 8 + adcnum << 4, 0])
    data = ((r[1] & 3) << 8) + r[2]
    return data

myMQTTClient = AWSIoTMQTTClient("BE496ClientID") #random key, if another connection using the same key is opened the previous one is auto closed by AWS IOT
myMQTTClient.configureEndpoint("a2tud3mf4dwlbh-ats.iot.us-east-1.amazonaws.com", 8883)

myMQTTClient.configureCredentials("/home/pi/AWSIoT/root-ca.pem", "/home/pi/AWSIoT/private.pem.key", "/home/pi/AWSIoT/certificate.pem.crt")

myMQTTClient.configureOfflinePublishQueueing(-1) # Infinite offline Publish queueing
myMQTTClient.configureDrainingFrequency(2) # Draining: 2 Hz
myMQTTClient.configureConnectDisconnectTimeout(10) # 10 sec
myMQTTClient.configureMQTTOperationTimeout(5) # 5 sec
print ('Initiating Realtime Data Transfer From Raspberry Pi...')
myMQTTClient.connect()
# myMQTTClient.subscribe("home/helloworld", 1, helloworld)
# while True:
#     time.sleep(.5)

# GPIO.setmode(GPIO.BCM)
# GPIO.setup(4,GPIO.IN)

try:
    prev_pad_value=0
    while True:

        pad_value = readadc(pad_channel)
        # print("---------------------------------------")
        # print("Pressure Pad Value: %d" % pad_value)
        # time.sleep(delay)


        #if the last reading was low and this one high, alert us
        if (abs(pad_value-prev_pad_value) > 10):
            # pad_value = readadc(pad_channel)
            print("---------------------------------------")
            print("Pressure Pad Value: %d" % pad_value)
            # time.sleep(delay)
            # print("Under Pressure", prev_input, input)
            # print("Publishing Message from RPI")
            myMQTTClient.publish(
                topic="home/helloworld",
                QoS=1,
                payload="{'Message': 'Message By RPI - pressure changed loooooool !" + str(pad_value)+ " '}"
            )
        #update previous input
        prev_pad_value = pad_value
        #slight pause
        time.sleep(delay)


except KeyboardInterrupt:
    pass




