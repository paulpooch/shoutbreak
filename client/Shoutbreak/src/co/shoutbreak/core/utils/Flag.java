package co.shoutbreak.core.utils;


public class Flag {
	// use flag.get() to get value
	// use flag.set(boolean value) to set value
	// flag will print out an error if get() is called before set()
	
	private static final String TAG = "Flag";
	
	private boolean _isInitialized;
	private int _value;
	private String _name = "";
	
	public Flag() {
		_isInitialized = false;
	}
	
	public Flag(String name) {
		_isInitialized = false;
		_name = name;
	}
	
	public Flag(boolean value) {
		set(value);
	}
	
	public void set(boolean value) {
		if (value)
			_value = 1;
		else
			_value = -1;
		_isInitialized = true;
	}
	
	public boolean get() {
		if (_isInitialized) {
			return _value == 1;
		} else {
			if (_name != "") {
				SBLog.e(TAG, "Flag '" + _name + "' never initialized, must call set()!");	
			} else {
				SBLog.e(TAG, "Flag never initialized, must call set()!");
			}
			return false;
		}
	}
}
