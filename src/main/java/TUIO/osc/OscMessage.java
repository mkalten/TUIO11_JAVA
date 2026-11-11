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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A decoded OSC message: an address pattern plus a list of typed
 * arguments. Argument values are boxed as Integer (i, c), Float (f),
 * String (s, S), byte[] (b), Long (h), Double (d, I), Character (c is
 * mapped to Integer — see decoder), Boolean (T, F) or OscNil (N).
 */
public class OscMessage {

	private String address;
	private final List<Object> arguments = new ArrayList<Object>();

	public OscMessage() {}

	public OscMessage(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void addArgument(Object argument) {
		arguments.add(argument);
	}

	public List<Object> getArguments() {
		return Collections.unmodifiableList(arguments);
	}

	public Object[] getArgumentArray() {
		return arguments.toArray();
	}

	public int size() {
		return arguments.size();
	}
}
