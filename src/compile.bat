cd src
javac -Xlint:-options -Xlint:unchecked -O -source 1.8 -target 1.8 -cp . *.java TUIO\*.java com\illposed\osc\*.java
jar cfm ..\TuioDemo.jar manifest.inc *.class TUIO\*.class com\illposed\osc\*.class com\illposed\osc\utility\*.class
jar cf ..\libTUIO.jar TUIO\*.class com\illposed\osc\*.class com\illposed\osc\utility\*.class
del *.class TUIO\*.class com\illposed\osc\*.class com\illposed\osc\utility\*.class
cd ..
