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
import java.util.Date;
import java.util.List;

/**
 * A decoded OSC bundle: an NTP timetag plus an ordered list of
 * elements, each of which is either an OscMessage or a nested
 * OscBundle.
 */
public class OscBundle {

	private long timetag = OscTimeTag.IMMEDIATE;
	private final List<Object> elements = new ArrayList<Object>();

	public OscBundle() {}

	public long getTimetag() {
		return timetag;
	}

	public void setTimetag(long timetag) {
		this.timetag = timetag;
	}

	public Date getTimestamp() {
		return OscTimeTag.toDate(timetag);
	}

	public void addElement(Object element) {
		elements.add(element);
	}

	public void addMessage(OscMessage message) {
		elements.add(message);
	}

	public void addBundle(OscBundle bundle) {
		elements.add(bundle);
	}

	public List<Object> getElements() {
		return Collections.unmodifiableList(elements);
	}

	public int size() {
		return elements.size();
	}
}
