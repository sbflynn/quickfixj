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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.quickfixj.FIXComponent;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.field.BytesField;

import quickfix.field.converter.BooleanConverter;
import quickfix.field.converter.CharConverter;
import quickfix.field.converter.DecimalConverter;
import quickfix.field.converter.DoubleConverter;
import quickfix.field.converter.IntConverter;
import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.field.converter.UtcTimestampConverter;

/**
 * Field container used by messages, groups, and composites.
 */
public abstract class FieldMap implements FIXFieldGraph, Serializable {

    static final long serialVersionUID = -3193357271891865972L;

    private final int[] fieldOrder;

    private final NavigableMap<Integer, FIXField<?>> fields;

    //  private final TreeMap<Integer, List<Group>> groups = new TreeMap<Integer, List<Group>>();

    protected FieldMap(int[] fieldOrder) {

        this.fieldOrder = fieldOrder;

        fields = new TreeMap<Integer, FIXField<?>>(fieldOrder != null
                ? new FieldOrderComparator()
                : null);
    }

    protected FieldMap() {

        this(null);
    }

    public int[] getFieldOrder() {

        return fieldOrder;
    }

    public void clear() {

        fields.clear();
        //  groups.clear();
    }

    public boolean isEmpty() {

        return fields.isEmpty();
    }

    protected static int indexOf(int field, int[] fieldOrder) {

        if (fieldOrder != null) {
            for (int i = 0; i < fieldOrder.length; i++) {
                if (field == fieldOrder[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isOrderedField(int field, int[] fieldOrder) {

        return indexOf(field, fieldOrder) > -1;
    }

    private class FieldOrderComparator implements Comparator<Integer>, Serializable {

        private static final long serialVersionUID = 3416006398018829270L;

        private int rank(int field, int[] fieldOrder) {

            int index = indexOf(field, fieldOrder);
            return index > -1 ? index : Integer.MAX_VALUE; // unspecified fields
                                                           // are last
        }

        @Override
        public int compare(Integer tag1, Integer tag2) {

            int rank1 = rank(tag1, getFieldOrder());
            int rank2 = rank(tag2, getFieldOrder());

            return rank1 != Integer.MAX_VALUE || rank2 != Integer.MAX_VALUE ? rank1 - rank2 // order by rank if it is specified for either tag
            : tag1 - tag2; // order by tag if both tags have unspecified
                           // ordering
        }
    }

    public void setFields(FieldMap fieldMap) {

        fields.clear();
        fields.putAll(fieldMap.fields);
    }

    protected void setComponent(MessageComponent component) {

        component.copyTo(this);
    }

    protected void getComponent(MessageComponent component) {

        component.clear();
        component.copyFrom(this);
    }

    //    public void setGroups(FieldMap fieldMap) {
    //
    //        groups.clear();
    //        groups.putAll(fieldMap.groups);
    //    }
    //
    //    protected void setGroups(int key, List<Group> groupList) {
    //
    //        groups.put(key, groupList);
    //    }

    public void setString(int field, String value) {

        setField(new StringField(field, value));
    }

    //    public void setBytes(int field, byte[] value) {
    //
    //        setField(field, new BytesField(field, value));
    //    }

    public void setBoolean(int field, boolean value) {

        setField(new StringField(field, BooleanConverter.convert(value)));
    }

    public void setChar(int field, char value) {

        setField(new StringField(field, CharConverter.convert(value)));
    }

    public void setInt(int field, int value) {

        setField(new StringField(field, IntConverter.convert(value)));
    }

    public void setDouble(int field, double value) {

        setDouble(field, value, 0);
    }

    public void setDouble(int field, double value, int padding) {

        setField(new StringField(field, DoubleConverter.convert(value, padding)));
    }

    public void setDecimal(int field, BigDecimal value) {

        setField(new StringField(field, DecimalConverter.convert(value)));
    }

    public void setDecimal(int field, BigDecimal value, int padding) {

        setField(new StringField(field, DecimalConverter.convert(value, padding)));
    }

    public void setUtcTimeStamp(int field, Date value) {

        setUtcTimeStamp(field, value, false);
    }

    public void setUtcTimeStamp(int field, Date value, boolean includeMilliseconds) {

        setField(new StringField(field, UtcTimestampConverter.convert(value, includeMilliseconds)));
    }

    public void setUtcTimeOnly(int field, Date value) {

        setUtcTimeOnly(field, value, false);
    }

    public void setUtcTimeOnly(int field, Date value, boolean includeMillseconds) {

        setField(new StringField(field, UtcTimeOnlyConverter.convert(value, includeMillseconds)));
    }

    public void setUtcDateOnly(int field, Date value) {

        setField(new StringField(field, UtcDateOnlyConverter.convert(value)));
    }

    @Override
    public FIXField<?> getField(int field) {

        FIXField<?> f = fields.get(field);

        if (f == null) {
            throw new FieldNotFound(field);
        }
        return f;
    }

    protected <T extends FIXField> T getField(int field, Class<T> fieldType) {

        FIXField<?> f = fields.get(field);

        if (f == null) {
            throw new FieldNotFound(field);
        }

        if (fieldType.isInstance(f)) {
            return fieldType.cast(f);
        }

        try {

            // fall back to reflection only in the case that the stored field type doesn't match the required 
            // type. This should never happen if data dictionaries are set up correctly
            return fieldType.getConstructor(CharSequence.class).newInstance(f.getCharacters());

        } catch (Exception e) {
            throw new FieldConvertError(String.format(
                    "Stored field of type %s cannot be converted to required field of type %s",
                    f.getClass(), fieldType));
        }
    }

    private FIXField<?> getFieldOrDefault(int field, FIXField<?> defaultValue) {

        final FIXField<?> f = fields.get(field);
        if (f == null) {
            return defaultValue;
        }
        return f;
    }

    @Override
    public void setField(FIXField<?> field) {

        if (field.getValue() == null) {
            throw new NullPointerException("Null field values are not allowed.");
        }
        fields.put(field.getTag(), field);
    }

    public String getString(int field) throws FieldNotFound {

        return getField(field).getCharacters().toString();
    }

    public boolean getBoolean(int field) throws FieldNotFound {

        try {
            return BooleanConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public char getChar(int field) throws FieldNotFound {

        try {
            return CharConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public int getInt(int field) throws FieldNotFound {

        try {
            return IntConverter.convert(getField(field).getCharacters().toString());
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public double getDouble(int field) throws FieldNotFound {

        try {
            return DoubleConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public BigDecimal getDecimal(int field) throws FieldNotFound {

        try {
            return DecimalConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public Date getUtcTimeStamp(int field) throws FieldNotFound {

        try {
            return UtcTimestampConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public Date getUtcTimeOnly(int field) throws FieldNotFound {

        try {
            return UtcTimeOnlyConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public Date getUtcDateOnly(int field) throws FieldNotFound {

        try {
            return UtcDateOnlyConverter.convert(getString(field));
        } catch (final FieldConvertError e) {
            throw newIncorrectDataException(e, field);
        }
    }

    public void setField(int key, FIXField<?> field) {

        fields.put(key, field);
    }

    //        public void setField(BooleanField field) {
    //    
    //            setBoolean(field.getTag(), field.getValue());
    //        }
    //    
    //        public void setField(CharField field) {
    //    
    //            setChar(field.getTag(), field.getValue());
    //        }
    //    
    //        public void setField(IntField field) {
    //    
    //            setInt(field.getTag(), field.getValue());
    //        }
    //    
    //        public void setField(DoubleField field) {
    //    
    //            setDouble(field.getTag(), field.getValue());
    //        }
    //    
    //        public void setField(DecimalField field) {
    //    
    //            setDecimal(field.getTag(), field.getValue());
    //        }
    //    
    //        public void setField(UtcTimeStampField field) {
    //    
    //            setUtcTimeStamp(field.getTag(), field.getValue(), field.showMilliseconds());
    //        }
    //    
    //        public void setField(UtcTimeOnlyField field) {
    //    
    //            setUtcTimeOnly(field.getTag(), field.getValue(), field.showMilliseconds());
    //        }
    //    
    //        public void setField(UtcDateOnlyField field) {
    //    
    //            setUtcDateOnly(field.getTag(), field.getValue());
    //        }
    //    
    //        public void setField(BytesField field) {
    //    
    //            setBytes(field.getTag(), field.getObject());
    //        }

    static <T extends Serializable, F extends Field<T>> F updateValue(F field, T value) {

        field.setObject(value);
        return field;
    }

    public BytesField getField(BytesField field) throws FieldNotFound {

        final FIXField<?> returnField = fields.get(field.getTag());
        if (returnField == null) {
            throw new FieldNotFound(field.getTag());
        } else if (returnField instanceof BytesField) {
            return (BytesField) returnField;
        } else {
            throw new FieldException(SessionRejectReasonText.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    field.getTag());
        }
    }

    private FieldException newIncorrectDataException(FieldConvertError e, int tag) {

        return new FieldException(SessionRejectReasonText.INCORRECT_DATA_FORMAT_FOR_VALUE,
                e.getMessage(), tag);
    }

    @Deprecated
    public boolean isSetField(int field) {

        return fields.containsKey(field);
    }

    @Override
    public boolean isFieldSet(int field) {

        return fields.containsKey(field);
    }

    public boolean isSetField(FIXField<?> field) {

        return isFieldSet(field.getTag());
    }

    public void removeField(int field) {

        fields.remove(field);
    }

    @Override
    public Iterator<FIXField<?>> iterator() {

        return fields.values().iterator();
    }

    protected void initializeFrom(FieldMap source) {

        fields.clear();
        fields.putAll(source.fields);
        //        for (Entry<Integer, List<Group>> entry : source.groups.entrySet()) {
        //            final List<Group> clones = new ArrayList<Group>();
        //            for (final Group group : entry.getValue()) {
        //                final Group clone = new Group(group.getFieldTag(), group.delim(),
        //                        group.getFieldOrder());
        //                clone.initializeFrom(group);
        //                clones.add(clone);
        //            }
        //            groups.put(entry.getKey(), clones);
        //        }
    }

    private boolean isGroupField(FIXField<?> field) {

        return (field instanceof FIXGroupField);
    }

    private static void appendField(StringBuilder buffer, FIXField<?> field) {

        if (field != null) {

            buffer.append(field.getTag());
            buffer.append('=');
            buffer.append(field.getCharacters());
            buffer.append('\001');

            if (field instanceof FIXGroupField<?>) {
                FIXGroupField<?> groupField = (FIXGroupField<?>) field;
                for (FIXGroup group : groupField.getGroups()) {
                    for (FIXField<?> groupSubfield : group) {
                        appendField(buffer, groupSubfield);
                    }
                }
            }
        }
    }

    protected void calculateString(StringBuilder buffer, int[] preFields, int[] postFields) {

        if (preFields != null) {
            for (int preField : preFields) {
                appendField(buffer, getFieldOrDefault(preField, null));
            }
        }

        for (final FIXField<?> field : fields.values()) {

            final int tag = field.getTag();
            if (!isOrderedField(tag, preFields) && !isOrderedField(tag, postFields)
                    && !isGroupField(field)) {
                appendField(buffer, field);
            } else if (isGroupField(field) && getGroupCount(tag) > 0) {
                appendField(buffer, field);
            }
        }

        if (postFields != null) {
            for (int postField : postFields) {
                appendField(buffer, getFieldOrDefault(postField, null));
            }
        }
    }

    int calculateLength() {

        return calculateLength(this);
    }

    int calculateLength(FIXFieldGraph graph) {

        int result = 0;

        for (final FIXField<?> field : graph) {

            int tag = field.getTag();
            if (tag != FixTags.BEGIN_STRING && tag != FixTags.BODY_LENGTH
                    && tag != FixTags.CHECK_SUM) {
                result += field.getLength();
            }

            if (field instanceof FIXGroupField<?>) {
                FIXGroupField<?> groupField = (FIXGroupField<?>) field;
                for (FIXGroup group : groupField.getGroups()) {
                    result += calculateLength(group);
                }
            }
        }

        return result;
    }

    int calculateChecksum() {

        return calculateChecksum(this);
    }

    int calculateChecksum(FIXFieldGraph graph) {

        int result = 0;

        for (final FIXField<?> field : graph) {
            if (field.getTag() != FixTags.CHECK_SUM) {
                result += field.getChecksum();
            }

            if (field instanceof FIXGroupField<?>) {
                FIXGroupField<?> groupField = (FIXGroupField<?>) field;
                for (FIXGroup group : groupField.getGroups()) {
                    result += calculateChecksum(group);
                }
            }
        }

        return result & 0xFF;
    }

    //    /**
    //     * Returns the number of groups associated with the specified count tag.
    //     *
    //     * @param tag the count tag number
    //     * @return the number of times the group repeats
    //     */
    //    public int getGroupCount(int tag) {
    //
    //        return getGroups(tag).size();
    //    }

    //    public Iterator<Integer> groupKeyIterator() {
    //
    //        return groups.keySet().iterator();
    //    }
    //
    //    Map<Integer, List<Group>> getGroups() {
    //
    //        return groups;
    //    }

    private <T extends FIXGroup> FIXGroupField<T> getGroupField(int fieldTag, boolean create) {

        FIXGroupField<T> field = (FIXGroupField<T>) fields.get(fieldTag);

        if (field == null && create) {

            field = new GroupField<T>(fieldTag, -1, -1);
            fields.put(fieldTag, field);
        }

        return field;
    }

    /**
     * Returns the number of groups associated with the specified count tag.
     *
     * @param tag the count tag number
     * @return the number of times the group repeats
     */
    public int getGroupCount(int fieldTag) {

        FIXGroupField<?> field = getGroupField(fieldTag, false);

        if (field == null) {

            return 0;
        }

        return field.getGroups().size();
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

    public <T extends FIXGroup> void addGroup(T group) {

        FIXGroupField<T> field = getGroupField(group.getFieldTag(), true);

        field.getGroups().add(group);
        // addGroupRef(new Group(group));
    }

    //    public void addGroupRef(Group group) {
    //
    //        int countTag = group.getFieldTag();
    //        List<Group> currentGroups = getGroups(countTag);
    //        currentGroups.add(group);
    //        setGroupCount(countTag, currentGroups.size());
    //    }

    protected void setGroupCount(int countTag, int groupSize) {

        // setField(countTag, new StringField(countTag, Integer.toString(groupSize)));
    }

    //    public List<Group> getGroups(int field) {
    //
    //        List<Group> groupList = groups.get(field);
    //        if (groupList == null) {
    //            groupList = new ArrayList<Group>();
    //            groups.put(field, groupList);
    //        }
    //        return groupList;
    //    }

    public <T extends FIXGroup> T getRepeatingGroup(int fieldTag, int index) throws FieldNotFound {

        FIXGroupField<T> field = getGroupField(fieldTag, false);

        if (field == null || index > field.getGroups().size()) {
            throw new FieldNotFound(fieldTag + ", index=" + index);
        }
        final T grp = field.getGroups().get(index - 1);
        //        group.setFields(grp);
        //        group.setGroups(grp);
        return grp;
    }

    public <T extends FIXGroup> T getGroup(int num, T group) throws FieldNotFound {

        FIXGroupField<T> field = getGroupField(group.getFieldTag(), false);

        if (field == null || num > field.getGroups().size()) {
            throw new FieldNotFound(group.getFieldTag() + ", index=" + num);
        }
        final T grp = field.getGroups().get(num - 1);
        for (FIXField<?> grpField : grp) {
            group.setField(grpField);
        }

        //        group.setGroups(grp);
        return group;
    }

    //    public Group getGroup(int num, int groupTag) throws FieldNotFound {
    //
    //        List<Group> groupList = getGroups(groupTag);
    //        if (num > groupList.size()) {
    //            throw new FieldNotFound(groupTag + ", index=" + num);
    //        }
    //        return groupList.get(num - 1);
    //    }

    public <T extends FIXGroup> void replaceGroup(int num, T group) {

        final int offset = num - 1;
        FIXGroupField<T> groupList = getGroupField(group.getFieldTag(), true);
        if (offset < 0 || offset >= groupList.getGroups().size()) {
            return;
        }
        groupList.getGroups().set(offset, group);
    }

    public void removeGroup(int field) {

        //  getGroups(field).clear();
        removeField(field);
    }

    public void removeGroup(int num, int fieldTag) {

        FIXGroupField<?> groupField = getGroupField(fieldTag, false);

        if (groupField != null) {

            if (num <= groupField.getGroups().size()) {
                groupField.getGroups().remove(num - 1);
            }

            if (groupField.getGroups().isEmpty()) {
                removeGroup(fieldTag);
            }
        }
    }

    public void removeGroup(int num, Group group) {

        removeGroup(num, group.getFieldTag());
    }

    public void removeGroup(Group group) {

        removeGroup(group.getFieldTag());
    }

    public boolean hasGroup(int fieldTag) {

        return getGroupCount(fieldTag) > 0;
    }

    public boolean hasGroup(int num, int fieldTag) {

        return num <= getGroupCount(fieldTag);
    }

    public boolean hasGroup(int num, Group group) {

        return hasGroup(num, group.getFieldTag());
    }

    public boolean hasGroup(Group group) {

        return hasGroup(group.getFieldTag());
    }
}
