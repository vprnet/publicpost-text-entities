text-entities
=============

Stanford NER (Named Entity Recognizer) web application.

To build and run:

(1) Build the web application:
    mvn install

(2) Launch the application:
    java -server -Xms1024m -Xmx1024m -jar target/dependency/jetty-runner.jar target/*.war