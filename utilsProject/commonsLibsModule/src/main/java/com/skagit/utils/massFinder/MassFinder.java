package com.skagit.utils.massFinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.myLogger.MyLogger;

public class MassFinder {
    final private static int _SmallArraySize = 16;
    final public static int _AllItemTypeIds = Integer.MIN_VALUE;
    final private Box _staticBox;
    final private TreeMap<Integer, double[]> _itemTypeIdToWeight;
    final private Box _currentBox;
    private MassFinder[] _sons;
    private ArrayList<Item> _smallList;
    private boolean _dividingLineIsVertical;
    private double _dividingLine;
    final private int _level;

    private static class Box {
	private double _left, _low, _right, _high;

	/** Set so that the first stretch sets _left, _low, _right, and _high. */
	private Box() {
	    initialize();
	}

	private void initialize() {
	    _left = _low = Double.POSITIVE_INFINITY;
	    _right = _high = Double.NEGATIVE_INFINITY;
	}

	private Box(final double left, final double low, final double right, final double high) {
	    _left = left;
	    _low = low;
	    _right = right;
	    _high = high;
	}

	/** Returns true if some bound is pushed out. */
	private boolean stretchCurrentBox(final Item item) {
	    final double x = item.getX(), y = item.getY();
	    final boolean returnValue = x < _left || y < _low || x > _right || y > _high;
	    _left = Math.min(_left, x);
	    _low = Math.min(_low, y);
	    _right = Math.max(_right, x);
	    _high = Math.max(_high, y);
	    return returnValue;
	}

	/** Returns true if some bound is pushed out. */
	private boolean stretchCurrentBox(final Box box) {
	    final double left = box._left, low = box._low, right = box._right, high = box._high;
	    final boolean returnValue = left < _left || low < _low || right > _right || high > _high;
	    _left = Math.min(_left, left);
	    _low = Math.min(_low, low);
	    _right = Math.max(_right, right);
	    _high = Math.max(_high, high);
	    return returnValue;
	}

	public String getString() {
	    final String s = String.format("LLRH=[%.3f,%.3f,%.3f,%.3f]", _left, _low, _right, _high);
	    return s;
	}

	@Override
	public String toString() {
	    return getString();
	}

	public int getNInfinities(final boolean horizontal) {
	    if (horizontal) {
		return (Double.isFinite(_left) ? 1 : 0) + (Double.isFinite(_right) ? 1 : 0);
	    }
	    return (Double.isFinite(_low) ? 1 : 0) + (Double.isFinite(_high) ? 1 : 0);
	}

	private boolean contains(final Item item) {
	    final double x = item.getX(), y = item.getY();
	    return _left <= x && x < _right && _low <= y && y < _high;
	}
    }

    abstract public static class Item {
	final public double _weight;
	final public int _itemTypeId;

	public abstract double getX();

	public abstract double getY();

	public boolean compatibleWith(final Item item) {
	    return true;
	}

	public Item(final double weight, final int itemTypeId) {
	    _weight = weight;
	    _itemTypeId = itemTypeId;
	}

	public String getString() {
	    final StringBuffer b = new StringBuffer();
	    b.append(NumericalRoutines.getString(new double[] { getX(), getY() }));
	    b.append(';').append(NumericalRoutines.getString(_weight, 8, 3));
	    b.append('+').append(_itemTypeId);
	    return new String(b);
	}

	@Override
	public String toString() {
	    return getString();
	}
    }

    private static Comparator<Item> _byX = new Comparator<>() {
	@Override
	public int compare(final Item item0, final Item item1) {
	    if (item0 == null) {
		return item1 != null ? 1 : 0;
	    } else if (item1 == null) {
		return -1;
	    } else {
		return Double.compare(item0.getX(), item1.getX());
	    }
	}
    };
    private static Comparator<Item> _byY = new Comparator<>() {
	@Override
	public int compare(final Item item0, final Item item1) {
	    if (item0 == null) {
		return item1 != null ? 1 : 0;
	    } else if (item1 == null) {
		return -1;
	    } else {
		return Double.compare(item0.getY(), item1.getY());
	    }
	}
    };

    public MassFinder() {
	this(/* staticLeft= */Double.NEGATIVE_INFINITY, //
		/* staticLow= */Double.NEGATIVE_INFINITY, //
		/* staticRight= */Double.POSITIVE_INFINITY, //
		/* staticHigh= */Double.POSITIVE_INFINITY, //
		/* level= */1);
    }

    /** Reason 1 of 2 for the class' existence. */
    public Item[] getClosestItems(final int nToFind, final Item item) {
	final double[] dSquareds = new double[nToFind];
	final Item[] incumbents = new Item[nToFind];
	for (int i = 0; i < nToFind; ++i) {
	    dSquareds[i] = Double.POSITIVE_INFINITY;
	    incumbents[i] = null;
	}
	/**
	 * Find the List-Type MassFinder that contains (x,y) to give a good bound.
	 */
	final Stack<MassFinder> toConsider = new Stack<>();
	toConsider.push(this);
	MassFinder goodGuess = null;
	while (!toConsider.isEmpty()) {
	    final MassFinder massFinder = toConsider.pop();
	    if (massFinder._staticBox.contains(item)) {
		if (massFinder._sons == null) {
		    goodGuess = massFinder;
		    break;
		}
		toConsider.push(massFinder._sons[0]);
		toConsider.push(massFinder._sons[1]);
	    }
	}
	if (goodGuess != null) {
	    goodGuess.updateForGetClosestPoints(item, dSquareds, incumbents);
	}
	updateForGetClosestPoints(item, dSquareds, incumbents);
	Item[] foundItems = null;
	for (int iPass = 0; iPass < 2; ++iPass) {
	    int nFound = 0;
	    for (int k = 0; k < nToFind; ++k) {
		final Item incumbent = incumbents[k];
		if (incumbent != null) {
		    if (iPass == 1) {
			foundItems[nFound] = incumbent;
		    }
		    ++nFound;
		}
	    }
	    if (iPass == 0) {
		if (nFound == nToFind) {
		    return incumbents;
		}
		foundItems = new Item[nFound];
	    }
	}
	return foundItems;
    }

    private void updateForGetClosestPoints(final Item item, final double[] dSquareds, final Item[] items) {
	final int nToFind = dSquareds.length;
	double threshold = dSquareds[nToFind - 1];
	final double minX = getMinX(), maxX = getMaxX();
	final double x = item.getX(), y = item.getY();
	final double dX = x < minX ? (minX - x) : (x > maxX ? (x - maxX) : 0);
	double dXSquared = dX * dX;
	if (dXSquared >= threshold) {
	    return;
	}
	final double minY = getMinY(), maxY = getMaxY();
	final double dY = y < minY ? (minY - y) : (y > maxY ? (y - maxY) : 0);
	double dYSquared = dY * dY;
	if (dYSquared >= threshold) {
	    return;
	}
	double dSquared = dXSquared + dYSquared;
	if (dSquared >= threshold) {
	    return;
	}
	if (_sons == null) {
	    final int nItems = _smallList.size();
	    NextItem: for (int k = 0; k < nItems; ++k) {
		final Item incumbent = _smallList.get(k);
		if (!item.compatibleWith(incumbent)) {
		    continue;
		}
		for (int k1 = 0; k1 < nToFind; ++k1) {
		    if (incumbent == items[k1]) {
			/** Cannot use this item; we've seen it before. */
			continue NextItem;
		    }
		}
		final double itemX = incumbent.getX(), itemY = incumbent.getY();
		dXSquared = (x - itemX) * (x - itemX);
		if (dXSquared >= threshold) {
		    continue;
		}
		dYSquared = (y - itemY) * (y - itemY);
		if (dYSquared >= threshold) {
		    continue;
		}
		dSquared = dXSquared + dYSquared;
		if (dSquared >= threshold) {
		    continue;
		}
		/**
		 * Working up from the bottom, find the first acceptable one that is less than
		 * or equal. We can start at n-2 since dSquared < threshold.
		 */
		boolean foundAHome = false;
		for (int k1 = nToFind - 2; k1 >= 0; --k1) {
		    if (dSquareds[k1] <= dSquared) {
			/** dSquared goes in k1+1. */
			dSquareds[k1 + 1] = dSquared;
			items[k1 + 1] = incumbent;
			foundAHome = true;
			break;
		    }
		    dSquareds[k1 + 1] = dSquareds[k1];
		    items[k1 + 1] = items[k1];
		}
		if (!foundAHome) {
		    dSquareds[0] = dSquared;
		    items[0] = incumbent;
		}
		threshold = dSquareds[nToFind - 1];
	    }
	    return;
	}
	_sons[0].updateForGetClosestPoints(item, dSquareds, items);
	_sons[1].updateForGetClosestPoints(item, dSquareds, items);
    }

    /**
     * Reason 2 of 2 for the class' existence. This one is not threadsafe so if some
     * other routine is adding extra items at the same time that this is called,
     * there will be a problem..
     */
    public double getMass(final double left, final double low, final double right, final double high,
	    final boolean includeLeft, final boolean includeLow, final boolean includeRight, final boolean includeHigh,
	    final int itemTypeId) {
	/** Check for "none of my items is in the box." */
	final double minX = getMinX(), maxX = getMaxX();
	final double minY = getMinY(), maxY = getMaxY();
	if (left >= maxX || right < minX) {
	    return 0d;
	} else if (!includeRight && right == minX) {
	    return 0d;
	} else if (low >= maxY || high < minY) {
	    return 0d;
	} else if (!includeHigh && high == minY) {
	    return 0d;
	} else {
	    /** Check for "all of me is within the box? */
	    if (left < minX || (left == minX && includeLeft)) {
		if (low < minY || (low == minY && includeLow)) {
		    if (right >= maxX && high >= maxY) {
			return getTotalWeight(itemTypeId);
		    }
		}
	    }
	}
	/** Some overlap. */
	if (_sons == null) {
	    double total = 0d;
	    final int nItems = _smallList.size();
	    for (int k = 0; k < nItems; ++k) {
		final Item item = _smallList.get(k);
		if (itemTypeId == _AllItemTypeIds || item._itemTypeId == itemTypeId) {
		    final double itemX = item.getX(), itemY = item.getY();
		    if (itemX > left || (itemX == left && includeLeft)) {
			if (itemX < right || (itemX == right && includeRight)) {
			    if (itemY > low || (itemY == low && includeLow)) {
				if (itemY < high || (itemY == high && includeHigh)) {
				    total += item._weight;
				}
			    }
			}
		    }
		}
	    }
	    return total;
	}
	double total = 0d;
	total += _sons[0].getMass(left, low, right, high, includeLeft, includeLow, includeRight, includeHigh,
		itemTypeId);
	total += _sons[1].getMass(left, low, right, high, includeLeft, includeLow, includeRight, includeHigh,
		itemTypeId);
	return total;
    }

    protected void addToAllItems(final ArrayList<Item> allItems) {
	if (_sons != null) {
	    for (int i = 0; i < 2; ++i) {
		if (_sons[i] != null) {
		    _sons[i].addToAllItems(allItems);
		}
	    }
	    return;
	}
	allItems.addAll(_smallList);
    }

    public ArrayList<Item> getAllItems() {
	final ArrayList<Item> items = new ArrayList<>();
	addToAllItems(items);
	return items;
    }

    @Override
    public String toString() {
	return getString();
    }

    public String getString() {
	final StringBuffer b = new StringBuffer();
	indent(b);
	b.append("[#,TotalWt,level] = [");
	final int nItems = getNItems();
	b.append(nItems).append(',');
	final double totalWeight = getTotalWeight(_AllItemTypeIds);
	b.append(totalWeight).append(',').append(Integer.toBinaryString(_level));
	b.append("].");
	b.append("  [left,low,right,high] = ");
	b.append(_staticBox.getString());
	b.append("  [minX,minY,maxX,maxY] = ");
	b.append(NumericalRoutines.getString(new double[] { getMinX(), getMinY(), getMaxX(), getMaxY() }));
	if (_sons != null) {
	    b.append('\n');
	    indent(b);
	    b.append("DividingLine = " + NumericalRoutines.getString(_dividingLine, 9, 3)
		    + (_dividingLineIsVertical ? '|' : '-'));
	    b.append(", SonWeights = ");
	    final double[] sonWeights = new double[2];
	    for (int i = 0; i < 2; ++i) {
		sonWeights[i] = _sons[i] != null ? _sons[i].getTotalWeight(_AllItemTypeIds) : 0d;
	    }
	    b.append(NumericalRoutines.getString(sonWeights));
	    for (int i = 0; i < 2; ++i) {
		if (_sons[i] != null) {
		    b.append('\n').append(_sons[i]);
		}
	    }
	} else {
	    for (final Item item : _smallList) {
		b.append('\n');
		indent(b);
		b.append(item);
	    }
	}
	return new String(b);
    }

    private int getNItems() {
	if (_sons == null) {
	    return _smallList.size();
	}
	/** Find which son to add to. */
	int nItems = 0;
	for (int k = 0; k < 2; ++k) {
	    final MassFinder son = _sons[k];
	    nItems += son == null ? 0 : son.getNItems();
	}
	return nItems;
    }

    private MassFinder(final double left, final double low, final double right, final double high, final int level) {
	_staticBox = new Box(left, low, right, high);
	_currentBox = new Box();
	_level = level;
	_itemTypeIdToWeight = new TreeMap<>();
	_sons = null;
	_smallList = null;
    }

    public double getMinX() {
	return _currentBox._left;
    }

    public double getMinY() {
	return _currentBox._low;
    }

    public double getMaxX() {
	return _currentBox._right;
    }

    public double getMaxY() {
	return _currentBox._high;
    }

    private double getTotalWeight(final int itemTypeId) {
	double[] totalWeightArray = _itemTypeIdToWeight.get(itemTypeId);
	if (totalWeightArray == null) {
	    totalWeightArray = new double[] { 0d };
	    if (_sons != null) {
		for (final MassFinder son : _sons) {
		    if (son != null) {
			totalWeightArray[0] += son.getTotalWeight(itemTypeId);
		    }
		}
	    } else {
		final int nItems = _smallList.size();
		for (int k = 0; k < nItems; ++k) {
		    final Item item = _smallList.get(k);
		    if (itemTypeId == _AllItemTypeIds || item._itemTypeId == itemTypeId) {
			totalWeightArray[0] += item._weight;
		    }
		}
	    }
	    _itemTypeIdToWeight.put(itemTypeId, totalWeightArray);
	}
	return totalWeightArray[0];
    }

    private static Box getExtrema(final List<Item> items) {
	final Box box = new Box(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
		Double.NEGATIVE_INFINITY);
	final int nItems = items.size();
	for (int i = 0; i < nItems; ++i) {
	    final Item item = items.get(i);
	    box.stretchCurrentBox(item);
	}
	return box;
    }

    /** Returns true if _currentBox changed. */
    protected boolean addExtraItem(final Item item) {
	/** Find which son to add to. */
	final int nSons = _sons == null ? 0 : 2;
	for (int k = 0; k < nSons; ++k) {
	    final MassFinder son = _sons[k];
	    if (son != null && son._staticBox.contains(item)) {
		if (son.addExtraItem(item)) {
		    return _currentBox.stretchCurrentBox(item);
		}
		return false;
	    }
	}
	if (_smallList == null) {
	    _smallList = new ArrayList<>();
	}
	_smallList.add(item);
	if (_smallList.size() > 2 * _SmallArraySize) {
	    /** Split this MassFinder into _sons. */
	    trySplit(_smallList, /* forceHorizontalDividingLine= */false, /* forceVerticalDividingLine= */false);
	}
	return _currentBox.stretchCurrentBox(item);
    }

    private void trySplit(final List<Item> items, final boolean forceHorizontalDividingLine,
	    final boolean forceVerticalDividingLine) {
	/**
	 * A son S gets items that have a value at least as big as S's low value, but
	 * less than S's high value.
	 */
	final int nItems = items.size();
	/** Decide to use a vertical dividing line or a horizontal dividing line. */
	if (forceHorizontalDividingLine) {
	    _dividingLineIsVertical = false;
	} else if (forceVerticalDividingLine) {
	    _dividingLineIsVertical = true;
	} else {
	    final int nHorizontalInfinities = _staticBox.getNInfinities(/* horizontal= */true);
	    final int nVerticalInfinities = _staticBox.getNInfinities(/* horizontal= */false);
	    if (nHorizontalInfinities > nVerticalInfinities) {
		_dividingLineIsVertical = true;
	    } else if (nVerticalInfinities > nHorizontalInfinities) {
		_dividingLineIsVertical = false;
	    } else {
		final Box llrh = getExtrema(items);
		_dividingLineIsVertical = llrh._right - llrh._left > llrh._high - llrh._low;
	    }
	}
	final Comparator<Item> byWhat = _dividingLineIsVertical ? _byX : _byY;
	items.sort(byWhat);
	final int i1 = (nItems - 1) / 2, i2 = nItems / 2;
	final Item item1 = items.get(i1), item2 = items.get(i2);
	if (_dividingLineIsVertical) {
	    _dividingLine = (item1.getX() + item2.getX()) / 2d;
	} else {
	    _dividingLine = (item1.getY() + item2.getY()) / 2d;
	}
	/** Find the smallest index that's as big as dividingLine. */
	int tooLow = i2 - 1;
	for (; tooLow >= 0; --tooLow) {
	    final Item lowItem = items.get(tooLow);
	    final double value = _dividingLineIsVertical ? lowItem.getX() : lowItem.getY();
	    if (value < _dividingLine) {
		break;
	    }
	}
	final int dividingIndex = tooLow + 1;
	/**
	 * If the smallest index that's as big as dividing line is 0, then everything is
	 * the same. Split it the other way. If we still have that problem, then we're
	 * looking at the same point, and we'll use a list.
	 */
	if (dividingIndex == 0) {
	    /** If trying to split and under direct orders, just fail. */
	    if (forceHorizontalDividingLine || forceVerticalDividingLine) {
		_sons = null;
		return;
	    }
	    /** Not forced, but failed. Try the other way. */
	    trySplit(items, _dividingLineIsVertical, !_dividingLineIsVertical);
	    return;
	}
	_sons = new MassFinder[2];
	double left = _staticBox._left;
	double right = _dividingLineIsVertical ? _dividingLine : _staticBox._right;
	double low = _staticBox._low;
	double high = _dividingLineIsVertical ? _staticBox._high : _dividingLine;
	List<Item> subList = items.subList(0, dividingIndex);
	_sons[0] = new MassFinder(left, low, right, high, _level * 2);
	_sons[0].setListAndExtrema(subList);
	left = _dividingLineIsVertical ? _dividingLine : _staticBox._left;
	right = _staticBox._right;
	low = _dividingLineIsVertical ? _staticBox._low : _dividingLine;
	high = _staticBox._high;
	subList = items.subList(dividingIndex, nItems);
	_sons[1] = new MassFinder(left, low, right, high, _level * 2 + 1);
	_sons[1].setListAndExtrema(subList);
	_smallList = null;
    }

    public void setListAndExtrema(final List<Item> items) {
	_itemTypeIdToWeight.clear();
	_sons = null;
	_smallList = null;
	_currentBox.initialize();
	final int nItems = items.size();
	if (nItems > _SmallArraySize) {
	    trySplit(items, false, false);
	    if (_sons != null) {
		/** _sons have their extrema set. Set ours. */
		for (final MassFinder son : _sons) {
		    if (son != null) {
			_currentBox.stretchCurrentBox(son._currentBox);
		    }
		}
		return;
	    }
	}
	_smallList = new ArrayList<>(nItems);
	_smallList.addAll(items);
	_smallList.trimToSize();
	for (int k = 0; k < nItems; ++k) {
	    _currentBox.stretchCurrentBox(_smallList.get(k));
	}
    }

    public void setListAndExtrema() {
	setListAndExtrema(new ArrayList<Item>());
    }

    /** Part of toString. */
    private void indent(final StringBuffer b) {
	if (_level > 1) {
	    final String levelString = Integer.toBinaryString(_level);
	    final int indentLength = levelString.length() - 1;
	    for (int i = 0; i < indentLength; ++i) {
		b.append("  ");
	    }
	}
    }

    public static Item getSimpleItem(final double weight, final int itemTypeId, final double x, final double y) {
	return new Item(weight, itemTypeId) {

	    @Override
	    public double getX() {
		return x;
	    }

	    @Override
	    public double getY() {
		return y;
	    }
	};
    }

    public static void main(final String[] args) {
	final MassFinder massFinder = new MassFinder();
	final ArrayList<Item> items = new ArrayList<>();
	for (double x = 1d; x <= 2d; x += 1d) {
	    for (double y = 1d; y <= 1d; y += 1d) {
		final Item item = getSimpleItem(1d, 0, x, y);
		items.add(item);
	    }
	}
	massFinder.setListAndExtrema(items);
	final int n = 3;
	final Item[] items2 = massFinder.getClosestItems(n, getSimpleItem(1d, 0, 3d, 3d));
	for (final Item item : items2) {
	    MyLogger.out(null, item.toString());
	}
    }
}
