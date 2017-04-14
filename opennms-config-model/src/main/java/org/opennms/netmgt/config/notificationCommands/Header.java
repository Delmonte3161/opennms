/*******************************************************************************
 * This file is part of OpenNMS(R).
 * 
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 * 
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * 
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *     http://www.gnu.org/licenses/
 * 
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config.notificationCommands;


import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class Header.
 * 
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "header")
@XmlAccessorType(XmlAccessType.FIELD)
public class Header implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "ver", required = true)
    private String ver;

    /**
     * creation time in the 'dow mon dd hh:mm:ss zzz yyyy'
     *  format
     */
    @XmlElement(name = "created", required = true)
    private String created;

    @XmlElement(name = "mstation", required = true)
    private String mstation;

    public Header() {
    }

    /**
     * Overrides the Object.equals method.
     * 
     * @param obj
     * @return true if the objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }
        
        if (obj instanceof Header) {
            Header temp = (Header)obj;
            boolean equals = Objects.equals(temp.ver, ver)
                && Objects.equals(temp.created, created)
                && Objects.equals(temp.mstation, mstation);
            return equals;
        }
        return false;
    }

    /**
     * Returns the value of field 'created'. The field 'created' has the following
     * description: creation time in the 'dow mon dd hh:mm:ss zzz yyyy'
     *  format
     * 
     * @return the value of field 'Created'.
     */
    public String getCreated() {
        return this.created;
    }

    /**
     * Returns the value of field 'mstation'.
     * 
     * @return the value of field 'Mstation'.
     */
    public String getMstation() {
        return this.mstation;
    }

    /**
     * Returns the value of field 'ver'.
     * 
     * @return the value of field 'Ver'.
     */
    public String getVer() {
        return this.ver;
    }

    /**
     * Method hashCode.
     * 
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        int hash = Objects.hash(
            ver, 
            created, 
            mstation);
        return hash;
    }

    /**
     * Sets the value of field 'created'. The field 'created' has the following
     * description: creation time in the 'dow mon dd hh:mm:ss zzz yyyy'
     *  format
     * 
     * @param created the value of field 'created'.
     */
    public void setCreated(final String created) {
        this.created = created;
    }

    /**
     * Sets the value of field 'mstation'.
     * 
     * @param mstation the value of field 'mstation'.
     */
    public void setMstation(final String mstation) {
        this.mstation = mstation;
    }

    /**
     * Sets the value of field 'ver'.
     * 
     * @param ver the value of field 'ver'.
     */
    public void setVer(final String ver) {
        this.ver = ver;
    }

}
