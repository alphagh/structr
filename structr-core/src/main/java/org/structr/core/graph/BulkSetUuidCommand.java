/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;


//~--- classes ----------------------------------------------------------------

/**
 * Sets a new UUID on each graph object of the given type. For nodes, set type,
 * for relationships relType.
 *
 *
 */
public class BulkSetUuidCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetUuidCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String nodeType         = (String) attributes.get("type");
		final String relType          = (String) attributes.get("relType");
		final Boolean allNodes        = (Boolean) attributes.get("allNodes");
		final Boolean allRels         = (Boolean) attributes.get("allRels");
		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");

		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);
		final RelationshipFactory relFactory   = new RelationshipFactory(superUserContext);

		if (nodeType != null || Boolean.TRUE.equals(allNodes)) {

			Result<AbstractNode> result = null;
			Iterable<Node> nodes        = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				if (Boolean.TRUE.equals(allNodes)) {
					nodes = Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllNodes());
				} else {
					nodes = Iterables.filter(new TypePredicate<>(nodeType),Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllNodes()));
				}

				result = nodeFactory.instantiateAll(nodes);

				tx.success();

			} catch (FrameworkException fex) {
				logger.log(Level.WARNING, "Exception while creating all nodes iterator.");
				fex.printStackTrace();
			}

			logger.log(Level.INFO, "Start setting UUID on all nodes of type {0}", new Object[] { nodeType });

			final long count = bulkGraphOperation(securityContext, result.getResults().iterator(), 1000, "SetNodeUuid", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(final SecurityContext securityContext, final AbstractNode node) {

					try {
						node.setProperty(GraphObject.id, NodeServiceCommand.getNextUuid());

					} catch (FrameworkException fex) {

						logger.log(Level.WARNING, "Unable to set UUID of node {0}: {1}", new Object[] { node, fex.getMessage() });
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.log(Level.WARNING, "Unable to set UUID of node {0}: {1}", new Object[] { node, t.getMessage() });
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set UUID on node: {0}", t.getMessage());
				}
			});

			logger.log(Level.INFO, "Done with setting UUID on {0} nodes", count);

			return;
		}

		if (relType != null || Boolean.TRUE.equals(allRels)) {

			Result<AbstractRelationship> result = null;
			Iterable<Relationship> rels         = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				if (Boolean.TRUE.equals(allRels)) {
					rels = Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllRelationships());
				} else {
					rels = Iterables.filter(new TypePredicate<>(relType), Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllRelationships()));
				}

				result = relFactory.instantiateAll(rels);

				tx.success();

			} catch (FrameworkException fex) {
				logger.log(Level.WARNING, "Exception while creating all nodes iterator.");
				fex.printStackTrace();
			}

			logger.log(Level.INFO, "Start setting UUID on all rels of type {0}", new Object[] { relType });

			final long count = bulkGraphOperation(securityContext, result.getResults().iterator(), 1000, "SetRelationshipUuid", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					try {
						rel.setProperty(GraphObject.id, NodeServiceCommand.getNextUuid());

					} catch (FrameworkException fex) {

						logger.log(Level.WARNING, "Unable to set UUID of relationship {0}: {1}", new Object[] { rel, fex.getMessage() });
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.log(Level.WARNING, "Unable to set UUID of relationship {0}: {1}", new Object[] { rel, t.getMessage() });
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set UUID on relationship: {0}", t.getMessage());
				}
			});

			logger.log(Level.INFO, "Done with setting UUID on {0} relationships", count);

			return;
		}

		logger.log(Level.INFO, "Unable to determine entity type to set UUID.");

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
