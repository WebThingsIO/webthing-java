#!/bin/bash -e

# clone the webthing-tester
git clone https://github.com/WebThingsIO/webthing-tester
pip3 install --user -r webthing-tester/requirements.txt

# build the jar
mvn clean compile assembly:single
jar=$(find target -type f -name 'webthing-*-jar-with-dependencies.jar')

# build and test the single-thing example
java -cp "${jar}" io.webthings.webthing.example.SingleThing &
EXAMPLE_PID=$!
sleep 15
./webthing-tester/test-client.py
kill -15 $EXAMPLE_PID
wait $EXAMPLE_PID || true

# build and test the multiple-things example
java -cp "${jar}" io.webthings.webthing.example.MultipleThings &
EXAMPLE_PID=$!
sleep 15
./webthing-tester/test-client.py --path-prefix "/0"
kill -15 $EXAMPLE_PID
wait $EXAMPLE_PID || true
