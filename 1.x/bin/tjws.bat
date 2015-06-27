cd ..
set JDK_HOME=
set SERVLET_API=E:\projects\servlet\javax.servlet.jar
set JSP_PROV=lib\jasper.jar
set JSR356API=lib\javax.websocket-client-api.jar;lib\javax.websocket-server-api.jar
java -cp "%SERVLET_API%;lib\war.jar;lib\webserver.jar;lib\jsp.jar;lib\wskt.jar;lib\class-scanner.jar;%JSR356API%;%JSP_PROV%;%JDK_HOME%\lib\tools.jar" -Dtjws.webappdir=webapps -Dtjws.wardeploy.warname-as-context=yes Acme.Serve.Main -a aliases.properties -p 80 -l -c cgi-bin -acceptorImpl Acme.Serve.SelectorAcceptor
