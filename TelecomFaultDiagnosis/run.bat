@echo off
REM Compile and run the demo, then open the dashboard (Windows)
if not exist out mkdir out
javac -d out java\*.java || exit /b 1
java -cp out Main --demo
start dashboard.html
