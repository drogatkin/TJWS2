package rogatkin.app.remote;


/**
* rogatkin/app/remote/bind_listHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from idl/remotecontext.idl
* Wednesday, February 21, 2018 12:25:09 PM PST
*/

abstract public class bind_listHelper
{
  private static String  _id = "IDL:rogatkin.app.remote/app/bind_list:1.0";

  public static void insert (org.omg.CORBA.Any a, rogatkin.app.remote.bind[] that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static rogatkin.app.remote.bind[] extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = rogatkin.app.remote.bindHelper.type ();
      __typeCode = org.omg.CORBA.ORB.init ().create_sequence_tc (0, __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_alias_tc (rogatkin.app.remote.bind_listHelper.id (), "bind_list", __typeCode);
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static rogatkin.app.remote.bind[] read (org.omg.CORBA.portable.InputStream istream)
  {
    rogatkin.app.remote.bind value[] = null;
    int _len0 = istream.read_long ();
    value = new rogatkin.app.remote.bind[_len0];
    for (int _o1 = 0;_o1 < value.length; ++_o1)
      value[_o1] = rogatkin.app.remote.bindHelper.read (istream);
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, rogatkin.app.remote.bind[] value)
  {
    ostream.write_long (value.length);
    for (int _i0 = 0;_i0 < value.length; ++_i0)
      rogatkin.app.remote.bindHelper.write (ostream, value[_i0]);
  }

}