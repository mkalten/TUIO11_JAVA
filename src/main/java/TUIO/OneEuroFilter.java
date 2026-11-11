/*
 * TUIO Java library
 * Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>
 * Based on an example by Nicolas Roussel <nicolas.roussel@inria.fr>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package TUIO;

/**
 * One Euro filter for smoothing noisy signals while minimizing lag.
 * Port of the C++ OneEuroFilter implementation.
 */
public class OneEuroFilter {

	private static class LowPassFilter {
		private final double initval;
		private double lastRawValue;
		private double lastResult;
		private boolean initialized = false;

		LowPassFilter(double initval, double alpha) {
			this.initval = initval;
			lastRawValue = initval;
			lastResult = initval;
		}

		LowPassFilter(LowPassFilter f) {
			initval = f.initval;
			lastRawValue = f.lastRawValue;
			lastResult = f.lastResult;
			initialized = f.initialized;
		}

		double filter(double value, double alpha) {
			if (alpha <= 0.0 || alpha > 1.0) return value;

			double result;
			if (initialized)
				result = alpha * value + (1.0 - alpha) * lastResult;
			else {
				result = value;
				initialized = true;
			}

			lastRawValue = value;
			lastResult = result;
			return result;
		}
	}

	private double freq;
	private final double mincutoff;
	private final double beta;
	private final double dcutoff;
	private LowPassFilter x;
	private LowPassFilter dx;
	private double lasttime = -1; // undefined

	/**
	 * @param f  sampling frequency in Hz, must be &gt; 0
	 * @param mc minimal cutoff frequency, must be &gt; 0 (default 1.0)
	 * @param b  speed coefficient, must be &gt; 0 (default 0.0)
	 * @param dc derivative cutoff frequency, must be &gt; 0 (default 1.0)
	 */
	public OneEuroFilter(double f, double mc, double b, double dc) {
		if (f <= 0) throw new IllegalArgumentException("freq should be >0");
		freq = f;
		if (mc <= 0) throw new IllegalArgumentException("mincutoff should be >0");
		mincutoff = mc;
		if (b <= 0) throw new IllegalArgumentException("beta should be >0");
		beta = b;
		if (dc <= 0) throw new IllegalArgumentException("dcutoff should be >0");
		dcutoff = dc;

		x = new LowPassFilter(0.0, alpha(mincutoff));
		dx = new LowPassFilter(0.0, alpha(dcutoff));
	}

	public OneEuroFilter(double f, double mc, double b) {
		this(f, mc, b, 1.0);
	}

	public OneEuroFilter(double f) {
		this(f, 1.0, 0.0, 1.0);
	}

	public OneEuroFilter(OneEuroFilter f) {
		freq = f.freq;
		mincutoff = f.mincutoff;
		beta = f.beta;
		dcutoff = f.dcutoff;
		x = new LowPassFilter(f.x);
		dx = new LowPassFilter(f.dx);
		lasttime = f.lasttime;
	}

	private double alpha(double cutoff) {
		double te = 1.0 / freq;
		double tau = 1.0 / (2.0 * Math.PI * cutoff);
		return 1.0 / (1.0 + tau / te);
	}

	/**
	 * Filters the provided value. The dt parameter is the time since the
	 * previous call in seconds; pass a negative value if undefined.
	 */
	public double filter(double value, double dt) {
		// update the sampling frequency based on timestamps
		if (lasttime >= 0 && dt > 0 && dt != lasttime)
			freq = 1.0 / dt;
		lasttime = dt;

		// estimate the current variation per second
		double dvalue = x.initialized ? (value - x.lastRawValue) * freq : value;

		double edvalue = dx.filter(dvalue, alpha(dcutoff));
		// use it to update the cutoff frequency
		double cutoff = mincutoff + beta * Math.abs(edvalue);
		// filter the given value
		return x.filter(value, alpha(cutoff));
	}

	/** Filters the provided value without a timestamp. */
	public double filter(double value) {
		return filter(value, -1);
	}
}
