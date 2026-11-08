package com.skagit.utils.color;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.StaticUtilities;

/**
 * <pre>
 * There are 140 standard html colors.
 * Standard HTML colors: http://kb.iu.edu/data/aetf.html
 * http://www.tomjewett.com/colors/hsb.html
 * Good discussion on legibility:
 * 	https://stackoverflow.com/questions/946544/good-text-foreground-color-for-a-given-background-color
 * </pre>
 */
public class ColorUtil {
	public enum ColorGrouping {
		RED, GREEN, YELLOW, PURPLE, BROWN, PINK, ORANGE, BLUE, WHITE, GRAY
	}

	public final static ColorGrouping[] _ColorGroupings = ColorGrouping.values();

	private static ColorGrouping groupNameToColorGrouping(final String groupName) {
		final int nColorGroupings = _ColorGroupings.length;
		for (int k = 0; k < nColorGroupings; ++k) {
			final ColorGrouping colorGrouping = _ColorGroupings[k];
			final String colorGroupingNameLc = colorGrouping.name().toLowerCase();
			if (groupName.toLowerCase().contains(colorGroupingNameLc)) {
				return colorGrouping;
			}
		}
		return null;
	}

	final private static String _BackgroundColorName = "Navy";
	final public static int[] _BackgroundRgb;
	final public static double _ColorBackgroundLum;
	final public static double _ColorBackgroundH;

	/**
	 * We assume dark background so we want brightness. We also don't want too much
	 * white so we want saturation.
	 */
	final private static Comparator<int[]> _CriticalComparator = new Comparator<>() {

		@Override
		public int compare(final int[] rgb0, final int[] rgb1) {
			final float[] hsb0 = rgbToHsb(rgb0);
			final double h0 = hsb0[0];
			final double rawAbsDiffH0 = Math.abs(h0 - _ColorBackgroundH);
			final double h0Diff = Math.min(rawAbsDiffH0, 1d - rawAbsDiffH0);
			final boolean suffGeneralDiff0;
			if (h0Diff > 0.25) {
				suffGeneralDiff0 = true;
			} else {
				final double lum0 = getLuminance(rgb0);
				final double lum0Diff = Math.abs(lum0 - _ColorBackgroundLum);
				suffGeneralDiff0 = lum0Diff > 0.25;
			}
			final float[] hsb1 = rgbToHsb(rgb1);
			final double h1 = hsb1[0];
			final double rawAbsDiffH1 = Math.abs(h1 - _ColorBackgroundH);
			final double h1Diff = Math.min(rawAbsDiffH1, 1d - rawAbsDiffH1);
			final boolean suffGeneralDiff1;
			if (h1Diff > 0.25) {
				suffGeneralDiff1 = true;
			} else {
				final double lum1 = getLuminance(rgb1);
				final double lum1Diff = Math.abs(lum1 - _ColorBackgroundLum);
				suffGeneralDiff1 = lum1Diff > 0.25;
			}
			if (suffGeneralDiff0 && !suffGeneralDiff1) {
				return -1;
			}
			if (!suffGeneralDiff0 && suffGeneralDiff1) {
				return 1;
			}
			final double s0 = hsb0[1];
			final double b0 = hsb0[2];
			final double s1 = hsb1[1];
			final double b1 = hsb1[2];
			final boolean suffS0 = s0 >= 0.5;
			final boolean suffB0 = b0 >= 0.6;
			final boolean suffS1 = s1 >= 0.5;
			final boolean suffB1 = b1 >= 0.6;
			if (suffS0 == suffS1 && suffB0 == suffB1) {
				final double m0 = Math.min(s0, b0);
				final double m1 = Math.min(s1, b1);
				if (m0 != m1) {
					return m0 > m1 ? -1 : 1;
				}
				return CombinatoricTools._ByAllInOrder.compare(rgb0, rgb1);
			}
			if (suffB0 && !suffB1) {
				/** 0 is better. */
				return -1;
			}
			if (!suffB0 && suffB1) {
				/** 1 is better. */
				return 1;
			}
			if (suffS0 && !suffS1) {
				return -1;
			}
			assert !suffS0 && suffS1;
			return 1;
		}
	};

	public static class NameAndRgb {
		public final String _name;
		public final int[] _rgb;

		public NameAndRgb(final String name, final int[] rgb) {
			_name = name;
			_rgb = rgb;
		}
	}

	public static class ColorGroup {

		public final NameAndRgb[] _nameAndRgbs;
		public final String[] _memberNames;

		public ColorGroup(final ColorGrouping colorGrouping, final String[] memberNames) {
			final int nMembers = memberNames.length;
			_nameAndRgbs = new NameAndRgb[nMembers];
			for (int k = 0; k < nMembers; ++k) {
				final String name = memberNames[k];
				final int[] rgb = _NameToRgb.get(name);
				_nameAndRgbs[k] = new NameAndRgb(name, rgb);
			}
			/** Sort them. */
			Arrays.sort(_nameAndRgbs, (nameAndRgb0, nameAndRgb1) -> {
				final int[] rgb0 = nameAndRgb0._rgb;
				final int[] rgb1 = nameAndRgb1._rgb;
				return _CriticalComparator.compare(rgb0, rgb1);
			});
			_memberNames = new String[nMembers];
			for (int k = 0; k < nMembers; ++k) {
				_memberNames[k] = _nameAndRgbs[k]._name;
			}
		}

		public String getColorGroupName() {
			return _nameAndRgbs[0]._name;
		}

		public String getColorName(final int idx) {
			return _nameAndRgbs[Math.abs(idx) % _nameAndRgbs.length]._name;
		}
	}

	/** The colors and ColorGroups. */
	final public static int[] _IndianRed = { 205, 92, 92 };
	final public static int[] _LightCoral = { 240, 128, 128 };
	final public static int[] _Salmon = { 250, 128, 114 };
	final public static int[] _DarkSalmon = { 233, 150, 122 };
	final public static int[] _Crimson = { 220, 20, 60 };
	final public static int[] _Red = { 255, 0, 0 };
	final public static int[] _FireBrick = { 178, 34, 34 };
	final public static int[] _DarkRed = { 139, 0, 0 };
	final public static int[][] _Reds = { _Red, _IndianRed, _LightCoral, _Salmon, _DarkSalmon, _Crimson, _FireBrick,
			_DarkRed };

	final public static int[] _Pink = { 255, 192, 203 };
	final public static int[] _LightPink = { 255, 182, 193 };
	final public static int[] _HotPink = { 255, 105, 180 };
	final public static int[] _DeepPink = { 255, 20, 147 };
	final public static int[] _MediumVioletRed = { 199, 21, 133 };
	final public static int[] _PaleVioletRed = { 219, 112, 147 };
	final public static int[][] _Pinks = { _Pink, _LightPink, _HotPink, _DeepPink, _MediumVioletRed, _PaleVioletRed };

	final public static int[] _LightSalmon = { 255, 160, 122 };
	final public static int[] _Coral = { 255, 127, 80 };
	final public static int[] _Tomato = { 255, 99, 71 };
	final public static int[] _OrangeRed = { 255, 69, 0 };
	final public static int[] _DarkOrange = { 255, 140, 0 };
	final public static int[] _Orange = { 255, 165, 0 };
	final public static int[][] _Oranges = { _Orange, _LightSalmon, _Coral, _Tomato, _OrangeRed, _DarkOrange };

	final public static int[] _Gold = { 255, 215, 0 };
	final public static int[] _Yellow = { 255, 255, 0 };
	final public static int[] _LightYellow = { 255, 255, 224 };
	final public static int[] _LemonChiffon = { 255, 250, 205 };
	final public static int[] _LightGoldenrodYellow = { 250, 250, 210 };
	final public static int[] _PapayaWhip = { 255, 239, 213 };
	final public static int[] _Moccasin = { 255, 228, 181 };
	final public static int[] _PeachPuff = { 255, 218, 185 };
	final public static int[] _PaleGoldenrod = { 238, 232, 170 };
	final public static int[] _Khaki = { 240, 230, 140 };
	final public static int[] _DarkKhaki = { 189, 183, 107 };
	final public static int[][] _Yellows = { _Yellow, _Gold, _LightYellow, _LemonChiffon, _LightGoldenrodYellow,
			_PapayaWhip, _Moccasin, _PeachPuff, _PaleGoldenrod, _Khaki, _DarkKhaki };

	final public static int[] _Lavender = { 230, 230, 250 };
	final public static int[] _Thistle = { 216, 191, 216 };
	final public static int[] _Plum = { 221, 160, 221 };
	final public static int[] _Violet = { 238, 130, 238 };
	final public static int[] _Orchid = { 218, 112, 214 };
	final public static int[] _Magenta = { 255, 0, 255 };
	final public static int[] _MediumOrchid = { 186, 85, 211 };
	final public static int[] _MediumPurple = { 147, 112, 219 };
	final public static int[] _RebeccaPurple = { 102, 51, 153 };
	final public static int[] _BlueViolet = { 138, 43, 226 };
	final public static int[] _DarkViolet = { 148, 0, 211 };
	final public static int[] _DarkOrchid = { 153, 50, 204 };
	final public static int[] _DarkMagenta = { 139, 0, 139 };
	final public static int[] _Purple = { 128, 0, 128 };
	final public static int[] _Indigo = { 75, 0, 130 };
	final public static int[] _SlateBlue = { 106, 90, 205 };
	final public static int[] _DarkSlateBlue = { 72, 61, 139 };
	final public static int[] _MediumSlateBlue = { 123, 104, 238 };
	final public static int[][] _Purples = { _Magenta, _Purple, _Lavender, _Thistle, _Plum, _Violet, _Orchid,
			_MediumOrchid, _MediumPurple, _RebeccaPurple, _BlueViolet, _DarkViolet, _DarkOrchid, _DarkMagenta, _Indigo,
			_SlateBlue, _DarkSlateBlue, _MediumSlateBlue };

	final public static int[] _GreenYellow = { 173, 255, 47 };
	final public static int[] _Chartreuse = { 127, 255, 0 };
	final public static int[] _Lime = { 0, 255, 0 };
	final public static int[] _LimeGreen = { 50, 205, 50 };
	final public static int[] _PaleGreen = { 152, 251, 152 };
	final public static int[] _LightGreen = { 144, 238, 144 };
	final public static int[] _MediumSpringGreen = { 0, 250, 154 };
	final public static int[] _SpringGreen = { 0, 255, 127 };
	final public static int[] _MediumSeaGreen = { 60, 179, 113 };
	final public static int[] _SeaGreen = { 46, 139, 87 };
	final public static int[] _ForestGreen = { 34, 139, 34 };
	final public static int[] _Green = { 0, 128, 0 };
	final public static int[] _DarkGreen = { 0, 100, 0 };
	final public static int[] _YellowGreen = { 154, 205, 50 };
	final public static int[] _OliveDrab = { 107, 142, 35 };
	final public static int[] _Olive = { 128, 128, 0 };
	final public static int[] _DarkOliveGreen = { 85, 107, 47 };
	final public static int[] _MediumAquamarine = { 102, 205, 170 };
	final public static int[] _DarkSeaGreen = { 143, 188, 139 };
	final public static int[] _LightSeaGreen = { 32, 178, 170 };
	final public static int[] _DarkCyan = { 0, 139, 139 };
	final public static int[] _Teal = { 0, 128, 128 };
	final public static int[][] _Greens = { _Lime, _Green, _GreenYellow, _Chartreuse, _LimeGreen, _PaleGreen,
			_LightGreen, _MediumSpringGreen, _SpringGreen, _MediumSeaGreen, _SeaGreen, _ForestGreen, _DarkGreen,
			_YellowGreen, _OliveDrab, _Olive, _DarkOliveGreen, _MediumAquamarine, _DarkSeaGreen, _LightSeaGreen,
			_DarkCyan, _Teal };

	final public static int[] _Cyan = { 0, 255, 255 };
	final public static int[] _LightCyan = { 224, 255, 255 };
	final public static int[] _PaleTurquoise = { 175, 238, 238 };
	final public static int[] _Aquamarine = { 127, 255, 212 };
	final public static int[] _Turquoise = { 64, 224, 208 };
	final public static int[] _MediumTurquoise = { 72, 209, 204 };
	final public static int[] _DarkTurquoise = { 0, 206, 209 };
	final public static int[] _CadetBlue = { 95, 158, 160 };
	final public static int[] _SteelBlue = { 70, 130, 180 };
	final public static int[] _LightSteelBlue = { 176, 196, 222 };
	final public static int[] _PowderBlue = { 176, 224, 230 };
	final public static int[] _LightBlue = { 173, 216, 230 };
	final public static int[] _SkyBlue = { 135, 206, 235 };
	final public static int[] _LightSkyBlue = { 135, 206, 250 };
	final public static int[] _DeepSkyBlue = { 0, 191, 255 };
	final public static int[] _DodgerBlue = { 30, 144, 255 };
	final public static int[] _CornflowerBlue = { 100, 149, 237 };
	final public static int[] _RoyalBlue = { 65, 105, 225 };
	final public static int[] _Blue = { 0, 0, 255 };
	final public static int[] _MediumBlue = { 0, 0, 205 };
	final public static int[] _DarkBlue = { 0, 0, 139 };
	final public static int[] _Navy = { 0, 0, 128 };
	final public static int[] _MidnightBlue = { 25, 25, 112 };
	final public static int[][] _Blues = { _Cyan, _LightCyan, _PaleTurquoise, _Aquamarine, _Turquoise, _MediumTurquoise,
			_DarkTurquoise, _CadetBlue, _SteelBlue, _LightSteelBlue, _PowderBlue, _LightBlue, _SkyBlue, _LightSkyBlue,
			_DeepSkyBlue, _DodgerBlue, _CornflowerBlue, _RoyalBlue, _Blue, _MediumBlue, _DarkBlue, _Navy,
			_MidnightBlue };

	final public static int[] _Cornsilk = { 255, 248, 220 };
	final public static int[] _BlanchedAlmond = { 255, 235, 205 };
	final public static int[] _Bisque = { 255, 228, 196 };
	final public static int[] _NavajoWhite = { 255, 222, 173 };
	final public static int[] _Wheat = { 245, 222, 179 };
	final public static int[] _BurlyWood = { 222, 184, 135 };
	final public static int[] _Tan = { 210, 180, 140 };
	final public static int[] _RosyBrown = { 188, 143, 143 };
	final public static int[] _SandyBrown = { 244, 164, 96 };
	final public static int[] _Goldenrod = { 218, 165, 32 };
	final public static int[] _DarkGoldenrod = { 184, 134, 11 };
	final public static int[] _Peru = { 205, 133, 63 };
	final public static int[] _Chocolate = { 210, 105, 30 };
	final public static int[] _SaddleBrown = { 139, 69, 19 };
	final public static int[] _Sienna = { 160, 82, 45 };
	final public static int[] _Brown = { 165, 42, 42 };
	final public static int[] _Maroon = { 128, 0, 0 };
	final public static int[][] _Browns = { _Brown, _Cornsilk, _BlanchedAlmond, _Bisque, _NavajoWhite, _Wheat,
			_BurlyWood, _Tan, _RosyBrown, _SandyBrown, _Goldenrod, _DarkGoldenrod, _Peru, _Chocolate, _SaddleBrown,
			_Sienna, _Maroon };

	final public static int[] _White = { 255, 255, 255 };
	final public static int[] _Snow = { 255, 250, 250 };
	final public static int[] _HoneyDew = { 240, 255, 240 };
	final public static int[] _MintCream = { 245, 255, 250 };
	final public static int[] _Azure = { 240, 255, 255 };
	final public static int[] _AliceBlue = { 240, 248, 255 };
	final public static int[] _GhostWhite = { 248, 248, 255 };
	final public static int[] _WhiteSmoke = { 245, 245, 245 };
	final public static int[] _SeaShell = { 255, 245, 238 };
	final public static int[] _Beige = { 245, 245, 220 };
	final public static int[] _OldLace = { 253, 245, 230 };
	final public static int[] _FloralWhite = { 255, 250, 240 };
	final public static int[] _Ivory = { 255, 255, 240 };
	final public static int[] _AntiqueWhite = { 250, 235, 215 };
	final public static int[] _Linen = { 250, 240, 230 };
	final public static int[] _LavenderBlush = { 255, 240, 245 };
	final public static int[] _MistyRose = { 255, 228, 225 };
	final public static int[][] _Whites = { _White, _Snow, _HoneyDew, _MintCream, _Azure, _AliceBlue, _GhostWhite,
			_WhiteSmoke, _SeaShell, _Beige, _OldLace, _FloralWhite, _Ivory, _AntiqueWhite, _Linen, _LavenderBlush,
			_MistyRose };

	final public static int[] _Gainsboro = { 220, 220, 220 };
	final public static int[] _LightGray = { 211, 211, 211 };
	final public static int[] _Silver = { 192, 192, 192 };
	final public static int[] _DarkGray = { 169, 169, 169 };
	final public static int[] _Gray = { 128, 128, 128 };
	final public static int[] _DimGray = { 105, 105, 105 };
	final public static int[] _LightSlateGray = { 119, 136, 153 };
	final public static int[] _SlateGray = { 112, 128, 144 };
	final public static int[] _DarkSlateGray = { 47, 79, 79 };
	final public static int[] _Black = { 0, 0, 0 };
	final public static int[][] _Grays = { _Gray, _Gainsboro, _LightGray, _Silver, _DarkGray, _DimGray, _LightSlateGray,
			_SlateGray, _DarkSlateGray, _Black, };

	final private static TreeMap<String, int[]> _NameToRgb;
	final private static TreeMap<int[], String> _RgbToName;
	final private static TreeMap<String, String> _NameToNormalizedName;
	final private static String[] _ForegroundColorNames;

	final private static ColorGroup[] _ColorGroups;
	static {
		_NameToRgb = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		_RgbToName = new TreeMap<>((rgb0, rgb1) -> {
			final int min0 = Math.min(rgb0[0], Math.min(rgb0[1], rgb0[2]));
			final int min1 = Math.min(rgb1[0], Math.min(rgb1[1], rgb1[2]));
			if (min0 != min1) {
				return min0 > min1 ? -1 : 1;
			}
			return CombinatoricTools._ByAllInOrder.compare(rgb0, rgb1);
		});
		_NameToNormalizedName = new TreeMap<>(_NameToRgb.comparator());
		@SuppressWarnings("rawtypes")
		final Class myClass = StaticUtilities.getMyClass();
		final Field[] fields = myClass.getDeclaredFields();
		for (final Field field : fields) {
			final String fieldName = field.getName();
			if (fieldName.charAt(0) == '_') {
				try {
					final Object o = field.get(null);
					if (o instanceof final int[] rgb) {
						if (rgb.length == 3) {
							final String lookUpName = fieldName.substring(1);
							/**
							 * Put it in the two maps. Then, whether it's a duplicate Rgb or not, record the
							 * "normalized name" for this name.
							 */
							final String oldName = _RgbToName.get(rgb);
							if (oldName == null) {
								_NameToRgb.put(lookUpName, rgb);
								_NameToNormalizedName.put(lookUpName, lookUpName);
								_RgbToName.put(rgb, lookUpName);
							} else {
								_NameToNormalizedName.put(lookUpName, oldName);
							}
						}
					}
				} catch (final IllegalArgumentException | IllegalAccessException e) {
					/** Won't worry about it. */
				}
			}
		}
		_BackgroundRgb = _NameToRgb.get(_BackgroundColorName);
		_ColorBackgroundLum = getLuminance(_BackgroundRgb);
		_ColorBackgroundH = rgbToHsb(_BackgroundRgb)[0];
		final int nColors = _RgbToName.size();

		/** Gather each ColorGroup and sort each one internally. */
		final int nGroups = _ColorGroupings.length;
		final TreeMap<ColorGrouping, ColorGroup> colorGroupsMap = new TreeMap<>();
		for (final Field field0 : fields) {
			final String fieldName0 = field0.getName();
			if (fieldName0.charAt(0) == '_') {
				try {
					final Object o = field0.get(null);
					if (o instanceof final int[][] group) {
						final int nInGroup = group.length;
						if (nInGroup > 0) {
							final String[] memberNames = new String[nInGroup];
							for (int k = 0; k < nInGroup; ++k) {
								memberNames[k] = _RgbToName.get(group[k]);
							}
							final ColorGrouping colorGrouping = groupNameToColorGrouping(fieldName0.substring(1));
							colorGroupsMap.put(colorGrouping, new ColorGroup(colorGrouping, memberNames));
						}
					}
				} catch (final IllegalArgumentException | IllegalAccessException e) {
				}
			}
		}
		_ColorGroups = new ColorGroup[nGroups];
		for (int k = 0; k < nGroups; ++k) {
			_ColorGroups[k] = colorGroupsMap.get(_ColorGroupings[k]);
		}

		final ArrayList<String> inNoGroup = new ArrayList<>();
		NEXT_COLOR: for (final String name : _NameToRgb.keySet()) {
			for (final ColorGroup group : _ColorGroups) {
				for (final String memberName : group._memberNames) {
					if (memberName.equalsIgnoreCase(name)) {
						continue NEXT_COLOR;
					}
				}
			}
			System.out.printf("\nIn No Group: %s", name);
			inNoGroup.add(name);
		}
		/**
		 * Each ColorGroup is sorted. We rotate through them to get
		 * _ForegroundColorNames.
		 */
		_ForegroundColorNames = new String[nColors];
		final int[] idxS = new int[nGroups];
		Arrays.fill(idxS, 0);
		OUTSIDE: for (int inNoGroupIdx = 0, kGroup = 0, k0 = 0; k0 < nColors; ++k0) {
			for (int nGroupsTried = 0; nGroupsTried < nGroups; kGroup = (kGroup + 1) % nGroups, ++nGroupsTried) {
				final ColorGroup colorGroup = _ColorGroups[kGroup];
				final int idx = idxS[kGroup];
				if (idx < colorGroup._nameAndRgbs.length) {
					final NameAndRgb nameAndRgb = colorGroup._nameAndRgbs[idx];
					_ForegroundColorNames[k0] = nameAndRgb._name;
					++idxS[kGroup];
					kGroup = (kGroup + 1) % nGroups;
					continue OUTSIDE;
				}
			}
			_ForegroundColorNames[k0] = inNoGroup.get(inNoGroupIdx++);
		}

	}

	public static String[] getMemberColorNames(final ColorGrouping colorGrouping) {
		return _ColorGroups[colorGrouping.ordinal()]._memberNames;
	}

	public static String[] getSampleFromEachGroup() {
		final int nGroups = _ColorGroupings.length;
		final String[] returnValue = new String[nGroups];
		for (int k = 0; k < nGroups; ++k) {
			final ColorGroup colorGroup = _ColorGroups[k];
			returnValue[k] = colorGroup._memberNames[0];
		}
		return returnValue;
	}

	public static int[] getBackgroundRgb(final String colorName) {
		final double luminance = getLuminance(_NameToRgb.get(normalizeColorName(colorName)));
		return luminance < 0.5 ? _White : _Black;
	}

	/***
	 * <pre>
	 * We use the NTSC formula: Luminance = .299*r + .587*g + .114*b. White is
	 * (uniquely) 255.0 and Black is (uniquely) 0.0
	 */
	public static double getLuminance(final int[] rgb) {
		return (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 256d;
	}

	public static String bestMatchToHsb(final int[] rgb) {
		final float[] hsbvals0 = new float[3];
		java.awt.Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsbvals0);
		double bestD = Double.MAX_VALUE;
		String winner = null;
		for (final Map.Entry<String, int[]> entry : _NameToRgb.entrySet()) {
			final float[] hsbvals1 = new float[3];
			final int[] rgb1 = entry.getValue();
			java.awt.Color.RGBtoHSB(rgb1[0], rgb1[1], rgb1[2], hsbvals1);
			final double d = Math.abs(hsbvals1[0] - hsbvals0[0]) + Math.abs(hsbvals1[1] - hsbvals0[1])
					+ Math.abs(hsbvals1[2] - hsbvals0[2]);
			if (winner == null || d < bestD) {
				bestD = d;
				winner = entry.getKey();
			}
		}
		return winner;
	}

	public static String bestMatchToRgb(final int[] rgb) {
		double bestD = Double.MAX_VALUE;
		String winner = null;
		for (final Map.Entry<String, int[]> entry : _NameToRgb.entrySet()) {
			final int[] rgb1 = entry.getValue();
			final double d = Math.abs(rgb1[0] - rgb[0]) + Math.abs(rgb1[1] - rgb[1]) + Math.abs(rgb1[2] - rgb[2]);
			if (winner == null || d < bestD) {
				bestD = d;
				winner = entry.getKey();
			}
		}
		return winner;
	}

	public static double getHsbCloseness(final int[] rgb0, final int[] rgb1) {
		final float[] hsb0 = new float[3];
		java.awt.Color.RGBtoHSB(rgb0[0], rgb0[1], rgb0[2], hsb0);
		final float[] hsb1 = new float[3];
		java.awt.Color.RGBtoHSB(rgb1[0], rgb1[1], rgb1[2], hsb1);
		return Math.abs(hsb1[0] - hsb0[0]) + Math.abs(hsb1[1] - hsb0[1]) + Math.abs(hsb1[2] - hsb0[2]);
	}

	public static Set<String> getAllColorNames() {
		return _NameToRgb.keySet();
	}

	public static String normalizeColorName(final String colorName) {
		return _NameToNormalizedName.get(colorName);
	}

	public static String rgbToName(final int[] rgb) {
		return _RgbToName.get(rgb);
	}

	public static int[] normalizeRgb(final int[] rgb) {
		final String name = _RgbToName.get(rgb);
		return name == null ? rgb : _NameToRgb.get(name);
	}

	public static String getBackgroundColorName() {
		return _BackgroundColorName;
	}

	public static int[] nameToRgb(final String name) {
		return _NameToRgb.get(name);
	}

	public static int[] getGroupRgb(final ColorGrouping colorGrouping) {
		return _ColorGroups[colorGrouping.ordinal()]._nameAndRgbs[0]._rgb;
	}

	public static ColorGroup getColorGroup(final ColorGrouping colorGrouping) {
		return _ColorGroups[colorGrouping.ordinal()];
	}

	/**
	 * brightValue can go from -1 to 1. -1 dims to black, 0 leaves it alone, and 1
	 * raises the b of hsb to 1.
	 */
	public static int[] mixInBright(final int[] rgb, double brightValue) {
		brightValue = Math.max(-1d, Math.min(brightValue, 1d));
		final float[] hsb = rgbToHsb(rgb);
		final float oldB = hsb[2];
		final double newB;
		if (brightValue <= 0d) {
			newB = (1d + brightValue) * oldB;
		} else {
			newB = oldB + brightValue * (1d - oldB);
		}
		hsb[2] = (float) newB;
		return hsbToRgb(hsb);
	}

	/** {@link http://www.exampledepot.com/egs/java.awt/color_Hsb.html}. */
	public static float[] rgbToHsb(final int[] rgb) {
		return java.awt.Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null);
	}

	public static int[] hsbToRgb(final float[] hsb) {
		final int rgbInt = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
		final int r = (rgbInt >> 16) & 0xFF;
		final int g = (rgbInt >> 8) & 0xFF;
		final int b = rgbInt & 0xFF;
		return new int[] { r, g, b };
	}

	public static String[] getForegroundColorNames() {
		return _ForegroundColorNames;
	}

	public static String getColorName(final ColorGrouping colorGrouping, final int idx) {
		return _ColorGroups[colorGrouping.ordinal()].getColorName(idx);
	}

	public static String getForegroundColorName(final int i) {
		return _ForegroundColorNames[Math.abs(i) % _ForegroundColorNames.length];
	}

	static int[] oldHsbInterpolate(final int[] lowRgb, final int[] middleRgb, final int[] highRgb, final int i,
			final int nColors) {
		if (nColors == 1) {
			return middleRgb.clone();
		}
		final int[] rgb0, rgb1;
		final float p;
		if (2 * i < nColors - 1) {
			rgb0 = lowRgb;
			rgb1 = middleRgb;
			p = 2.0f * i / (nColors - 1.0f);
		} else {
			rgb0 = middleRgb;
			rgb1 = highRgb;
			p = 2.0f * i / (nColors - 1.0f) - 1.0f;
		}
		final float[] hsb0 = rgbToHsb(rgb0);
		final float[] hsb1 = rgbToHsb(rgb1);
		final float[] hsb = new float[3];
		for (int k = 0; k < 3; ++k) {
			hsb[k] = (1.0f - p) * hsb0[k] + p * hsb1[k];
		}
		return hsbToRgb(hsb);
	}

	@SuppressWarnings("unused")
	public static void main(final String[] args) {
		final float[] redHsb = rgbToHsb(_Red);
		final float[] greenHsb = rgbToHsb(_Lime);
		final float[] blueHsb = rgbToHsb(_Blue);
		final float[] whiteHsb = rgbToHsb(_White);
		final float[] blackHsb = rgbToHsb(_Black);
		final int x = 0;
	}

}
