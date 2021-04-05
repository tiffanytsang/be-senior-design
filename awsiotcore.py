import spidev
import time
from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient

#Define Variables
delay = 0.5
pad_channel_1 = 0
pad_channel_2 = 1
pad_channel_3 = 2
pad_channel_4 = 3
pad_channel_5 = 4

#Create SPI
spi = spidev.SpiDev()
spi.open(0, 0)
spi.max_speed_hz=1000000

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

try:
    # prev_pad_value_1=0
    while True:

        pad_value_1 = readadc(pad_channel_1)
        pad_value_2 = readadc(pad_channel_2)
        pad_value_3 = readadc(pad_channel_3)
        pad_value_4 = readadc(pad_channel_4)
        pad_value_5 = readadc(pad_channel_5)
        # if (abs(pad_value_1-prev_pad_value) > 10):
        print("---------------------------------------")
        print("Pressure Pad Value 1: %d" % pad_value_1)
        print("Pressure Pad Value 2: %d" % pad_value_2)
        print("Pressure Pad Value 3: %d" % pad_value_3)
        print("Pressure Pad Value 4: %d" % pad_value_4)
        print("Pressure Pad Value 5: %d" % pad_value_5)
        myMQTTClient.publish(
            topic="home/helloworld",
            QoS=1,
            payload=str(pad_value_1)+","+str(pad_value_2)+","+str(pad_value_3)+","+str(pad_value_4)+","+str(pad_value_5)
        )

        #update previous input
        # prev_pad_value = pad_value_1
        #slight pause
        time.sleep(delay)

except KeyboardInterrupt:
    pass