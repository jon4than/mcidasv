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
package edu.wisc.ssec.mcidasv.chooser;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.PreferenceList;

/**
 * A very simple chooser that allows for convenient access of remote model data.
 */
public class TDSGridChooser extends XmlChooser {

    /**
     * Create the {@code XmlChooser}
     *
     * @param mgr The {@link IdvChooserManager}. Cannot be {@code null}.
     * @param root XML root that defines this chooser. Cannot be {@code null}.
     *
     */
    public TDSGridChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

    /**
     * Overridden by McIDAS-V so that we're always calling 
     * {@link IdvChooser#getPreferenceList(String, boolean)} and thus always
     * performing a merge of user preferences and values from 
     * {@code mcidasv.properties}.
     * 
     * @param listProp Ignored for this chooser.
     */
    @Override public PreferenceList getPreferenceList(String listProp) {
        return super.getPreferenceList("idv.data.grid.list", true);
    }
}
