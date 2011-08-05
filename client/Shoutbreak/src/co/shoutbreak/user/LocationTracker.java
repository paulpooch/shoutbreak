package co.shoutbreak.user;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.utils.SBLog;

import com.google.android.maps.GeoPoint;

public class LocationTracker implements LocationListener, Colleague {
	// http://stackoverflow.com/questions/1389811/gps-not-update-location-after-close-and-reopen-app-on-android
		
	private static final String TAG = "LocationTracker";
	
	private Mediator _m;
	private LocationManager _locationManager;
	private Location _location;
	private String _provider;
	private Criteria _criteria;	
	
	public LocationTracker(Mediator mediator) {
		_m = mediator;
		_locationManager = (LocationManager)_m.getSystemService(Context.LOCATION_SERVICE);
		//_location = _locationManager.getLastKnownLocation(_provider);
		_criteria = new Criteria();
		_criteria.setAccuracy(Criteria.ACCURACY_FINE);
		_criteria.setAltitudeRequired(false);
		_criteria.setBearingRequired(false);
		_criteria.setSpeedRequired(false);
		_criteria.setCostAllowed(true);
		_criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		//String allowedProviders = Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	}
	
	@Override
	public void unsetMediator() {
		_m = null;
	}
	
	public void toggleListeningToLocation(boolean turnOn) {
		if (turnOn) {
			_provider = _locationManager.getBestProvider(_criteria, true);
			_locationManager.requestLocationUpdates(_provider, C.CONFIG_GPS_MIN_UPDATE_MILLISECS, C.CONFIG_GPS_MIN_UPDATE_METERS, LocationTracker.this);
			_location = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			//_service.getStateManager().setIsLocationTrackerUsingGPS(true);
		} else {
			_locationManager.removeUpdates(LocationTracker.this);
			//_service.getStateManager().setIsLocationTrackerUsingGPS(false);
		}
	}
	
	public boolean isLocationEnabled() {
		_provider = _locationManager.getBestProvider(_criteria, true);
		_location = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		// if _location is null, it means User has location disabled in settings
		if (_location != null && _provider != null && _locationManager.isProviderEnabled(_provider)) {
			_locationManager.requestLocationUpdates(_provider, C.CONFIG_GPS_MIN_UPDATE_MILLISECS, C.CONFIG_GPS_MIN_UPDATE_METERS, LocationTracker.this);
			return true;
		}
		_locationManager.removeUpdates(LocationTracker.this);
		return false;
	}
	
	public Location getLocation() {
		return _location;
	}
	
	public synchronized double getLatitude() {
		// We only care about 5 decimal places = 1.11m accuracy
		return ((double)Math.round(_location.getLatitude() * 100000)) / 100000;
	}
	
	public synchronized double getLongitude() {
		// We only care about 5 decimal places = 1.11m accuracy
		return ((double)Math.round(_location.getLongitude() * 100000)) / 100000;
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
		if (cellDensity.cellX == C.CONFIG_DENSITY_GRID_X_GRANULARITY) {
			cellDensity.cellX = 0;
		}
		// Y must be between 0 & 64,799
		if (cellDensity.cellY == C.CONFIG_DENSITY_GRID_Y_GRANULARITY) {
			cellDensity.cellY = 0;
		}		
		
		return cellDensity;
	}

	 // http://developer.android.com/guide/topics/location/obtaining-user-location.html
	 /** Determines whether one Location reading is better than the current Location fix
	   * @param location  The new Location that you want to evaluate
	   * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	   */
	 protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		 final int TWO_MINUTES = 1000 * 60 * 2;
		 if (currentBestLocation == null) {
	         // A new location is always better than no location
	         return true;
	     }

	     // Check whether the new location fix is newer or older
	     long timeDelta = location.getTime() - currentBestLocation.getTime();
	     boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	     boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	     boolean isNewer = timeDelta > 0;

	     // If it's been more than two minutes since the current location, use the new location
	     // because the user has likely moved
	     if (isSignificantlyNewer) {
	         return true;
	     // If the new location is more than two minutes older, it must be worse
	     } else if (isSignificantlyOlder) {
	         return false;
	     }

	     // Check whether the new location fix is more or less accurate
	     int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	     boolean isLessAccurate = accuracyDelta > 0;
	     boolean isMoreAccurate = accuracyDelta < 0;
	     boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	     // Check if the old and new location are from the same provider
	     boolean isFromSameProvider = isSameProvider(location.getProvider(),
	             currentBestLocation.getProvider());

	     // Determine location quality using a combination of timeliness and accuracy
	     if (isMoreAccurate) {
	         return true;
	     } else if (isNewer && !isLessAccurate) {
	         return true;
	     } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	         return true;
	     }
	     return false;
	 }

	 /** Checks whether two providers are the same */
	 private boolean isSameProvider(String provider1, String provider2) {
	     if (provider1 == null) {
	       return provider2 == null;
	     }
	     return provider1.equals(provider2);
	 }
	 
	 public static GeoPoint LocationToGeoPoint(Location location) {
		 return new GeoPoint((int)(location.getLatitude() * 1e6), (int)(location.getLongitude() * 1e6));
	 }
	 
	public LocationManager getLocationManager() {
		return _locationManager;
	}
        
	@Override
	public void onLocationChanged(Location location) {
		SBLog.i(TAG, "onLocationChanged()");
		if (isBetterLocation(location, _location)) {
			_location = location;
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		SBLog.i(TAG, "onProviderEnabled()");
	}

	@Override
	public void onProviderDisabled(String provider) {
		SBLog.i(TAG, "onProviderDisabled()");
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		//_provider = _locationManager.getBestProvider(_criteria, true);
		//_locationManager.requestLocationUpdates(_provider, C.CONFIG_GPS_MIN_UPDATE_MILLISECS, C.CONFIG_GPS_MIN_UPDATE_METERS, LocationTracker.this);
		if (status == LocationProvider.AVAILABLE) {
			_m.onLocationEnabled();
		} else if (status == LocationProvider.OUT_OF_SERVICE) {
			_m.onLocationDisabled();
		}
	}
}
