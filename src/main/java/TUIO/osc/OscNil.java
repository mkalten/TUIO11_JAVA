/*
 * TUIO OSC Codec
 * Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>
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

package TUIO.osc;

/**
 * Marker for the OSC nil argument value ('N' type tag).
 */
public final class OscNil {

	public static final OscNil NIL = new OscNil();

	private OscNil() {}

	@Override
	public String toString() {
		return "nil";
	}
}
