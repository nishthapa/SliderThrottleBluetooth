#include<SoftwareSerial.h>
#include<Servo.h>

SoftwareSerial bluetooth(9,10);
Servo esc;

char RAW_DATA[4];
//char RAW_DATA;
String THROTTLE_STR;
int i=0,THROTTLE_INT;

void setup()
{
  Serial.begin(9600);
  bluetooth.begin(9600);
  esc.attach(3);
  // put your setup code here, to run once:

}

void loop()
{
  THROTTLE_STR = "@@@@@@";
  if(bluetooth.available())
  {
    for(i=0;i<=3;i++)
    {
      RAW_DATA[i] = bluetooth.read();
      //RAW_DATA = (char)bluetooth.read();
      delay(1);
    }
  }
  THROTTLE_STR = (String)RAW_DATA;
  //THROTTLE_STR = THROTTLE_STR.substring(0,5);
  //STAR = THROTTLE_STR.indexOf("*");
  //EXCLA = THROTTLE_STR.indexOf("!");
  //RESULTANT_THROTTLE = THROTTLE_STR.substring((STAR + 1),(EXCLA));
  //THROTTLE_INT = RESULTANT_THROTTLE.toInt();
  //THROTTLE_STR = (String)bluetooth.read();
  //}
  //else
  //{
    //THROTTLE_INT = 0;
  //}
  THROTTLE_INT = THROTTLE_STR.toInt();
  //THROTTLE_INT = map(THROTTLE_INT,0,9,1000,2000);
  //Serial.println(THROTTLE_INT);
  Serial.println(THROTTLE_INT);
  esc.writeMicroseconds(THROTTLE_INT);
  //delay(100);
  // put your main code here, to run repeatedly:

}
