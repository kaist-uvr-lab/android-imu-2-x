# android-imu-2-x
This project includes android app which stream IMU data(Accelerometer, Gyroscope, Rotation Vector) through UDP socket and example code of receving IMU datastream in various platform.

## Instructions(How to use)
### AndroidIMU
Project is written with Android Studio, so it will be easier to import this folder as Project on Android Studio. Theere are three different modules inside Project. Three are seperate module for each device type: phone, watch, glass. Three module share common core code which is in 'shared_lib'.

There are several options which can set on CONSTANTS class in shared_lib. Can check details in the code with descriptions. 
When you run this app, it shows current status on start screen. 

First line is target IP and Port. \\
Second line is current connected WiFi SSID. \\
Third line is MAC address of current device. \\
Fourth line is current device's IP. \\

Press CONNECT button to start stream.(You can change target IP and Port with long click before start sending.) Then long click CONNECT button to stop stream. \\
Press WRITE FILE button to wrtie sensor stream on phone.


## Modules
### AndroidIMU

### receiver_python

### receiver_unity
