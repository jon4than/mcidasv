/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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
import java.util.HashMap;
import java.util.Map;

import ucar.ma2.DataType;
import visad.Set;
import visad.RealTupleType;
import visad.RealType;
import visad.Gridded3DSet;
import visad.GriddedSet;
import visad.Gridded3DDoubleSet;


public class ProfileAlongTrack3D extends MultiDimensionAdapter {

  public ProfileAlongTrack adapter2D;
  MultiDimensionReader reader;
  float[] vertLocs;
  RealTupleType domain3D;


  public ProfileAlongTrack3D(ProfileAlongTrack adapter2D) {
    super(adapter2D.getReader(), adapter2D.getMetadata());
    this.adapter2D = adapter2D;
    this.reader = adapter2D.getReader();
    rangeProcessor = adapter2D.getRangeProcessor();
    try {
      init();
    } catch (Exception e) {
      System.out.println("init failed");
    }
  }

  void init() throws Exception {
    vertLocs = adapter2D.getVertBinAltitude();
    domain3D = RealTupleType.SpatialEarth3DTuple;
    rangeType = adapter2D.getRangeType();
  }


  public Set makeDomain(Map<String, double[]> subset) throws Exception {
    double[] vert_coords = subset.get(ProfileAlongTrack.vertDim_name);
    double[] track_coords = subset.get(ProfileAlongTrack.trackDim_name);

    int vert_idx = adapter2D.getVertIdx();
    int track_idx = adapter2D.getTrackIdx();

    String lonArrayName = (String)getMetadata().get(ProfileAlongTrack.longitude_name);
    String latArrayName = (String)getMetadata().get(ProfileAlongTrack.latitude_name);

    int[] start = null;
    int[] count = null;
    int[] stride = null;

    int rank = (reader.getDimensionLengths(lonArrayName)).length;

    if (rank == 2) {
      start = new int[2];
      count = new int[2];
      stride = new int[2];

      start[vert_idx] = (int) 0;
      count[vert_idx] = (int) 1;
      stride[vert_idx] = (int) vert_coords[2];

      start[track_idx] = (int) track_coords[0];
      count[track_idx] = (int) ((track_coords[1] - track_coords[0])/track_coords[2] + 1f);
      stride[track_idx] = (int) track_coords[2];
    }
    else if (rank == 1) {
      start = new int[1];
      count = new int[1];
      stride = new int[1];
      start[0] = (int) track_coords[0];
      count[0] = (int) ((track_coords[1] - track_coords[0])/track_coords[2] + 1f);
      stride[0] = (int) track_coords[2];
    }

    int vert_len = (int) ((vert_coords[1] - vert_coords[0])/vert_coords[2] + 1f);
    int track_len = count[track_idx];

    float[] altitudes = new float[vert_len];
    for (int k=0; k<vert_len;k++) {
      altitudes[k] = vertLocs[(int)vert_coords[0] + k*((int)vert_coords[2])];
    }

    GriddedSet domainSet = null;

    if (reader.getArrayType(lonArrayName) == DataType.FLOAT ) {
      float[] lonValues = reader.getFloatArray(lonArrayName, start, count, stride);
      float[] latValues = reader.getFloatArray(latArrayName, start, count, stride);
      float[][] alt_lon_lat = new float[3][vert_len*track_len];
      oneD_threeDfill(lonValues, latValues, track_len, altitudes, vert_len, alt_lon_lat);
      domainSet = new Gridded3DSet(domain3D, alt_lon_lat, vert_len, track_len);
    }
    else if (reader.getArrayType(lonArrayName) == DataType.DOUBLE) {
      double[] lonValues = reader.getDoubleArray(lonArrayName, start, count, stride);
      double[] latValues = reader.getDoubleArray(latArrayName, start, count, stride);
      double[][] alt_lon_lat = new double[3][vert_len*track_len];
      double[] altsDbl = new double[altitudes.length];
      for (int i=0; i<altsDbl.length; i++) {
         altsDbl[i] = (double) altitudes[i];
      }
      oneD_threeDfillDbl(lonValues, latValues, track_len, altsDbl, vert_len, alt_lon_lat);
      domainSet = new Gridded3DDoubleSet(domain3D, alt_lon_lat, vert_len, track_len);
    }

    return domainSet;
  }
  

  public Map<String, double[]> getDefaultSubset() {
    return adapter2D.getDefaultSubset();
  }

  public Map<String, double[]> getSubsetFromLonLatRect(Map<String, double[]> subset, double minLat, double maxLat, double minLon, double maxLon) {
    return adapter2D.getSubsetFromLonLatRect(subset, minLat, maxLat, minLon, maxLon);
  }

  public Map<String, double[]> getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon) {
    return adapter2D.getSubsetFromLonLatRect(minLat, maxLat, minLon, maxLon);
  }

  public Map<String, double[]> getSubsetFromLonLatRect(double minLat, double maxLat, double minLon, double maxLon, int xStride, int yStride, int zStride) {
    return adapter2D.getSubsetFromLonLatRect(minLat, maxLat, minLon, maxLon, xStride, yStride, zStride);
  }

  public static void oneD_threeDfill(float[] b, float[] c, int leny, float[] a, int lenx, float[][] abc) {
    int cnt = 0;
    for (int i=0; i<leny; i++) {
      for (int j=0; j<lenx; j++) {
        abc[0][cnt] = b[i];
        abc[1][cnt] = c[i];
        abc[2][cnt] = a[j];
        cnt++;
       }
     }
   }

  public static void oneD_threeDfillDbl(double[] b, double[] c, int leny, double[] a, int lenx, double[][] abc) {
    int cnt = 0;
    for (int i=0; i<leny; i++) {
      for (int j=0; j<lenx; j++) {
        abc[0][cnt] = b[i];
        abc[1][cnt] = c[i];
        abc[2][cnt] = a[j];
        cnt++;
       }
     }
   }


}
