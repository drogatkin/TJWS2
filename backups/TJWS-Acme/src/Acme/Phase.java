// Phase - phase of the moon calculations
//
// Adapted from "moontool.c" by John Walker, Release 2.0.
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights
// reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package Acme;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/// Phase - phase of the moon calculations
// <P>
// Adapted from "moontool.c" by John Walker, Release 2.0.
// <P>
// <A HREF="/resources/classes/Acme/Phase.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Phase {
	
	// Astronomical constants.
	
	// 1980 January 0.0
	private static final double epoch = 2444238.5D;
	
	// Constants defining the Sun's apparent orbit.
	
	// ecliptic longitude of the Sun at epoch 1980.0
	private static final double elonge = 278.833540D;
	
	// ecliptic longitude of the Sun at perigee
	private static final double elongp = 282.596403D;
	
	// eccentricity of Earth's orbit
	private static final double eccent = 0.016718D;
	
	// semi-major axis of Earth's orbit, km
	private static final double sunsmax = 1.495985e8D;
	
	// sun's angular size, degrees, at semi-major axis distance
	private static final double sunangsiz = 0.533128D;
	
	// Elements of the Moon's orbit, epoch 1980.0.
	
	// moon's mean lonigitude at the epoch
	private static final double mmlong = 64.975464D;
	
	// mean longitude of the perigee at the epoch
	private static final double mmlongp = 349.383063D;
	
	// mean longitude of the node at the epoch
	private static final double mlnode = 151.950429D;
	
	// inclination of the Moon's orbit
	private static final double minc = 5.145396D;
	
	// eccentricity of the Moon's orbit
	private static final double mecc = 0.054900D;
	
	// moon's angular size at distance a from Earth
	private static final double mangsiz = 0.5181D;
	
	// semi-major axis of Moon's orbit in km
	private static final double msmax = 384401.0D;
	
	// parallax at distance a from Earth
	private static final double mparallax = 0.9507D;
	
	// synodic month (new Moon to new Moon)
	private static final double synmonth = 29.53058868D;
	
	// base date for E. W. Brown's numbered series of lunations (1923 Jan 16)
	private static final double lunatbase = 2423436.0D;
	
	// Properties of the Earth.
	
	// radius of Earth in kilometres
	private static final double earthrad = 6378.16D;
	
	// Mathematical constants.
	private static final double EPSILON = 1E-6D;
	
	// Handy mathematical functions.
	
	// Fix angle.
	private static double fixangle(double a) {
		double b = a - 360.0 * Math.floor(a / 360.0D);
		// Can't use Math.IEEEremainder here because remainder differs
		// from modulus for negative numbers.
		return b;
	}
	
	// Degrees to radians.
	private static double torad(double d) {
		return d * Math.PI / 180.0D;
	}
	
	// Radians to degrees.
	private static double todeg(double r) {
		return r * 180.0D / Math.PI;
	}
	
	// Sin from degrees.
	private static double dsin(double d) {
		return Math.sin(torad(d));
	}
	
	// Cos from degrees.
	private static double dcos(double d) {
		return Math.cos(torad(d));
	}
	
	// jdate - convert internal GMT date to Julian day.
	private static long jdate(Date t) {
		long c, m, y;
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(t);
		y = cal.get(Calendar.YEAR) + 1900;
		m = cal.get(Calendar.MONTH) + 1;
		if(m > 2)
			m = m - 3;
		else {
			m = m + 9;
			--y;
		}
		c = y / 100L;		// compute century
		y -= 100L * c;
		return cal.get(Calendar.DATE) + (c * 146097L) / 4 + (y * 1461L) / 4 + (m * 153L + 2) / 5 + 1721119L;
	}
	
	/// Convert internal date and time to astronomical Julian
	// time (i.e. Julian date plus day fraction, expressed as
	// a double).
	public static double jtime(Date t) {
		int c;
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(t);
		c = -cal.get(Calendar.ZONE_OFFSET);	// !!! should this be negative?
		return (jdate(t) - 0.5) + (cal.get(Calendar.SECOND) + 60 * (cal.get(Calendar.MINUTE) + c + 60 * cal.get(Calendar.HOUR_OF_DAY))) / 86400.0;
	}
	
	// jyear - convert Julian date to year, month, day, which are
	// returned via integer pointers to integers
	private static void jyear(double td, RefInt yy, RefInt mm, RefInt dd) {
		double j, d, y, m;
		
		td += 0.5;	// astronomical to civil
		j = Math.floor(td);
		j = j - 1721119.0;
		y = Math.floor(((4 * j) - 1) / 146097.0);
		j = (j * 4.0) - (1.0 + (146097.0 * y));
		d = Math.floor(j / 4.0);
		j = Math.floor(((4.0 * d) + 3.0) / 1461.0);
		d = ((4.0 * d) + 3.0) - (1461.0 * j);
		d = Math.floor((d + 4.0) / 4.0);
		m = Math.floor(((5.0 * d) - 3) / 153.0);
		d = (5.0 * d) - (3.0 + (153.0 * m));
		d = Math.floor((d + 5.0) / 5.0);
		y = (100.0 * y) + j;
		if(m < 10.0)
			m = m + 3;
		else {
			m = m - 9;
			y = y + 1;
		}
		yy.val = (int) y;
		mm.val = (int) m;
		dd.val = (int) d;
	}
	
	// meanphase - calculates mean phase of the Moon for a given base date
	// and desired phase:
	// 0.0 New Moon
	// 0.25 First quarter
	// 0.5 Full moon
	// 0.75 Last quarter
	// Beware!!! This routine returns meaningless results for any other
	// phase arguments. Don't attempt to generalise it without understanding
	// that the motion of the moon is far more complicated that this
	// calculation reveals.
	private static double meanphase(double sdate, double phase, RefDouble usek) {
		RefInt yy = new RefInt();
		RefInt mm = new RefInt();
		RefInt dd = new RefInt();
		double k, t, t2, t3, nt1;
		
		jyear(sdate, yy, mm, dd);
		
		k = (yy.val + ((mm.val - 1) * (1.0 / 12.0)) - 1900) * 12.3685;
		
		// Time in Julian centuries from 1900 January 0.5.
		t = (sdate - 2415020.0) / 36525;
		t2 = t * t;		   // square for frequent use
		t3 = t2 * t;		   // cube for frequent use
		
		usek.val = k = Math.floor(k) + phase;
		nt1 = 2415020.75933 + synmonth * k + 0.0001178 * t2 - 0.000000155 * t3 + 0.00033 * dsin(166.56 + 132.87 * t - 0.009173 * t2);
		
		return nt1;
	}
	
	// truephase - given a K value used to determine the mean phase of the
	// new moon, and a phase selector (0.0, 0.25, 0.5, 0.75),
	// obtain the true, corrected phase time
	private static double truephase(double k, double phase) {
		double t, t2, t3, pt, m, mprime, f;
		boolean apcor = false;
		
		k += phase; /* add phase to new moon time */
		t = k / 1236.85; /*
							 * time in Julian centuries from
							 * 1900 January 0.5
							 */
		t2 = t * t; /* square for frequent use */
		t3 = t2 * t; /* cube for frequent use */
		pt = 2415020.75933 /* mean time of phase */
				+ synmonth * k + 0.0001178 * t2 - 0.000000155 * t3 + 0.00033 * dsin(166.56 + 132.87 * t - 0.009173 * t2);
		
		m = 359.2242 /* Sun's mean anomaly */
				+ 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
		mprime = 306.0253 /* Moon's mean anomaly */
				+ 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
		f = 21.2964 /* Moon's argument of latitude */
				+ 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
		if((phase < 0.01) || (Math.abs(phase - 0.5) < 0.01)) {
			/* Corrections for New and Full Moon. */
			pt += (0.1734 - 0.000393 * t) * dsin(m) + 0.0021 * dsin(2 * m) - 0.4068 * dsin(mprime) + 0.0161 * dsin(2 * mprime) - 0.0004 * dsin(3 * mprime) + 0.0104 * dsin(2 * f) - 0.0051 * dsin(m + mprime) - 0.0074 * dsin(m - mprime) + 0.0004 * dsin(2 * f + m) - 0.0004 * dsin(2 * f - m)
					- 0.0006 * dsin(2 * f + mprime) + 0.0010 * dsin(2 * f - mprime) + 0.0005 * dsin(m + 2 * mprime);
			apcor = true;
		} else if((Math.abs(phase - 0.25) < 0.01 || (Math.abs(phase - 0.75) < 0.01))) {
			pt += (0.1721 - 0.0004 * t) * dsin(m) + 0.0021 * dsin(2 * m) - 0.6280 * dsin(mprime) + 0.0089 * dsin(2 * mprime) - 0.0004 * dsin(3 * mprime) + 0.0079 * dsin(2 * f) - 0.0119 * dsin(m + mprime) - 0.0047 * dsin(m - mprime) + 0.0003 * dsin(2 * f + m) - 0.0004 * dsin(2 * f - m)
					- 0.0006 * dsin(2 * f + mprime) + 0.0021 * dsin(2 * f - mprime) + 0.0003 * dsin(m + 2 * mprime) + 0.0004 * dsin(m - 2 * mprime) - 0.0003 * dsin(2 * m + mprime);
			if(phase < 0.5)
				/* First quarter correction. */
				pt += 0.0028 - 0.0004 * dcos(m) + 0.0003 * dcos(mprime);
			else
				/* Last quarter correction. */
				pt += -0.0028 + 0.0004 * dcos(m) - 0.0003 * dcos(mprime);
			apcor = true;
		}
		if(!apcor)
			throw new InternalError("Acme.Phase.truephase() called with invalid phase selector");
		return pt;
	}
	
	/// Find time of phases of the moon which surround the current
	// date. Five phases are found, starting and ending with the
	// new moons which bound the current lunation.
	public static void phasehunt5(double sdate, double[] phases) {
		double adate, nt1, nt2;
		RefDouble k1 = new RefDouble();
		RefDouble k2 = new RefDouble();
		
		adate = sdate - 45;
		nt1 = meanphase(adate, 0.0, k1);
		for(;;) {
			adate += synmonth;
			nt2 = meanphase(adate, 0.0, k2);
			if(nt1 <= sdate && nt2 > sdate)
				break;
			nt1 = nt2;
			k1.val = k2.val;
		}
		phases[0] = truephase(k1.val, 0.0);
		phases[1] = truephase(k1.val, 0.25);
		phases[2] = truephase(k1.val, 0.5);
		phases[3] = truephase(k1.val, 0.75);
		phases[4] = truephase(k2.val, 0.0);
	}
	
	// phasehunt2 - find time of phases of the moon which surround the current
	// date. Two phases are found.
	void phasehunt2(double sdate, double[] phases, double[] which) {
		double adate, nt1, nt2;
		RefDouble k1 = new RefDouble();
		RefDouble k2 = new RefDouble();
		
		adate = sdate - 45;
		nt1 = meanphase(adate, 0.0, k1);
		for(;;) {
			adate += synmonth;
			nt2 = meanphase(adate, 0.0, k2);
			if(nt1 <= sdate && nt2 > sdate)
				break;
			nt1 = nt2;
			k1.val = k2.val;
		}
		phases[0] = truephase(k1.val, 0.0);
		which[0] = 0.0;
		phases[1] = truephase(k1.val, 0.25);
		which[1] = 0.25;
		if(phases[1] <= sdate) {
			phases[0] = phases[1];
			which[0] = which[1];
			phases[1] = truephase(k1.val, 0.5);
			which[1] = 0.5;
			if(phases[1] <= sdate) {
				phases[0] = phases[1];
				which[0] = which[1];
				phases[1] = truephase(k1.val, 0.75);
				which[1] = 0.75;
				if(phases[1] <= sdate) {
					phases[0] = phases[1];
					which[0] = which[1];
					phases[1] = truephase(k2.val, 0.0);
					which[1] = 0.0;
				}
			}
		}
	}
	
	// kepler - solve the equation of Kepler
	private static double kepler(double m, double ecc) {
		double e, delta;
		
		e = m = torad(m);
		do {
			delta = e - ecc * Math.sin(e) - m;
			e -= delta / (1 - ecc * Math.cos(e));
		} while(Math.abs(delta) > EPSILON);
		return e;
	}
	
	/// Calculate phase of moon as a fraction.
	// <P>
	// @param pdate time for which the phase is requested, as from jtime()
	// @param pphaseR Ref for illuminated fraction of Moon's disk
	// @param mageR Ref for age of moon in days
	// @param distR Ref for distance in km from center of Earth
	// @param angdiaR Ref for angular diameter in degrees as seen from Earth
	// @param sudistR Ref for distance in km to Sun
	// @param suangdiaR Ref for Sun's angular diameter
	// @return terminator phase angle as a fraction of a full circle (i.e., 0 to
	/// 1)
	//
	public static double phase(double pdate, RefDouble pphaseR, RefDouble mageR, RefDouble distR, RefDouble angdiaR, RefDouble sudistR, RefDouble suangdiaR) {
		double Day, N, M, Ec, Lambdasun, ml, MM, Ev, Ae, A3, MmP, mEc, A4, lP, V, lPP, MoonAge, MoonPhase, MoonDist, MoonDFrac, MoonAng, F, SunDist, SunAng;
		
		// Calculation of the Sun's position.
		
		Day = pdate - epoch;			// date within epoch
		N = fixangle((360 / 365.2422) * Day);	// mean anomaly of the Sun
		M = fixangle(N + elonge - elongp);	// convert from perigee co-ordinates
												// to epoch 1980.0
		Ec = kepler(M, eccent);			// solve equation of Kepler
		Ec = Math.sqrt((1 + eccent) / (1 - eccent)) * Math.tan(Ec / 2);
		Ec = 2 * todeg(Math.atan(Ec));		// true anomaly
		Lambdasun = fixangle(Ec + elongp);	// Sun's geocentric ecliptic
												// longitude
		// Orbital distance factor.
		F = ((1 + eccent * Math.cos(torad(Ec))) / (1 - eccent * eccent));
		SunDist = sunsmax / F;			// distance to Sun in km
		SunAng = F * sunangsiz;			// Sun's angular size in degrees
		
		// Calculation of the Moon's position.
		
		// Moon's mean longitude.
		ml = fixangle(13.1763966 * Day + mmlong);
		
		// Moon's mean anomaly.
		MM = fixangle(ml - 0.1114041 * Day - mmlongp);
		
		// Evection.
		Ev = 1.2739 * Math.sin(torad(2 * (ml - Lambdasun) - MM));
		
		// Annual equation.
		Ae = 0.1858 * Math.sin(torad(M));
		
		// Correction term.
		A3 = 0.37 * Math.sin(torad(M));
		
		// Corrected anomaly.
		MmP = MM + Ev - Ae - A3;
		
		// Correction for the equation of the centre.
		mEc = 6.2886 * Math.sin(torad(MmP));
		
		// Another correction term.
		A4 = 0.214 * Math.sin(torad(2 * MmP));
		
		// Corrected longitude.
		lP = ml + Ev + mEc - Ae + A4;
		
		// Variation.
		V = 0.6583 * Math.sin(torad(2 * (lP - Lambdasun)));
		
		// True longitude.
		lPP = lP + V;
		
		// Calculation of the phase of the Moon.
		
		// Age of the Moon in degrees.
		MoonAge = lPP - Lambdasun;
		
		// Phase of the Moon.
		MoonPhase = (1 - Math.cos(torad(MoonAge))) / 2;
		
		// Calculate distance of moon from the centre of the Earth.
		
		MoonDist = (msmax * (1 - mecc * mecc)) / (1 + mecc * Math.cos(torad(MmP + mEc)));
		
		// Calculate Moon's angular diameter.
		
		MoonDFrac = MoonDist / msmax;
		MoonAng = mangsiz / MoonDFrac;
		
		pphaseR.val = MoonPhase;
		mageR.val = synmonth * (fixangle(MoonAge) / 360.0);
		distR.val = MoonDist;
		angdiaR.val = MoonAng;
		sudistR.val = SunDist;
		suangdiaR.val = SunAng;
		return torad(fixangle(MoonAge));
	}
	
}
