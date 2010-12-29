package com.shoutbreak.service;

import com.shoutbreak.Vars;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Location;
import android.os.Bundle;

public class LocationTracker {

	private Context _context;
	private LocationManager _locationManager;
	private Location _location;

	public LocationTracker(Context context) {

		_context = context;
		_locationManager = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
		initializeLocation();
		
	}
	
	public void initializeLocation() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

		String provider = _locationManager.getBestProvider(criteria, true);
		_locationManager.requestLocationUpdates(provider, Vars.GPS_MIN_UPDATE_MILLISECS, Vars.GPS_MIN_UPDATE_METERS, _locationListener);

		_location = _locationManager.getLastKnownLocation(provider);
	}

	public Location getLocation() {
		return _location;
	}
	
	private LocationListener _locationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			_location = location;
		}

		public void onProviderDisabled(String provider) {

		}

		public void onProviderEnabled(String provider) {

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

	};
	
	public double getLatitude() {
		return _location.getLatitude();
	}
	
	public double getLongitude() {
		return _location.getLongitude();
	}
	
	public CellDensity getCurrentCell() {
		CellDensity cellDensity = new CellDensity();
		double lat;
		double lng;
		lat = getLatitude() + 90;
		lng = getLongitude() + 180;
		lat = lat * 60 * 6;
		lng = lng * 60 * 6;
		cellDensity.cellY = (int) Math.floor(lat);
		cellDensity.cellX = (int) Math.floor(lng);
								
		// GRID WRAPPING
		// X must be between 0 & 129,599
		if (cellDensity.cellX == Vars.DENSITY_GRID_X_GRANULARITY) {
			cellDensity.cellX = 0;
		}
		// Y must be between 0 & 64,799
		if (cellDensity.cellY == Vars.DENSITY_GRID_Y_GRANULARITY) {
			cellDensity.cellY = 0;
		}		
		
		return cellDensity;
	}
	
}
