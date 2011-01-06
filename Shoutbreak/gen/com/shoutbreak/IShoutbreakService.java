/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\John\\Desktop\\Android\\shoutbreak\\Shoutbreak\\src\\com\\shoutbreak\\IShoutbreakService.aidl
 */
package com.shoutbreak;
public interface IShoutbreakService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.shoutbreak.IShoutbreakService
{
private static final java.lang.String DESCRIPTOR = "com.shoutbreak.IShoutbreakService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.shoutbreak.IShoutbreakService interface,
 * generating a proxy if needed.
 */
public static com.shoutbreak.IShoutbreakService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.shoutbreak.IShoutbreakService))) {
return ((com.shoutbreak.IShoutbreakService)iin);
}
return new com.shoutbreak.IShoutbreakService.Stub.Proxy(obj);
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
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
com.shoutbreak.IShoutbreakServiceCallback _arg0;
_arg0 = com.shoutbreak.IShoutbreakServiceCallback.Stub.asInterface(data.readStrongBinder());
this.registerCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
com.shoutbreak.IShoutbreakServiceCallback _arg0;
_arg0 = com.shoutbreak.IShoutbreakServiceCallback.Stub.asInterface(data.readStrongBinder());
this.unregisterCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_shout:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.shout(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vote:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
this.vote(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_deleteShout:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.deleteShout(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.shoutbreak.IShoutbreakService
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
/**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
public void registerCallback(com.shoutbreak.IShoutbreakServiceCallback cb) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Remove a previously registered callback interface.
     */
public void unregisterCallback(com.shoutbreak.IShoutbreakServiceCallback cb) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void shout(java.lang.String shoutText) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(shoutText);
mRemote.transact(Stub.TRANSACTION_shout, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void vote(java.lang.String shoutID, int vote) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(shoutID);
_data.writeInt(vote);
mRemote.transact(Stub.TRANSACTION_vote, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void deleteShout(java.lang.String shoutID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(shoutID);
mRemote.transact(Stub.TRANSACTION_deleteShout, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_shout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_vote = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_deleteShout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
/**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
public void registerCallback(com.shoutbreak.IShoutbreakServiceCallback cb) throws android.os.RemoteException;
/**
     * Remove a previously registered callback interface.
     */
public void unregisterCallback(com.shoutbreak.IShoutbreakServiceCallback cb) throws android.os.RemoteException;
public void shout(java.lang.String shoutText) throws android.os.RemoteException;
public void vote(java.lang.String shoutID, int vote) throws android.os.RemoteException;
public void deleteShout(java.lang.String shoutID) throws android.os.RemoteException;
}
