@echo off
echo Installing Fuata CLI...

rem Copy the fat JAR and create a bash script in a folder that can be added to environment PATH
copy build\libs\fuata-1.0-SNAPSHOT.jar "C:\Program Files\Fuata\fuata-1.0.jar"
echo @echo off > "C:\Program Files\Fuata\fuata.bat"
echo java -jar "C:\Program Files\Fuata\fuata.jar" %%* >> "C:\Program Files\Fuata\fuata.bat"

echo Installation complete. Add "C:\Program Files\Path" to your PATH to use the 'fuata' command