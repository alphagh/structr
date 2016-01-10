/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import java.util.Iterator;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.DatabaseService;
import org.structr.api.util.Iterables;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;

//~--- classes ----------------------------------------------------------------
/**
 * Rebuild index for nodes or relationships of given type.
 *
 * Use 'type' argument for node type, and 'relType' for relationship type.
 *
 *
 */
public class BulkCreateLabelsCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkCreateLabelsCommand.class.getName());

	//~--- methods --------------------------------------------------------
	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String entityType                = (String) attributes.get("type");
		final DatabaseService graphDb          = (DatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);

		Iterator<AbstractNode> nodeIterator = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			nodeIterator = Iterables.filter(new TypePredicate<>(entityType), Iterables.map(nodeFactory, Iterables.filter(new StructrAndSpatialPredicate(true, false, false), graphDb.getAllNodes()))).iterator();
			tx.success();

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Exception while creating all nodes iterator.");
			fex.printStackTrace();
		}

		if (entityType == null) {

			logger.log(Level.INFO, "Node type not set or no entity class found. Starting creation of labels for all nodes.");

		} else {

			logger.log(Level.INFO, "Starting creation of labels for all nodes of type {0}", entityType);
		}

		final long count = NodeServiceCommand.bulkGraphOperation(securityContext, nodeIterator, 1000, "CreateLabels", new BulkGraphOperation<AbstractNode>() {

			@Override
			public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

				final String type = node.getProperty(GraphObject.type);
				if (type != null) {

					try {

						// Since the setProperty method of the TypeProperty
						// overrides the default setProperty behaviour, we
						// do not need to set a different type value first.

						node.unlockReadOnlyPropertiesOnce();
						GraphObject.type.setProperty(securityContext, node, type);

					} catch (FrameworkException fex) {
						// ignore
					}
				}
				node.updateInIndex();

			}

			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
				logger.log(Level.WARNING, "Unable to create labels for node {0}: {1}", new Object[]{node, t.getMessage()});
			}

			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
				logger.log(Level.WARNING, "Unable to create labels for node: {0}", t.getMessage());
			}
		});

		logger.log(Level.INFO, "Done with creating labels on {0} nodes", count);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
