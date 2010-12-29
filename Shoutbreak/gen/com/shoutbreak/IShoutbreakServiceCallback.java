/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\work\\shoutbreak_client\\001\\Shoutbreak\\src\\com\\shoutbreak\\IShoutbreakServiceCallback.aidl
 */
package com.shoutbreak;
/**
 * Example of a callback interface used by IRemoteService to send
 * synchronous notifications back to its clients.  Note that this is a
 * one-way interface so the server does not block waiting for the client.
 */
public interface IShoutbreakServiceCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.shoutbreak.IShoutbreakServiceCallback
{
private static final java.lang.String DESCRIPTOR = "com.shoutbreak.IShoutbreakServiceCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.shoutbreak.IShoutbreakServiceCallback interface,
 * generating a proxy if needed.
 */
public static com.shoutbreak.IShoutbreakServiceCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.shoutbreak.IShoutbreakServiceCallback))) {
return ((com.shoutbreak.IShoutbreakServiceCallback)iin);
}
return new com.shoutbreak.IShoutbreakServiceCallback.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_serviceEventComplete:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.serviceEventComplete(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.shoutbreak.IShoutbreakServiceCallback
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void serviceEventComplete(int eventCode) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(eventCode);
mRemote.transact(Stub.TRANSACTION_serviceEventComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_serviceEventComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void serviceEventComplete(int eventCode) throws android.os.RemoteException;
}
