# TJWS startup script with Jasper JSP engine
cd ..
# copy jasper.jar and commons-*.jar to lib, or provide alternative location in CP
# remove tools.jar from CP when JRE or JSP not used.
JDK_HOME=
JSR356API=lib/javax.websocket-client-api.jar:lib/javax.websocket-server-api.jar

corba_path=/usr/local/share/lib/jacorb-3.9/lib
corba=$corba_path/antlr-2.7.2.jar:$corba_path/idl.jar:$corba_path/jacorb-3.9.jar:$corba_path/jacorb-omgapi-3.9.jar:$corba_path/jacorb-services-3.9.jar:$corba_path/picocontainer-1.2.jar:$corba_path/slf4j-api-1.7.14.jar:$corba_path/slf4j-jdk14-1.7.14.jar:$corba_path/wrapper-3.1.0.jar:./lib/stub.jar
# use Open JDK to launch Java without path, note that Open JDK may have no tools.jar, same true for JDK > 10

java -cp ./lib/servlet.jar:./lib/war.jar:./lib/webserver.jar:./lib/class-scanner.jar:./lib/wskt.jar:./lib/jsp.jar:lib/jasper.jar:$JSR356API:$JDK_HOME/lib/tools.jar:$corba -Dtjws.webappdir=./webapps Acme.Serve.Main -a ./aliases.properties -p 8080 -l -c cgi-bin -acceptorImpl Acme.Serve.SelectorAcceptor
