/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2023
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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.Map;

import visad.RealType;

public class HSRL2D extends ProfileAlongTrack {

      public HSRL2D() {
      }

      public HSRL2D(MultiDimensionReader reader, Map<String, Object> metadata) {
        super(reader, metadata);
      }

      public float[] getVertBinAltitude() throws Exception {
         float[] altitude = new float[VertLen];
         for (int k=0; k<VertLen; k++) {
            altitude[k] = -300 + k*15f;
         }
         return altitude;
      }

      public float[] getTrackTimes() throws Exception {
        return null;
      }

      public RealType makeVertLocType() throws Exception {
        return RealType.Altitude;
      }

      public RealType makeTrackTimeType() throws Exception {
        return null;
      }

      public float[] getTrackLongitude() throws Exception {
        int[] start = new int[] {0,0};
        int[] count = new int[] {TrackLen,1};
        int[] stride = new int[] {1,1};
        double[] dvals = reader.getDoubleArray((String)metadata.get(longitude_name), start, count, stride);
        float[] vals = new float[dvals.length];
        for (int i=0; i<vals.length;i++) vals[i] = (float)dvals[i];
        return vals;
      }

      public float[] getTrackLatitude() throws Exception {
        int[] start = new int[] {0,0};
        int[] count = new int[] {TrackLen,1};
        int[] stride = new int[] {1,1};
        double[] dvals = reader.getDoubleArray((String)metadata.get(latitude_name), start, count, stride);
        float[] vals = new float[dvals.length];
        for (int i=0; i<vals.length;i++) vals[i] = (float)dvals[i];
        return vals;
      }

      public Map<String, double[]> getDefaultSubset() {
        Map<String, double[]> subset = ProfileAlongTrack.getEmptySubset();

        double[] coords = subset.get("TrackDim");
        coords[0] = 0.0;
        coords[1] = TrackLen - 1;
        coords[2] = 5.0;
        subset.put("TrackDim", coords);

        coords = subset.get("VertDim");
        coords[0] = 0.0;
        coords[1] = VertLen - 1;
        coords[2] = 2.0;
        subset.put("VertDim", coords);
        return subset;
      }

}
