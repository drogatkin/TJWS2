# TJWS startup script with Jasper JSP engine
cd .
# copy jasper.jar and commons-*.jar to lib, or provide alternative location in CP
# remove tools.jar from CP when JRE or JSP not used.
JDK_HOME=
JSR356API=/home/dmitriy/projects/TJWS2/1.x/.temp_repo/javax.websocket-api-1.1.jar
SERVLET=/home/dmitriy/projects/TJWS2/1.x/.temp_repo/javax.servlet-api-3.1.0.jar

corba_path=/home/dmitriy/projects/jacorb-3.9/lib
corba=$corba_path/antlr-2.7.2.jar:$corba_path/idl.jar:$corba_path/jacorb-3.9.jar:$corba_path/jacorb-omgapi-3.9.jar:$corba_path/jacorb-services-3.9.jar:$corba_path/picocontainer-1.2.jar:$corba_path/slf4j-api-1.7.14.jar:$corba_path/slf4j-jdk14-1.7.14.jar:$corba_path/wrapper-3.1.0.jar:./lib/stub.jar
# use Open JDK to launch Java without path, note that Open JDK may have no tools.jar, same true for JDK > 10

java -cp $SERVLET:./lib/war.jar:./lib/webserver.jar:./lib/class-scanner.jar:./lib/wskt.jar:./lib/app.jar:lib/jasper.jar:$JSR356API:$corba -Dtjws.webappdir=./webapps rogatkin.app.Main -a ./aliases.properties -p 8000 -l -c cgi-bin -acceptorImpl Acme.Serve.SelectorAcceptor
