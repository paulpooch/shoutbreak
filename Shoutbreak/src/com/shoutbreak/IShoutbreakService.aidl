package com.shoutbreak;

import com.shoutbreak.IShoutbreakServiceCallback;

interface IShoutbreakService {

    /**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    void registerCallback(IShoutbreakServiceCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IShoutbreakServiceCallback cb);
    
    void shout(String shoutText);
	
	void vote(String shoutID, int vote);
	
	void deleteShout(String shoutID);
}
