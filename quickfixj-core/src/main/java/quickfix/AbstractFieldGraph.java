/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXComponent;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.field.FieldConversionException;
import org.quickfixj.field.GenericField;

/**
 * Field container used by messages, groups, and composites.
 */
public abstract class AbstractFieldGraph implements FIXFieldGraph, Serializable {

	static final long serialVersionUID = -3193357271891865972L;

	private NavigableMap<Integer, FIXField<?>> fields;

	// REVIEW : SHOULD THIS REALLY BE AN INSTANCE VARIABLE ??
	private List<Integer> fieldOrder;

	protected AbstractFieldGraph() {

		this(null);
	}

	protected AbstractFieldGraph(List<Integer> fieldOrder) {

		this.fieldOrder = fieldOrder;

		fields = new TreeMap<Integer, FIXField<?>>(
				fieldOrder != null ? new FieldOrderComparator() : null);
	}

	public List<Integer> getFieldOrder() {

		return fieldOrder;
	}

	@Override
	public void clear() {

		fields.clear();
	}

	@Override
	public boolean isEmpty() {

		return fields.isEmpty();
	}

	protected static int indexOf(int field, List<Integer> fieldOrder) {

		if (fieldOrder != null) {
			for (int i = 0; i < fieldOrder.size(); i++) {
				if (field == fieldOrder.get(i)) {
					return i;
				}
			}
		}
		return -1;
	}

	private static boolean isOrderedField(int field, List<Integer> fieldOrder) {

		return indexOf(field, fieldOrder) > -1;
	}

	private class FieldOrderComparator implements Comparator<Integer>,
			Serializable {

		private static final long serialVersionUID = 3416006398018829270L;

		private int rank(int field, List<Integer> fieldOrder) {

			int index = indexOf(field, fieldOrder);
			return index > -1 ? index : Integer.MAX_VALUE; // unspecified fields
															// are last
		}

		@Override
		public int compare(Integer tag1, Integer tag2) {

			int rank1 = rank(tag1, getFieldOrder());
			int rank2 = rank(tag2, getFieldOrder());

			return rank1 != Integer.MAX_VALUE || rank2 != Integer.MAX_VALUE ? rank1
					- rank2 // order by rank if it is specified for either tag
					: tag1 - tag2; // order by tag if both tags have unspecified
									// ordering
		}
	}

	@Deprecated
	public void setString(int field, String value) {

		setField(new GenericField(field, value));
	}

	@Override
	public FIXField<?> getField(int field) {

		FIXField<?> f = fields.get(field);

		if (f == null) {
			throw new FieldNotFound(field);
		}
		return f;
	}

	protected <T extends FIXField<?>> T getField(int field, Class<T> fieldType) {

		FIXField<?> f = fields.get(field);

		if (f == null) {
			throw new FieldNotFound(field);
		}

		if (fieldType.isInstance(f)) {
			return fieldType.cast(f);
		}

		try {

			// fall back to reflection only in the case that the stored field
			// type doesn't match the required
			// type. This should never happen if data dictionaries are set up
			// correctly
			return fieldType.getConstructor(CharSequence.class).newInstance(
					f.getCharacters());

		} catch (Exception e) {
			throw new FieldConversionException(
					String.format(
							"Stored field of type %s cannot be converted to required field of type %s",
							f.getClass(), fieldType));
		}
	}

	@Override
	public void setField(FIXField<?> field) {

		if (field.getValue() == null) {
			throw new NullPointerException("Null field values are not allowed.");
		}
		fields.put(field.getTag(), field);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @author stephen.flynn@jftechnology.com
	 * @since 2.0
	 */
	@Override
	public int getFieldCount() {
		return fields.size();
	}

	@Override
	public String getFieldValue(int field) {

		return getField(field).getCharacters().toString();
	}

	@Override
	public boolean isFieldSet(int field) {

		return fields.containsKey(field);
	}

	@Override
	public void removeField(int field) {

		fields.remove(field);
	}

	@Override
	public Iterator<FIXField<?>> iterator() {

		return fields.values().iterator();
	}

	private static void appendField(StringBuilder buffer, FIXFieldGraph graph,
			int tag) {

		if (graph.isFieldSet(tag)) {

			FIXField<?> field = graph.getField(tag);

			if (field instanceof FIXGroupField<?>) {
				FIXGroupField<?> groupField = (FIXGroupField<?>) field;
				if (groupField.size() > 0) {

					buffer.append(field.getTag());
					buffer.append('=');
					buffer.append(field.getCharacters());
					buffer.append('\001');

					for (FIXGroup group : groupField) {
						for (FIXField<?> groupSubfield : group) {
							appendField(buffer, group, groupSubfield.getTag());
						}
					}
				}
			} else {

				buffer.append(field.getTag());
				buffer.append('=');
				buffer.append(field.getCharacters());
				buffer.append('\001');
			}
		}
	}

	protected static void calculateString(FIXFieldGraph graph,
			StringBuilder buffer, List<Integer> preFields,
			List<Integer> postFields) {

		if (preFields != null) {
			for (int preField : preFields) {
				appendField(buffer, graph, preField);
			}
		}

		for (FIXField<?> field : graph) {

			int tag = field.getTag();
			if (!isOrderedField(tag, preFields)
					&& !isOrderedField(tag, postFields)) {
				appendField(buffer, graph, tag);
			}
		}

		if (postFields != null) {
			for (int postField : postFields) {
				appendField(buffer, graph, postField);
			}
		}
	}

	protected int calculateLength() {

		return calculateLength(this);
	}

	protected int calculateLength(FIXFieldGraph graph) {

		int result = 0;

		for (final FIXField<?> field : graph) {

			switch (field.getTag()) {

			case FIXTag.BEGIN_STRING:
			case FIXTag.BODY_LENGTH:
			case FIXTag.CHECK_SUM:
				// fields that do not contribute to length
				break;

			default:
				if (field instanceof FIXGroupField<?>) {
					FIXGroupField<?> groupField = (FIXGroupField<?>) field;
					if (groupField.size() > 0) {
						result += MessageUtils
								.length(CharsetSupport.getCharsetInstance(),
										groupField);
						for (FIXGroup group : groupField) {
							result += calculateLength(group);
						}
					}
				} else {
					result += MessageUtils.length(
							CharsetSupport.getCharsetInstance(), field);
				}
				break;
			}
		}

		return result;
	}

	protected int calculateChecksum() {

		return calculateChecksum(this);
	}

	protected int calculateChecksum(FIXFieldGraph graph) {

		int result = 0;

		for (final FIXField<?> field : graph) {

			if (field.getTag() == FIXTag.CHECK_SUM) {
				continue;
			}

			if (field instanceof FIXGroupField<?>) {
				FIXGroupField<?> groupField = (FIXGroupField<?>) field;
				if (groupField.size() > 0) {
					result += MessageUtils.checksum(
							CharsetSupport.getCharsetInstance(), groupField);
					for (FIXGroup group : groupField) {
						result += calculateChecksum(group);
					}
				}
			} else {
				result += MessageUtils.checksum(
						CharsetSupport.getCharsetInstance(), field);
			}
		}

		return result & 0xFF;
	}

	/**
	 * @param component
	 * @param fields
	 * @since 2.0
	 */
	public void copyValues(FIXComponent component, Collection<Integer> tags) {

		if (component != null) {

			for (Integer tag : tags) {
				if (component.isFieldSet(tag)) {
					setField(component.getField(tag));
				}
			}
		}
	}

	@Override
	public FIXFieldGraph clone() {

		try {

			// careful not to call super.clone() or we end up
			// with a shallow copy of the field storage
			AbstractFieldGraph clone = (AbstractFieldGraph) super.clone();

			clone.fieldOrder = fieldOrder;

			clone.fields = new TreeMap<Integer, FIXField<?>>(
					clone.fieldOrder != null ? new FieldOrderComparator()
							: null);

			for (FIXField<?> field : this) {
				clone.setField(field.clone());
			}

			return clone;

		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This will be expensive as it requires recursion through the fields on
	 * every call. Added for completeness only - this object should never be
	 * used as a map key.
	 *
	 * @author stephen.flynn@jftechnology.com
	 * @since 2.0
	 */
	@Override
	public int hashCode() {

		int hashcode = 17;

		for (FIXField<?> field : this) {
			hashcode = 37 * hashcode + field.hashCode();
		}

		return hashcode;
	}

	/**
	 * Two graphs are equal iff each and every field in this graph has an equal
	 * field in the other.
	 *
	 * @author stephen.flynn@jftechnology.com
	 * @since 2.0
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!FIXFieldGraph.class.isInstance(obj)) {
			return false;
		}

		FIXFieldGraph graph = (FIXFieldGraph) obj;

		if (getFieldCount() != graph.getFieldCount()) {
			return false;
		}

		for (FIXField<?> field : this) {
			if (!graph.isFieldSet(field.getTag())) {
				return false;
			}
			if (!field.equals(graph.getField(field.getTag()))) {
				return false;
			}
		}

		return true;
	}
}
