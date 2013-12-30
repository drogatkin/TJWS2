# TJWS startup script with Jasper JSP engine
cd ..
# copy jasper.jar and commons-*.jar to lib, or provide alternative location in CP
# remove tools.jar from CP when JRE or JSP not used.
JDK_HOME=
# use Open JDK to launch Java without path, note that Open JDK may have no tools.jar
java -cp ./lib/servlet.jar:./lib/war.jar:./lib/webserver.jar:./lib/jsp.jar:lib/jasper.jar:$JDK_HOME/lib/tools.jar -Dtjws.webappdir=./webapps Acme.Serve.Main -a ./aliases.properties -p 8080 -l -c cgi-bin
