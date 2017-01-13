#include<SoftwareSerial.h>
#include<Servo.h>

SoftwareSerial bluetooth(9,10);               //creating a bluetooth object for communication
Servo esc;                                    //creating Servo object for Electronic Speed Controller

char RAW_DATA[4];                             //to read raw data (character from bytes) from the bluetooth stream
String THROTTLE_STR;                          //to represent throttle in string after reading from bluetooth stream
int i=0,THROTTLE_INT;

void setup()
{
  Serial.begin(9600);
  bluetooth.begin(9600);                      //begin bluetooth communication
  esc.attach(3);                              //declare that the Electronic Speed Controller is attached to Arduino pin 3
}

void loop()
{
  THROTTLE_STR = "@@@@@@";                    //for filtering noise
  if(bluetooth.available())
  {
    for(i=0;i<=3;i++)
    {
      RAW_DATA[i] = bluetooth.read();         //read data from bluetooth stream
      delay(1);                               //give some time for finish reading
    }
  }
  THROTTLE_STR = (String)RAW_DATA;            //convert read data to string
  THROTTLE_INT = THROTTLE_STR.toInt();        //convert string to int for implementing Servo write
  Serial.println(THROTTLE_INT);               //print for debugging on serial monitor
  esc.writeMicroseconds(THROTTLE_INT);        //finally send throttle signal to the Electronic Speed Controller
}
