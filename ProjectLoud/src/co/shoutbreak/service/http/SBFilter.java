package co.shoutbreak.service.http;

import org.json.JSONObject;

public class SBFilter {
	public static JSONObject filterJSON(JSONObject json) {
		// TODO: treat all data from web as dangerous
		//check for every possible field
		//validate appropriately
		//i.e. uid, pw, etc
		return json;
	}
}
