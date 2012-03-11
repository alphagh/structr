/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core;

import java.util.Date;
import org.structr.common.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 * A common base class for {@see AbstractNode} and {@see AbstractRelationship}.
 *
 * @author Christian Morgner
 */
public interface GraphObject {

	// ----- common to both types -----
	public long getId();
	public String getType();

	public Iterable<String> getPropertyKeys(String propertyView);
	public void setProperty(String key, Object value) throws FrameworkException;
	public Object getProperty(String key);
	public String getStringProperty(String key);
	public Date getDateProperty(String key);
	public void removeProperty(String key) throws FrameworkException;

	public PropertyKey getDefaultSortKey();
	public String getDefaultSortOrder();

	public abstract boolean isValid(ErrorBuffer errorBuffer);
	
	// ----- rels only -----
//	public Long getStartNodeId();
//	public Long getEndNodeId();
//	public Long getOtherNodeId(final AbstractNode node);

	// ----- nodes only -----
//	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction);
//	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir);

	// ----- editing methods -----
//	public void delete(SecurityContext seucrityContext);

}
