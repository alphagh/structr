package org.structr.web.diff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;

/**
 *
 * @author Christian Morgner
 */
public abstract class InvertibleModificationOperation implements Comparable<InvertibleModificationOperation> {

	protected Map<String, DOMNode> hashMappedExistingNodes = new LinkedHashMap<>();

	public abstract void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException;
	public abstract InvertibleModificationOperation revert();
	public abstract Integer getPosition();

	public InvertibleModificationOperation(final Map<String, DOMNode> hashMappedExistingNodes) {
		this.hashMappedExistingNodes = hashMappedExistingNodes;
	}

	protected InsertPosition findInsertPosition(final Page sourcePage, final String parentHash, final List<String> siblingHashes, final DOMNode newNode) {

		DOMNode newParent  = hashMappedExistingNodes.get(parentHash);
		DOMNode newSibling = null;

		// we need to check the whole list of siblings here because
		// when inserting the first element of a list of elements,
		// the "next sibling" element is not there yet, so we have
		// to use the one after
		for (final String siblingHash : siblingHashes) {

			newSibling = hashMappedExistingNodes.get(siblingHash);
			if (newSibling != null) {
				break;
			}
		}

		if (newParent == null) {

			// new parent did not exist in source document, what do?
			// System.out.println("TODO: new parent not found in source document.");
		}

		return new InsertPosition(newParent, newSibling);
	}

	public static void collectNodes(final Page page, final Map<String, DOMNode> indexMappedNodes, final Map<String, DOMNode> hashMappedNodes, final Map<DOMNode, Integer> depthMap) {

		collectNodes(page, indexMappedNodes, hashMappedNodes, depthMap, 0, new LinkedHashMap<Integer, Integer>());
	}

	private static void collectNodes(final DOMNode node, final Map<String, DOMNode> indexMappedNodes, final Map<String, DOMNode> hashMappedNodes, final Map<DOMNode, Integer> depthMap, final int depth, final Map<Integer, Integer> childIndexMap) {

		Integer pos  = childIndexMap.get(depth);
		if (pos == null) {

			pos = 0;
		}

		int position = pos;
		childIndexMap.put(depth, ++position);

		// store node with its tree index
		final String hash = "[" + depth + ":" + position + "]";
		indexMappedNodes.put(hash, node);

		// store node with its data hash
		String dataHash = node.getProperty(DOMNode.dataHashProperty);
		if (dataHash == null) {
			dataHash = node.getIdHash();
		}

		hashMappedNodes.put(dataHash, node);
		depthMap.put(node, depth);

		// recurse
		for (final DOMChildren childRel : node.getChildRelationships()) {

			collectNodes(childRel.getTargetNode(), indexMappedNodes, hashMappedNodes, depthMap, depth+1, childIndexMap);
		}

	}

	@Override
	public int compareTo(final InvertibleModificationOperation op) {
		return getPosition().compareTo(op.getPosition());
	}

	protected static class InsertPosition {

		private DOMNode parent = null;
		private DOMNode sibling = null;

		public InsertPosition(final DOMNode parent, final DOMNode sibling) {
			this.parent  = parent;
			this.sibling = sibling;
		}

		public DOMNode getParent() {
			return parent;
		}

		public DOMNode getSibling() {
			return sibling;
		}
	}
}