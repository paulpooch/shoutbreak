package co.shoutbreak.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.graphics.Path.Direction;
import android.location.Location;

import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.User;
import co.shoutbreak.ui.CustomMapView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

//http://code.google.com/apis/maps/documentation/javascript/maptypes.html#WorldCoordinates
//http://code.google.com/android/add-ons/google-apis/reference/index.html

//If this crashes DroidX, use FixedMyLocationOverlay
//http://stackoverflow.com/questions/753793/how-can-i-use-a-custom-bitmap-for-the-you-are-here-point-in-a-mylocationoverlay
//http://www.gitorious.net/android-maps-api/android-maps-api/blobs/42614538ffda1a6985c398933a85fcd9afc752ee/src/com/google/android/maps/MyLocationOverlay.java
public class UserLocationOverlay extends MyLocationOverlay {

	private static final String TAG = "UserLocationOverlay";

	private Shoutbreak _ui;
	private CustomMapView _map;
	private int _maxShoutreach;
//	private boolean _isShoutreachRadiusSet;
	private long _shoutreachRadiusOfCurrentLocation ;
	private float _maxRadiusMeters;
	private float _maxRadiusPixels;
	private double _maxAreaPixels;
	private boolean _arePixelsCalculated;
	private GeoPoint _lastUserLocationGeoPoint; // the last user coordinates
	private int _resizeAdjustmentPixels;// user circle resize, in pixels
	private boolean _calibrateZoomLevelForRadiusSize; // have we calibrated for the best zoom level yet?
	private float _currentRadiusPixels; // currently displayed radius (baseRadius + resizeAdjustment)
	private int _peopleCount;

	// Artwork.
	private Paint _circleFillPaint;
	private Paint _circleBorderPaint;
	private Bitmap _resizeIcon;
	private Bitmap _resizeIconMax;
	private Bitmap _resizeIconMin;
	private Bitmap _resizeIconGone;
	private Point _resizeIconSize; // icon dimensions in pixels
	private Point _resizeIconLocation; // icon location in pixels
	private Point _userLocationPx; // user location in pixels
	private final double SQRT2 = (double) 1.4142; // square root of 2, for hypotenuse of 45, 45, 90 triangle, for resize icon location
	private int _mapSizeConstraint; // min(map width, map height)
	private int _zoomLevel; // current zoom level of map
	private int _resizeIconState;

	private static final int RESIZE_ICON = 0;
	private static final int RESIZE_ICON_MIN = 1;
	private static final int RESIZE_ICON_MAX = 2;
	private static final int RESIZE_ICON_GONE = 3;

	public UserLocationOverlay(Shoutbreak ui, MapView map) {
		super(ui, map);
		SBLog.constructor(TAG);
		_ui = ui;
		_map = null;
		_maxShoutreach = -1;
		_shoutreachRadiusOfCurrentLocation = -1;
//		_isShoutreachRadiusSet = false;
		_maxRadiusMeters = 0;
		_maxRadiusPixels = C.MIN_RADIUS_PX;
		_maxAreaPixels = C.MIN_RADIUS_PX * C.MIN_RADIUS_PX * Math.PI;
		_arePixelsCalculated = false;
		_lastUserLocationGeoPoint = null;
		_resizeAdjustmentPixels = 0;
		_calibrateZoomLevelForRadiusSize = true;
		_userLocationPx = null;
		_currentRadiusPixels = 0;
		_peopleCount = -1;
		_mapSizeConstraint = -1;
		_zoomLevel = C.DEFAULT_ZOOM_LEVEL;
		_circleFillPaint = new Paint();
		_circleFillPaint.setARGB(50, 108, 10, 171);
		_circleFillPaint.setAntiAlias(true);
		_circleBorderPaint = new Paint();
		_circleBorderPaint.setAntiAlias(true);
		_circleBorderPaint.setARGB(120, 69, 3, 111);
		_circleBorderPaint.setStyle(Style.STROKE);
		_circleBorderPaint.setStrokeWidth(4);
		Bitmap resizeBitmap = BitmapFactory.decodeResource(_ui.getResources(), R.drawable.resize_icon);
		Bitmap resizeBitmapMax = BitmapFactory.decodeResource(_ui.getResources(), R.drawable.resize_icon_max);
		Bitmap resizeBitmapMin = BitmapFactory.decodeResource(_ui.getResources(), R.drawable.resize_icon_min);
		Bitmap resizeBitmapGone = BitmapFactory.decodeResource(_ui.getResources(), R.drawable.resize_icon_gone);
		_resizeIconSize = new Point(resizeBitmap.getWidth(), resizeBitmap.getHeight());
		_resizeIconLocation = new Point();
		_resizeIcon = Bitmap.createBitmap(resizeBitmap, 0, 0, _resizeIconSize.x, _resizeIconSize.y);
		_resizeIconMax = Bitmap.createBitmap(resizeBitmapMax, 0, 0, _resizeIconSize.x, _resizeIconSize.y);
		_resizeIconMin = Bitmap.createBitmap(resizeBitmapMin, 0, 0, _resizeIconSize.x, _resizeIconSize.y);
		_resizeIconGone = Bitmap.createBitmap(resizeBitmapGone, 0, 0, _resizeIconSize.x, _resizeIconSize.y);
		_resizeIconState = RESIZE_ICON_MAX;
	}

	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLocation, long when) {
		// called every time map is redrawn.... don't do anything heavy here
		_lastUserLocationGeoPoint = myLocation;

		if (!_arePixelsCalculated) {
			_maxRadiusPixels = MeterPixelConverter.metersToPixels(_maxRadiusMeters, _map.getZoomLevel());
			_maxAreaPixels = _maxRadiusPixels * _maxRadiusPixels * Math.PI;
			if (getPeopleCount() != _maxShoutreach) {
				// subtract 1 from people count because we want the beginning of the range.
				// e.g. 22 people is from r0 -> r1.  We want r0.
				double peopleRatio = ((double)getPeopleCount() - 1) / (double)_maxShoutreach;
				double newAreaPixels = peopleRatio * _maxAreaPixels;
				double newRadiusPixels = Math.sqrt(newAreaPixels / Math.PI);
				_resizeAdjustmentPixels = (int)(newRadiusPixels - _maxRadiusPixels);
			} else {
				_resizeAdjustmentPixels = 0;
			}
			_arePixelsCalculated = true;
			calculateCurrentRadius();
		}
		
		_userLocationPx = mapView.getProjection().toPixels(_lastUserLocationGeoPoint, null);

		// draw circle
		Path path = new Path();
		path.addCircle(_userLocationPx.x, _userLocationPx.y, _currentRadiusPixels, Direction.CW);
		canvas.drawCircle(_userLocationPx.x, _userLocationPx.y, _currentRadiusPixels, _circleFillPaint);
		canvas.drawPath(path, _circleBorderPaint);

		// draw icon
		double iconXYOffset = _currentRadiusPixels / SQRT2;
		_resizeIconLocation.x = (int) (_userLocationPx.x - iconXYOffset - (_resizeIconSize.x / 2));
		_resizeIconLocation.y = (int) (_userLocationPx.y + iconXYOffset - (_resizeIconSize.y / 2));

		Bitmap currentIcon;
		switch (_resizeIconState) {
		case RESIZE_ICON_MIN:
			currentIcon = _resizeIconMin;
			break;
		case RESIZE_ICON_MAX:
			currentIcon = _resizeIconMax;
			break;
		case RESIZE_ICON_GONE:
			currentIcon = _resizeIconGone;
			break;
		default:
			currentIcon = _resizeIcon;
			break;
		}

		canvas.drawBitmap(currentIcon, _resizeIconLocation.x, _resizeIconLocation.y, null);

		if (_calibrateZoomLevelForRadiusSize) {
			calibrateZoomLevelToShowCircle();
		}
	}
	
	public void handleShoutreachRadiusChange(boolean isShoutreachRadiusSet, long newShoutreachRadius, int newLevel) {
//		_isShoutreachRadiusSet = isShoutreachRadiusSet;
		_shoutreachRadiusOfCurrentLocation = newShoutreachRadius;
		_maxShoutreach = newLevel;
		_maxRadiusMeters = _shoutreachRadiusOfCurrentLocation;
		_peopleCount = _maxShoutreach;
		_arePixelsCalculated = false;
		_resizeAdjustmentPixels = 0;
		_calibrateZoomLevelForRadiusSize = true;
	}

	@Override
	public void onLocationChanged(Location location) {
		_ui.centerMapOnUser(false);
		super.onLocationChanged(location);
	}

	public void calculateCurrentRadius() {
		_currentRadiusPixels = _maxRadiusPixels + _resizeAdjustmentPixels;
		updatePeopleCount();
	}

	protected void updatePeopleCount() {
		// TODO Introduce a tolerance. Prevents us from doing this way too often - let's only care every 50 microdegrees.
		double currentDensityInPixels = _maxShoutreach / _maxAreaPixels;
		double currentAreaInPixels = _currentRadiusPixels * _currentRadiusPixels * Math.PI;
		_peopleCount = (int) Math.min(Math.ceil(currentDensityInPixels * currentAreaInPixels), _maxShoutreach);
		_ui.mapPeopleCountTv.setText(Integer.toString(_peopleCount));
		if (_currentRadiusPixels <= C.MIN_RADIUS_PX - 2) {
			_resizeIconState = RESIZE_ICON_GONE;
		} else if (_currentRadiusPixels < C.MIN_RADIUS_PX + 2 && _currentRadiusPixels > C.MIN_RADIUS_PX - 2 && _peopleCount < _maxShoutreach) {
			_resizeIconState = RESIZE_ICON_MIN;
		} else if (_peopleCount >= _maxShoutreach) {
			_resizeIconState = RESIZE_ICON_MAX;
		} else {
			_resizeIconState = RESIZE_ICON;
		}
	}

	// called after zoom occurs
	public void handleZoomLevelChange() {
		_arePixelsCalculated = false;
	}

	// called when user drags resize icon
	public void resize(int radiusChangePx) {
		_resizeAdjustmentPixels += radiusChangePx;
		// force minimum radius size
		if (_maxRadiusPixels + _resizeAdjustmentPixels <= C.MIN_RADIUS_PX) {
			_resizeAdjustmentPixels = (int) (C.MIN_RADIUS_PX - _maxRadiusPixels);
		}
		// force maximum radius size
		if (!C.CONFIG_ADMIN_SUPERPOWERS && _resizeAdjustmentPixels > 0) {
			_resizeAdjustmentPixels = 0;
		}
		calculateCurrentRadius();
	}

	// finds a zoom level that displays the circle nicely
	// http://groups.google.com/group/google-maps-api/browse_thread/thread/6ff83431273c6adb/0f83700b2a7b4144
	public void calibrateZoomLevelToShowCircle() {
		if (_mapSizeConstraint == -1) {
			_mapSizeConstraint = (_map.getWidth() >= _map.getHeight()) ? _map.getHeight() : _map.getWidth();
		}
		double diameter = _currentRadiusPixels + _currentRadiusPixels;
		if (diameter > 0) {
			double factor = _mapSizeConstraint / diameter;
			// each zoom level shows half as much as the previous
			double zoomChange = Math.log(factor) / Math.log(2); // =
			// log2(factor)
			int flooredZoomChange = (int) Math.floor(zoomChange);
			_zoomLevel += flooredZoomChange;
		} else {
			_zoomLevel = _map.getMaxZoomLevel() - 1;
		}

		// TODO: fancy smooth zooming
		// TODO: can we use zoom to show a given lat/long span _map.getController().zoomToSpan(latSpanE6, lonSpanE6);

		_map.getController().setZoom(_zoomLevel);
		_calibrateZoomLevelForRadiusSize = false;
		_arePixelsCalculated = false;
	}

//	public boolean isShoutreachRadiusSet() {
//		return _isShoutreachRadiusSet;
//	}

	public int getPeopleCount() {
		return _peopleCount;
	}

	public Point getResizeIconLocation() {
		return _resizeIconLocation;
	}

	public void setMapView(CustomMapView mapView) {
		_map = mapView;
		if (!MeterPixelConverter.isInitialized) {
			MeterPixelConverter.pixelsPer100Meters = _map.getProjection().metersToEquatorPixels(100);
			MeterPixelConverter.baseZoomLevel = _map.getZoomLevel();
			MeterPixelConverter.isInitialized = true;
		}
	}

	public int getCurrentPower() {
		return User.calculatePower(_peopleCount);
	}

	// TODO: We can replace this with:
	// public static int metersToRadius(float meters, MapView map, double latitude) {
	// 	 return (int) (map.getProjection().metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(latitude))));         
	// }
	// But that will make things less consistent to debug.
	private static class MeterPixelConverter {
		public static boolean isInitialized = false;
		public static float pixelsPer100Meters = 0;
		public static int baseZoomLevel = 0;
		
		// http://stackoverflow.com/questions/2077054/how-to-compute-a-radius-around-a-point-in-an-android-mapview
		public static float metersToPixels(float meters, int currentZoomLevel) {
			float pixelsAtBaseZoom = (pixelsPer100Meters * meters) / 100;
			float result = (float) (pixelsAtBaseZoom * (Math.pow(2, currentZoomLevel - baseZoomLevel)));
			return Math.abs(result);
		}
	}
	
	// MISC TECHNIQUES ////////////////////////////////////////////////////////

	// private boolean _baseRadiusPxIsWrong; // is the radius calculation based
	// on the current zoom level? or is the pixel value wrong?
	// private GeoPoint _lastTopRadiusGeoPoint; // the coordinates of the top of
	// the current radius
	// private float _baseRadiusMeters;// radius length in meters of baseRadius
	// (baseRadius is radius length with no user resizing)
	// private float _baseRadiusPx; // baseRadius in pixels
	// private double _baseTopRadiusAbsoluteMeters; // how many meters north of
	// equator is the top of baseRadius
	// private int _latRadiusLatForPeopleCount;

	// distance math if we need it
	// GeoPoint radiusEdge =
	// _mapView.getProjection().fromPixels((int)(_userLocationPx.x + _radiusPx),
	// _userLocationPx.y);
	// Location location1 = new Location("gps");
	// location1.setLatitude(radiusEdge.getLatitudeE6() / 1E6);
	// location1.setLongitude(radiusEdge.getLongitudeE6() / 1E6);
	// Location location2 = new Location("gps");
	// location2.setLatitude(_lastUserLocation.getLatitudeE6() / 1E6);
	// location2.setLongitude(_lastUserLocation.getLongitudeE6() / 1E6);
	// _radiusMeters = location2.distanceTo(location1);

	// public void setPopulationDensity(int level, double density) {
	// TODO: don't hardcode level
	// TODO: do we keep track of resize if density changes? or just say fuck it?
	// _density = density;
	// _baseRadiusMeters = User.calculateRadius(level, _density);
	// _calibrateZoomLevelForRadiusSize = true;
	// _baseRadiusPxIsWrong = true;
	// this calls draw() immediately rather than wait for next interval
	// if (_canvas != null) {
	// _mapView.draw(_canvas);
	// }
	// }

	// if (Math.abs(_lastTopRadiusGeoPoint.getLatitudeE6() -
	// _latRadiusLatForPeopleCount) > 50) {
	// _latRadiusLatForPeopleCount = _lastTopRadiusGeoPoint.getLatitudeE6();
	// Location l1 = new Location("GPS");
	// l1.setLatitude(_lastTopRadiusGeoPoint.getLatitudeE6() / 1E6);
	// l1.setLongitude(_lastTopRadiusGeoPoint.getLongitudeE6() / 1E6);
	// Location l2 = new Location("GPS");
	// l2.setLatitude(_lastUserLocationGeoPoint.getLatitudeE6() / 1E6);
	// l2.setLongitude(_lastUserLocationGeoPoint.getLongitudeE6() / 1E6);
	// float dist = l1.distanceTo(l2);
	// _peopleCount = (int)(Math.PI * dist * dist * _density);
	// }

	// OLD LOGIC
	// This kept circle constant by nailing down the top GeoPoint of the cirlce.
	// if (_lastTopRadiusGeoPoint != null) {
	// Point topRadiusPx = _map.getProjection().toPixels(_lastTopRadiusGeoPoint,
	// null);
	// _baseRadiusPx = _userLocationPx.y - topRadiusPx.y; // downward y axis
	// _resizeAdjustmentPx = 0;
	// calculateCurrentRadius();
	// updatePeopleCount(true);
	// }

	// // initializes pixel values based on radiusMeters
	// private float calculateRadiusPixelsAtCurrentZoomLevel() {
	// // this is probably a little inaccurate, so only do when radiusMeters is
	// initialized
	// double topRadiusLat = _lastUserLocationGeoPoint.getLatitudeE6() / 1E6; //
	// get latitude of user location
	// _baseTopRadiusAbsoluteMeters = topRadiusLat * C.DEGREE_LAT_IN_METERS; //
	// convert that to meters
	// _baseTopRadiusAbsoluteMeters += _baseRadiusMeters; // add the radius to
	// it
	// topRadiusLat = _baseTopRadiusAbsoluteMeters / C.DEGREE_LAT_IN_METERS; //
	// convert back to degrees lat
	// GeoPoint topRadiusGeoPoint = new GeoPoint((int) (topRadiusLat * 1E6),
	// _lastUserLocationGeoPoint.getLongitudeE6());
	// Point topRadiusPx = _map.getProjection().toPixels(topRadiusGeoPoint,
	// null);
	// return _userLocationPx.y - topRadiusPx.y; // downward y axis
	// }

	// _lastTopRadiusGeoPoint =
	// _map.getProjection().fromPixels(_userLocationPx.x, (int)
	// (_userLocationPx.y - _currentRadiusPx));
}