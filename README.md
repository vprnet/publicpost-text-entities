text-entities
=============

Stanford NER (Named Entity Recognizer) web application.

Deployed on Heroku at: http://text-entities.herokuapp.com/classify

To run locally:

    mkdir text-entities
    git clone https://github.com/NearbyFYI/text-entities.git text-entities
    heroku git:remote -a text-entities
    mvn install
    java -server -Xms1024m -Xmx1024m -jar target/dependency/jetty-runner.jar target/*.war
