# TJWS startup script with Jasper JSP engine
cd ..
# copy jasper.jar and commons-*.jar to lib, or provide alternative location in CP
# remove tools.jar from CP when JRE or JSP not used.
JDK_HOME=
JSR356API=lib/javax.websocket-client-api.jar:lib/javax.websocket-server-api.jar
# use Open JDK to launch Java without path, note that Open JDK may have no tools.jar
java -cp ./lib/servlet.jar:./lib/war.jar:./lib/webserver.jar:./lib/class-scanner.jar:./lib/wskt.jar:./lib/jsp.jar:lib/jasper.jar:$JSR356API:$JDK_HOME/lib/tools.jar -Dtjws.webappdir=./webapps Acme.Serve.Main -a ./aliases.properties -p 8080 -l -c cgi-bin -acceptorImpl Acme.Serve.SelectorAcceptor
