package com.skagit.utils;

import com.skagit.utils.lsFormatter.LsFormatter;
import com.skagit.utils.navigation.LatLngArrayStatics;

public class Constants implements Constants0 {
	public static void main(final String[] args) {
		final double lowLat = 45d, highLat = 50;
		final double leftLng = 0d, rightLng = 90d;
		final double lngDiff = LatLngArrayStatics.degsToEast0_360L(leftLng, rightLng);
		final double lngProportion = lngDiff / 360d;
		final double sin1 = MathX0.sinX(Math.toRadians(highLat));
		final double sin0 = MathX0.sinX(Math.toRadians(lowLat));
		final double sqEarthRadii = _2Pi * (sin1 - sin0) * lngProportion;
		final double sqNmiCalculus = sqEarthRadii / (_NmiToRAuthalic * _NmiToRAuthalic);
		System.out.printf("\nCalculusArea[%.4f]", sqNmiCalculus);
		for (int nInsideLngs = 0; nInsideLngs < lngDiff * 240; nInsideLngs = Math.max(nInsideLngs + 1,
				nInsideLngs * 2)) {
			final double[] insideLngs = new double[nInsideLngs];
			final double deltaLng = lngDiff / (nInsideLngs + 1d);
			for (int k = 0; k < nInsideLngs; ++k) {
				insideLngs[k] = leftLng + (k + 1) * deltaLng;
			}
			final LatLng3[] latLngArray = new LatLng3[4 + 2 * nInsideLngs];
			int kLatLng = 0;
			latLngArray[kLatLng++] = LatLng3.getLatLngB(lowLat, leftLng);
			for (int k = 0; k < nInsideLngs; ++k) {
				latLngArray[kLatLng++] = LatLng3.getLatLngB(lowLat, insideLngs[k]);
			}
			latLngArray[kLatLng++] = LatLng3.getLatLngB(lowLat, rightLng);
			latLngArray[kLatLng++] = LatLng3.getLatLngB(highLat, rightLng);
			for (int k = 0; k < nInsideLngs; ++k) {
				latLngArray[kLatLng++] = LatLng3.getLatLngB(highLat, insideLngs[nInsideLngs - 1 - k]);
			}
			latLngArray[kLatLng++] = LatLng3.getLatLngB(highLat, leftLng);
			final int flag = Loop3Statics.createGenericFlag(/* isClockwise= */false);
			final Loop3 loop = Loop3.getLoop(/* logger= */null, /* id= */0, /* subId= */0, /* flag= */flag,
					/* ancestorId= */-1, latLngArray, /* logChanges= */false, /* debug= */false);
			final double sqNmi = loop.getSqNmi();
			System.out.printf("\nnInsideLngs[%d] DeltLng[%s] Area[%s], RelErr[%s]", nInsideLngs,
					LsFormatter.StandardFormatForLatOrLng(deltaLng), LsFormatter.StandardFormat(sqNmi),
					LsFormatter.StandardFormat(Math.abs(sqNmi - sqNmiCalculus) / sqNmiCalculus));
		}
	}

}
