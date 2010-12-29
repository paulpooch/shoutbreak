package com.shoutbreak.ui;

import java.util.ArrayList;
import android.graphics.drawable.Drawable;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

// Note this class isn't actually used... it's for adding 'pins' to the map if we ever wanna do that
public class CustomItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> _overlays = new ArrayList<OverlayItem>();
	
	public CustomItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		// TODO Auto-generated constructor stub
	}

	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		  return _overlays.get(i);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return _overlays.size();
	}
	
	public void addOverlay(OverlayItem overlay) {
	    _overlays.add(overlay);
	    populate();
	}

	@Override
	protected boolean onTap(int index) {
	  /*OverlayItem item = _overlays.get(index);
	  AlertDialog.Builder dialog = new AlertDialog.Builder(_context);
	  dialog.setTitle(item.getTitle());
	  dialog.setMessage(item.getSnippet());
	  dialog.show();*/
	  return true;
	}
	
}
