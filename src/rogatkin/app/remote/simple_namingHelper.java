package rogatkin.app.remote;


/**
* rogatkin/app/remote/simple_namingHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from idl/remotecontext.idl
* Wednesday, February 21, 2018 12:25:09 PM PST
*/

abstract public class simple_namingHelper
{
  private static String  _id = "IDL:rogatkin.app.remote/app/simple_naming:1.0";

  public static void insert (org.omg.CORBA.Any a, rogatkin.app.remote.simple_naming that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static rogatkin.app.remote.simple_naming extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (rogatkin.app.remote.simple_namingHelper.id (), "simple_naming");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static rogatkin.app.remote.simple_naming read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_simple_namingStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, rogatkin.app.remote.simple_naming value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static rogatkin.app.remote.simple_naming narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof rogatkin.app.remote.simple_naming)
      return (rogatkin.app.remote.simple_naming)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      rogatkin.app.remote._simple_namingStub stub = new rogatkin.app.remote._simple_namingStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static rogatkin.app.remote.simple_naming unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof rogatkin.app.remote.simple_naming)
      return (rogatkin.app.remote.simple_naming)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      rogatkin.app.remote._simple_namingStub stub = new rogatkin.app.remote._simple_namingStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}