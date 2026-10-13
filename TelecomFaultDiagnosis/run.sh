#!/bin/sh
# Compile and run the demo (Linux / macOS)
mkdir -p out
javac -d out java/*.java || exit 1
java -cp out Main --demo
echo "Now open dashboard.html in your browser."
