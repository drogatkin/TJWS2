package rogatkin.app.remote;


/**
* rogatkin/app/remote/simple_namingOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from idl/remotecontext.idl
* Wednesday, February 21, 2018 12:25:09 PM PST
*/

public interface simple_namingOperations 
{
  void bind1 (String name, org.omg.CORBA.Object o) throws rogatkin.app.remote.naming_exception;
  void unbind1 (String name) throws rogatkin.app.remote.naming_exception;
  org.omg.CORBA.Object lookup1 (String name) throws rogatkin.app.remote.naming_exception;
  void list1 (String filter, rogatkin.app.remote.bind_listHolder bindings) throws rogatkin.app.remote.naming_exception;
} // interface simple_namingOperations
