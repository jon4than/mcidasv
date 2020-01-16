/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2020
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.cyclone;

import java.util.ArrayList;
import java.util.List;

import visad.DateTime;
import visad.Real;
import visad.georef.EarthLocation;

/**
 * Created by IntelliJ IDEA. User: yuanho Date: Apr 18, 2008 Time: 1:45:38 PM To
 * change this template use File | Settings | File Templates.
 */

public class StormTrackPoint implements Comparable {

	/** _more_ */
	private int id = -1;

	/** _more_ */
	private boolean edited = false;

	/** _more_ */
	private EarthLocation location;

	/** _more_ */
	private DateTime time;

	/** _more_ */
	private List<Real> attributes;

	/** _more_ */
	private int forecastHour = 0;

	/**
	 * copy ctor
	 * 
	 * @param that
	 *            The track point to copy
	 */
	public StormTrackPoint(StormTrackPoint that) {
		this.location = that.location;
		this.time = that.time;
		if (that.attributes != null) {
			this.attributes = (List<Real>) new ArrayList(that.attributes);
		}
		this.forecastHour = that.forecastHour;
		this.id = that.id;
		this.edited = that.edited;
	}

	/**
	 * _more_
	 * 
	 * @param pointLocation
	 *            _more_
	 * @param time
	 *            _more_
	 * @param forecastHour
	 *            _more_
	 * @param attrs
	 *            _more_
	 */
	public StormTrackPoint(EarthLocation pointLocation, DateTime time,
			int forecastHour, List<Real> attrs) {
		this.location = pointLocation;
		this.time = time;
		this.forecastHour = forecastHour;
		this.attributes = attrs;
	}

	/**
	 * Compare this object to another.
	 * 
	 * @param o
	 *            object in question.
	 * @return spec from Comparable interface.
	 */
	public int compareTo(Object o) {
		if (o instanceof StormTrackPoint) {
			StormTrackPoint that = (StormTrackPoint) o;
			return (time.compareTo(that.time));
		}
		return toString().compareTo(o.toString());
	}

	/**
	 * Set the ForecastHour property.
	 * 
	 * @param value
	 *            The new value for ForecastHour
	 */
	public void setForecastHour(int value) {
		forecastHour = value;
	}

	/**
	 * Get the ForecastHour property.
	 * 
	 * @return The ForecastHour
	 */
	public int getForecastHour() {
		return forecastHour;
	}

	/**
	 * _more_
	 * 
	 * @param time
	 *            _more_
	 */
	public void setTime(DateTime time) {
		this.time = time;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public DateTime getTime() {
		return time;
	}

	/**
	 * _more_
	 * 
	 * @param point
	 *            _more_
	 */
	public void setLocation(EarthLocation point) {
		this.location = point;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public EarthLocation getLocation() {
		return location;
	}

	/*
	 * _more_
	 * 
	 * @return _more_
	 */

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public List<Real> getTrackAttributes() {
		return attributes;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public String toString() {
		return location + "";
	}

	/**
	 * _more_
	 * 
	 * 
	 * @param param
	 *            _more_
	 * 
	 * @return _more_
	 */
	public Real getAttribute(StormParam param) {
		return param.getAttribute(attributes);
	}

	/**
	 * _more_
	 * 
	 * 
	 * @param real
	 *            _more_
	 * 
	 */
	public void setAttribute(Real real) {
		for (Real attr : attributes) {
			if (attr.getType().equals(real.getType())) {
				// attr.(real)
				attributes.remove(attr);
				attributes.add(real);
			}
		}

	}

	/**
	 * _more_
	 * 
	 * @param attr
	 *            _more_
	 * 
	 */
	public void addAttribute(Real attr) {
		if (attributes == null) {
			attributes = new ArrayList<Real>();
		}

		attributes.add(attr);

	}

	/**
	 * _more_
	 * 
	 * @param o
	 *            _more_
	 * 
	 * @param value
	 *            _more_
	 * 
	 * @return _more_
	 */
	/*
	 * public boolean equals(Object o) { if (o == null) { return false; } if (
	 * !(o instanceof StormTrackPoint)) { return false; } StormTrackPoint other
	 * = (StormTrackPoint) o; return
	 * ((trackPointId.equals(other.trackPointId))); }
	 */

	/**
	 * Set the Id property.
	 * 
	 * @param value
	 *            The new value for Id
	 */
	public void setId(int value) {
		id = value;
	}

	/**
	 * Get the Id property.
	 * 
	 * @return The Id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the Edited property.
	 * 
	 * @param value
	 *            The new value for Edited
	 */
	public void setEdited(boolean value) {
		edited = value;
	}

	/**
	 * Get the Edited property.
	 * 
	 * @return The Edited
	 */
	public boolean getEdited() {
		return edited;
	}

}
