package rogatkin.mobile.web;

interface RCServ {

  String start();
  
  void stop();
  
  int getStatus();
  
  void logging(boolean on);
  
  List<String> getApps();
  
  List<String> rescanApps();
  
  String deployApp(String url);
  
  String getAppInfo(String name);
  
  List<String> stopApp(String name);
  
  List<String> redeployApp(String name);
  
  void removeApp(String name);
  
}