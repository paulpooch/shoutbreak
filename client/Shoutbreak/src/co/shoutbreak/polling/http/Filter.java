package co.shoutbreak.polling.http;

import org.json.JSONObject;

public class Filter {
	
	public static JSONObject filterJSON(JSONObject json) {
		// TODO: treat all data from web as dangerous
		//check for every possible field
		//validate appropriately
		//i.e. uid, pw, etc
		return json;
	}
	
}