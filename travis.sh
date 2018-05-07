#!/bin/bash -e

# clone the webthing-tester
git clone https://github.com/mozilla-iot/webthing-tester
pip3 install --user -r webthing-tester/requirements.txt

# build the jar
mvn clean compile assembly:single

# build and test the single-thing example
java -cp target/webthing-0.5.2-jar-with-dependencies.jar \
    org.mozilla.iot.webthing.example.SingleThing &
EXAMPLE_PID=$!
sleep 15
./webthing-tester/test-client.py --debug
kill -15 $EXAMPLE_PID

# build and test the multiple-things example
java -cp target/webthing-0.5.2-jar-with-dependencies.jar \
    org.mozilla.iot.webthing.example.MultipleThings &
EXAMPLE_PID=$!
sleep 15
./webthing-tester/test-client.py --path-prefix "/0"
kill -15 $EXAMPLE_PID
