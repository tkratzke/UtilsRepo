package com.skagit.utils.massFinder;

import java.util.ArrayList;

import com.skagit.util.navigation.LatLng3;
import com.skagit.util.navigation.TangentCylinder;
import com.skagit.util.navigation.TangentCylinder.FlatLatLng;

public class LatLngMassFinder extends MassFinder {
    public class LatLngItem extends Item {
	final TangentCylinder.FlatLatLng _flatLatLng;

	public LatLngItem(final double weight, final int itemTypeId, final TangentCylinder.FlatLatLng flatLatLng) {
	    super(weight, itemTypeId);
	    _flatLatLng = flatLatLng;
	}

	@Override
	final public double getX() {
	    return getFlatLatLng().getEastOffsetNmi();
	}

	@Override
	final public double getY() {
	    return getFlatLatLng().getNorthOffsetNmi();
	}

	public TangentCylinder.FlatLatLng getFlatLatLng() {
	    return _flatLatLng;
	}

    }

    protected TangentCylinder _tangentCylinder;

    public LatLngMassFinder() {
	super();
	_tangentCylinder = null;
    }

    public LatLngMassFinder(final FlatLatLng[] flatLatLngs) {
	super();
	_tangentCylinder = flatLatLngs[0].getOwningTangentCylinder();
	final int nFlatLatLngs = flatLatLngs.length;
	final ArrayList<Item> itemList = new ArrayList<>(nFlatLatLngs);
	for (int k = 0; k < nFlatLatLngs; ++k) {
	    itemList.add(new LatLngItem(1d, 0, flatLatLngs[k]));
	}
	setListAndExtrema(itemList);
    }

    public LatLngMassFinder(final TangentCylinder tangentCylinder) {
	super();
	_tangentCylinder = tangentCylinder;
	setListAndExtrema();
    }

    public LatLngItem[] getClosestLatLngItems(final int nPointsToFind, final LatLngItem latLngItem) {
	final Item[] items = getClosestItems(nPointsToFind, latLngItem);
	final int nFound = items.length;
	final LatLngItem latLngItems[] = new LatLngItem[nFound];
	for (int k = 0; k < nFound; ++k) {
	    latLngItems[k] = (LatLngItem) items[k];
	}
	return latLngItems;
    }

    public LatLngItem getSimpleLatLngItem(final LatLng3 latLng) {
	return new LatLngItem(/* weight= */1d, /* itemTypeId= */0, _tangentCylinder.convertToMyFlatLatLng(latLng));
    }

}
