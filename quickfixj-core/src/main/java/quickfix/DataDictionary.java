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

import static quickfix.FileUtil.Location.CLASSLOADER_RESOURCE;
import static quickfix.FileUtil.Location.CONTEXT_RESOURCE;
import static quickfix.FileUtil.Location.FILESYSTEM;
import static quickfix.FileUtil.Location.URL;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.quickfixj.xml.dictionary.ComponentDefinition;
import org.quickfixj.xml.dictionary.ComponentReference;
import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.DataDictionaryConfig.Fields;
import org.quickfixj.xml.dictionary.DataDictionaryConfig.Messages;
import org.quickfixj.xml.dictionary.DataDictionarySubsection;
import org.quickfixj.xml.dictionary.EnumBoolean;
import org.quickfixj.xml.dictionary.FieldDefinition;
import org.quickfixj.xml.dictionary.FieldReference;
import org.quickfixj.xml.dictionary.GroupDefinition;
import org.quickfixj.xml.dictionary.MessageDefinition;
import org.quickfixj.xml.dictionary.MessageParticle;
import org.quickfixj.xml.dictionary.ObjectFactory;
import org.xml.sax.SAXException;

import quickfix.field.BeginString;
import quickfix.field.MsgType;
import quickfix.field.SessionRejectReason;
import quickfix.field.converter.BooleanConverter;
import quickfix.field.converter.CharConverter;
import quickfix.field.converter.DoubleConverter;
import quickfix.field.converter.IntConverter;
import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.field.converter.UtcTimestampConverter;

/**
 * Provide the message metadata for various versions of FIX.
 */
public class DataDictionary {

    private static final String FIXT_PREFIX = "FIXT";

    private static final String FIX_PREFIX = "FIX";

    public static final String ANY_VALUE = "__ANY__";

    public static final String HEADER_ID = "HEADER";

    public static final String TRAILER_ID = "TRAILER";

    private static final String MESSAGE_CATEGORY_ADMIN = "admin".intern();

    private static final String MESSAGE_CATEGORY_APP = "app".intern();

    private static final int USER_DEFINED_TAG_MIN = 5000;

    private boolean hasVersion = false;

    private boolean checkFieldsOutOfOrder = true;

    private boolean checkFieldsHaveValues = true;

    private boolean checkUserDefinedFields = true;

    private boolean checkUnorderedGroupFields = true;

    private boolean allowUnknownMessageFields = false;

    private int[] orderedFieldsArray;

    private String beginString;

    private final Map<String, Set<Integer>> messageFields = new HashMap<String, Set<Integer>>();

    private final Map<String, Set<Integer>> requiredFields = new HashMap<String, Set<Integer>>();

    private final Set<String> messages = new HashSet<String>();

    private final Map<String, String> messageCategory = new HashMap<String, String>();

    private final Map<String, String> messageTypeForName = new HashMap<String, String>();

    private final LinkedHashSet<Integer> fields = new LinkedHashSet<Integer>();

    private final Map<Integer, FieldType> fieldTypes = new HashMap<Integer, FieldType>();

    private final Map<Integer, Set<String>> fieldValues = new HashMap<Integer, Set<String>>();

    private final Map<Integer, String> fieldNames = new HashMap<Integer, String>();

    private final Map<String, Integer> names = new HashMap<String, Integer>();

    private final Map<IntStringPair, String> valueNames = new HashMap<IntStringPair, String>();

    private final Map<IntStringPair, GroupInfo> groups = new HashMap<IntStringPair, GroupInfo>();

    private DataDictionary() {

    }

    /**
     * Initialize a data dictionary from a URL or a file path.
     *
     * @param location a URL or file system path
     * @throws ConfigError
     */
    public DataDictionary(String location) throws ConfigError {

        read(location);
    }

    /**
     * Initialize a data dictionary from an input stream.
     *
     * @param in the input stream
     * @throws ConfigError
     */
    public DataDictionary(InputStream in) throws ConfigError {

        loadJaxb(in);
    }

    /**
     * Copy a data dictionary.
     *
     * @param source the source dictionary that will be copied into this
     *        dictionary
     */
    public DataDictionary(DataDictionary source) {

        copyFrom(source);
    }

    private void setVersion(String beginString) {

        this.beginString = beginString;
        hasVersion = true;
    }

    /**
     * Get the FIX version associated with this dictionary.
     *
     * @return the FIX version
     */
    public String getVersion() {

        return beginString;
    }

    private void addField(int field) {

        fields.add(field);
    }

    private void addFieldName(int field, String name) throws ConfigError {

        if (names.put(name, field) != null) {
            throw new ConfigError("Field named " + name
                    + " defined multiple times");
        }
        fieldNames.put(field, name);
    }

    /**
     * Get the field name for a specified tag.
     *
     * @param field the tag
     * @return the field name
     */
    public String getFieldName(int field) {

        return fieldNames.get(field);
    }

    private void addValueName(int field, String value, String name) {

        valueNames.put(new IntStringPair(field, value), name);
    }

    /**
     * Get the value name, if any, for an enumerated field value.
     *
     * @param field the tag
     * @param value the value
     * @return the value's name
     */
    public String getValueName(int field, String value) {

        return valueNames.get(new IntStringPair(field, value));
    }

    /**
     * Predicate for determining if a tag is a defined field.
     *
     * @param field the tag
     * @return true if the field is defined, false otherwise
     */
    public boolean isField(int field) {

        return fields.contains(field);
    }

    /**
     * Return the field type for a field.
     *
     * @param field the tag
     * @return the field type
     */
    public FieldType getFieldTypeEnum(int field) {

        return fieldTypes.get(field);
    }

    private void addMsgType(String msgType, String msgName) {

        messages.add(msgType);
        if (msgName != null) {
            messageTypeForName.put(msgName, msgType);
        }
    }

    /**
     * Return the message type for the specified name.
     *
     * @param msgName The message name.
     * @return the message type
     */
    public String getMsgType(String msgName) {

        return messageTypeForName.get(msgName);
    }

    /**
     * Predicate for determining if message type is valid for a specified FIX
     * version.
     *
     * @param msgType the message type value
     * @return true if the message type if defined, false otherwise
     */
    public boolean isMsgType(String msgType) {

        return messages.contains(msgType);
    }

    /**
     * Predicate for determining if a message is in the admin category.
     *
     * @param msgType the messageType
     * @return true, if the msgType is a AdminMessage false, if the msgType is a
     *         ApplicationMessage
     */
    public boolean isAdminMessage(String msgType) {

        // Categories are interned
        return MESSAGE_CATEGORY_ADMIN.equals(messageCategory.get(msgType));
    }

    /**
     * Predicate for determining if a message is in the app category.
     *
     * @param msgType the messageType
     * @return true, if the msgType is a ApplicationMessage false, if the
     *         msgType is a AdminMessage
     */
    public boolean isAppMessage(String msgType) {

        // Categories are interned
        return MESSAGE_CATEGORY_APP.equals(messageCategory.get(msgType));
    }

    private void addMsgField(String msgType, int field) {

        Set<Integer> fields = messageFields.get(msgType);
        if (fields == null) {
            fields = new HashSet<Integer>();
            messageFields.put(msgType, fields);
        }
        fields.add(field);
    }

    /**
     * Predicate for determining if a field is valid for a given message type.
     *
     * @param msgType the message type
     * @param field the tag
     * @return true if field is defined for message, false otherwise.
     */
    public boolean isMsgField(String msgType, int field) {

        final Set<Integer> fields = messageFields.get(msgType);
        return fields != null && fields.contains(field);
    }

    /**
     * Predicate for determining if field is a header field.
     *
     * @param field the tag
     * @return true if field is a header field, false otherwise.
     */
    public boolean isHeaderField(int field) {

        Set<Integer> fields = messageFields.get(HEADER_ID);
        return fields != null && fields.contains(field);
    }

    /**
     * Predicate for determining if field is a trailer field.
     *
     * @param field the tag
     * @return true if field is a trailer field, false otherwise.
     */
    public boolean isTrailerField(int field) {

        Set<Integer> fields = messageFields.get(TRAILER_ID);
        return fields != null && fields.contains(field);
    }

    private void addFieldType(int field, FieldType fieldType) {

        fieldTypes.put(field, fieldType);
    }

    /**
     * Get the field type for a field.
     *
     * @param field a tag
     * @return the field type
     * @see #getFieldTypeEnum
     */
    public int getFieldType(int field) {

        return getFieldTypeEnum(field).getOrdinal();
    }

    /**
     * Get the field tag given a field name.
     *
     * @param name the field name
     * @return the tag
     */
    public int getFieldTag(String name) {

        final Integer tag = names.get(name);
        return tag != null ? tag : -1;
    }

    private void addRequiredField(String msgType, int field) {

        Set<Integer> fields = requiredFields.get(msgType);
        if (fields == null) {
            fields = new HashSet<Integer>();
            requiredFields.put(msgType, fields);
        }
        fields.add(field);
    }

    /**
     * Predicate for determining if a field is required for a message type
     *
     * @param msgType the message type
     * @param field the tag
     * @return true if field is required, false otherwise
     */
    public boolean isRequiredField(String msgType, int field) {

        final Set<Integer> fields = requiredFields.get(msgType);
        return fields != null && fields.contains(field);
    }

    /**
     * Predicate for determining if a header field is a required field
     *
     * @param field the tag
     * @return true if field s required, false otherwise
     */
    public boolean isRequiredHeaderField(int field) {

        return isRequiredField(HEADER_ID, field);
    }

    /**
     * Predicate for determining if a trailer field is a required field
     *
     * @param field the tag
     * @return true if field s required, false otherwise
     */
    public boolean isRequiredTrailerField(int field) {

        return isRequiredField(TRAILER_ID, field);
    }

    private void addFieldValue(int field, String value) {

        Set<String> values = fieldValues.get(field);
        if (values == null) {
            values = new HashSet<String>();
            fieldValues.put(field, values);
        }
        values.add(value);
    }

    /**
     * Predicate for determining if a field has enumerated values.
     *
     * @param field the tag
     * @return true if field is enumerated, false otherwise
     */
    public boolean hasFieldValue(int field) {

        final Set<String> values = fieldValues.get(field);
        return values != null && values.size() > 0;
    }

    /**
     * Predicate for determining if a field value is valid
     *
     * @param field the tag
     * @param value a possible field value
     * @return true if field value is valid, false otherwise
     */
    public boolean isFieldValue(int field, String value) {

        final Set<String> validValues = fieldValues.get(field);

        if (validValues == null || validValues.size() == 0) {
            return false;
        }

        if (validValues.contains(ANY_VALUE)) {
            return true;
        }

        if (!isMultipleValueStringField(field)) {
            return validValues.contains(value);
        }

        // MultipleValueString
        for (String val : value.split(" ")) {
            if (!validValues.contains(val)) {
                return false;
            }
        }

        return true;
    }

    private void addGroup(String msg, int field, int delim,
            DataDictionary dataDictionary) {

        groups.put(new IntStringPair(field, msg), new GroupInfo(delim,
                dataDictionary));
    }

    /**
     * Predicate for determining if a field is a group count field for a message
     * type.
     *
     * @param msg the message type
     * @param field the tag
     * @return true if field starts a repeating group, false otherwise
     */
    public boolean isGroup(String msg, int field) {

        return groups.containsKey(new IntStringPair(field, msg));
    }

    /**
     * Predicate for determining if a field is a header group count field
     *
     * @param field the tag
     * @return true if field starts a repeating group, false otherwise
     */
    public boolean isHeaderGroup(int field) {

        return groups.containsKey(new IntStringPair(field, HEADER_ID));
    }

    /**
     * Get repeating group metadata.
     *
     * @param msg the message type
     * @param field the tag
     * @return an object containing group-related metadata
     */
    public GroupInfo getGroup(String msg, int field) {

        return groups.get(new IntStringPair(field, msg));
    }

    /**
     * Predicate for determining if a field is a FIX raw data field.
     *
     * @param field the tag
     * @return true if field is a raw data field, false otherwise
     */
    public boolean isDataField(int field) {

        return fieldTypes.get(field) == FieldType.Data;
    }

    private boolean isMultipleValueStringField(int field) {

        return fieldTypes.get(field) == FieldType.MultipleValueString;
    }

    /**
     * Controls whether out of order fields are checked.
     *
     * @param flag true = checked, false = not checked
     */
    public void setCheckFieldsOutOfOrder(boolean flag) {

        checkFieldsOutOfOrder = flag;
    }

    public boolean isCheckFieldsOutOfOrder() {

        return checkFieldsOutOfOrder;
    }

    public boolean isCheckUnorderedGroupFields() {

        return checkUnorderedGroupFields;
    }

    /**
     * Controls whether group fields are in the same order
     *
     * @param flag true = checked, false = not checked
     */
    public void setCheckUnorderedGroupFields(boolean flag) {

        checkUnorderedGroupFields = flag;
        for (GroupInfo gi : groups.values()) {
            gi.getDataDictionary().setCheckUnorderedGroupFields(flag);
        }
    }

    /**
     * Controls whether empty field values are checked.
     *
     * @param flag true = checked, false = not checked
     */
    public void setCheckFieldsHaveValues(boolean flag) {

        checkFieldsHaveValues = flag;
        for (GroupInfo gi : groups.values()) {
            gi.getDataDictionary().setCheckFieldsHaveValues(flag);
        }
    }

    /**
     * Controls whether user defined fields are checked.
     *
     * @param flag true = checked, false = not checked
     */
    public void setCheckUserDefinedFields(boolean flag) {

        checkUserDefinedFields = flag;
        for (GroupInfo gi : groups.values()) {
            gi.getDataDictionary().setCheckUserDefinedFields(flag);
        }
    }

    public void setAllowUnknownMessageFields(boolean allowUnknownFields) {

        allowUnknownMessageFields = allowUnknownFields;
        for (GroupInfo gi : groups.values()) {
            gi.getDataDictionary().setAllowUnknownMessageFields(
                    allowUnknownFields);
        }
    }

    private void copyFrom(DataDictionary rhs) {

        hasVersion = rhs.hasVersion;
        beginString = rhs.beginString;
        checkFieldsOutOfOrder = rhs.checkFieldsOutOfOrder;
        checkFieldsHaveValues = rhs.checkFieldsHaveValues;
        checkUserDefinedFields = rhs.checkUserDefinedFields;

        copyMap(messageFields, rhs.messageFields);
        copyMap(requiredFields, rhs.requiredFields);
        copyCollection(messages, rhs.messages);
        copyCollection(fields, rhs.fields);
        copyMap(fieldTypes, rhs.fieldTypes);
        copyMap(fieldValues, rhs.fieldValues);
        copyMap(fieldNames, rhs.fieldNames);
        copyMap(names, rhs.names);
        copyMap(valueNames, rhs.valueNames);
        copyMap(groups, rhs.groups);
    }

    @SuppressWarnings("unchecked")
    private <K, V> void copyMap(Map<K, V> lhs, Map<K, V> rhs) {

        lhs.clear();
        for (Map.Entry<K, V> entry : rhs.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Collection) {
                Collection<V> copy;
                try {
                    copy = (Collection<V>) value.getClass().newInstance();
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final java.lang.Exception e) {
                    throw new RuntimeException(e);
                }
                copyCollection(copy, (Collection<V>) value);
                value = copy;
            }
            lhs.put(entry.getKey(), (V) value);
        }
    }

    private <V> void copyCollection(Collection<V> lhs, Collection<V> rhs) {

        lhs.clear();
        lhs.addAll(rhs);
    }

    /**
     * Validate a mesasge, including the header and trailer fields.
     *
     * @param message the message
     * @throws IncorrectTagValue if a field value is not valid
     * @throws FieldNotFound if a field cannot be found
     * @throws IncorrectDataFormat
     */
    public void validate(Message message) throws IncorrectTagValue,
            FieldNotFound, IncorrectDataFormat {

        validate(message, false);
    }

    /**
     * Validate the message body, with header and trailer fields being validated
     * conditionally.
     *
     * @param message the message
     * @param bodyOnly whether to validate just the message body, or to validate
     *        the header and trailer sections as well.
     * @throws IncorrectTagValue if a field value is not valid
     * @throws FieldNotFound if a field cannot be found
     * @throws IncorrectDataFormat
     */
    public void validate(Message message, boolean bodyOnly)
            throws IncorrectTagValue, FieldNotFound, IncorrectDataFormat {

        validate(message, bodyOnly ? null : this, this);
    }

    static void validate(Message message, DataDictionary sessionDataDictionary,
            DataDictionary applicationDataDictionary) throws IncorrectTagValue,
            FieldNotFound, IncorrectDataFormat {

        final boolean bodyOnly = sessionDataDictionary == null;

        if (isVersionSpecified(sessionDataDictionary)
                && !sessionDataDictionary.getVersion().equals(
                        message.getHeader().getString(BeginString.FIELD))
                && !message.getHeader().getString(BeginString.FIELD)
                        .equals("FIXT.1.1")
                && !sessionDataDictionary.getVersion().equals("FIX.5.0")) {
            throw new UnsupportedVersion("Message version '"
                    + message.getHeader().getString(BeginString.FIELD)
                    + "' does not match the data dictionary version '"
                    + sessionDataDictionary.getVersion() + "'");
        }

        if (!message.hasValidStructure() && message.getException() != null) {
            throw message.getException();
        }

        final String msgType = message.getHeader().getString(MsgType.FIELD);
        if (isVersionSpecified(applicationDataDictionary)) {
            applicationDataDictionary.checkMsgType(msgType);
            applicationDataDictionary.checkHasRequired(message.getHeader(),
                    message, message.getTrailer(), msgType, bodyOnly);
        }

        if (!bodyOnly) {
            sessionDataDictionary.iterate(message.getHeader(), HEADER_ID,
                    sessionDataDictionary);
            sessionDataDictionary.iterate(message.getTrailer(), TRAILER_ID,
                    sessionDataDictionary);
        }

        applicationDataDictionary.iterate(message, msgType,
                applicationDataDictionary);
    }

    private static boolean isVersionSpecified(DataDictionary dd) {

        return dd != null && dd.hasVersion;
    }

    private void iterate(FieldMap map, String msgType, DataDictionary dd)
            throws IncorrectTagValue, IncorrectDataFormat {

        final Iterator<Field<?>> iterator = map.iterator();
        while (iterator.hasNext()) {
            final StringField field = (StringField) iterator.next();

            checkHasValue(field);

            if (hasVersion) {
                checkValidFormat(field);
                checkValue(field);
            }

            if (beginString != null && shouldCheckTag(field)) {
                dd.checkValidTagNumber(field);
                if (map instanceof Message) {
                    checkIsInMessage(field, msgType);
                }
                dd.checkGroupCount(field, map, msgType);
            }
        }

        for (final List<Group> groups : map.getGroups().values()) {
            for (final Group group : groups) {
                iterate(group, msgType,
                        dd.getGroup(msgType, group.getFieldTag())
                                .getDataDictionary());
            }
        }
    }

    // / Check if message type is defined in spec.
    private void checkMsgType(String msgType) {

        if (!isMsgType(msgType)) {
            // It would be better to include the msgType in exception message
            // Doing that will break acceptance tests
            throw new FieldException(SessionRejectReason.INVALID_MSGTYPE);
        }
    }

    // / If we need to check for the tag in the dictionary
    private boolean shouldCheckTag(Field<?> field) {

        return checkUserDefinedFields
                || field.getField() < USER_DEFINED_TAG_MIN;
    }

    // / Check if field tag number is defined in spec.
    void checkValidTagNumber(Field<?> field) {

        if (!fields.contains(Integer.valueOf(field.getTag()))) {
            throw new FieldException(SessionRejectReason.INVALID_TAG_NUMBER,
                    field.getField());
        }
    }

    private void checkValidFormat(StringField field) throws IncorrectDataFormat {

        try {
            final FieldType fieldType = getFieldTypeEnum(field.getTag());
            if (fieldType == FieldType.String) {
                // String
            } else if (fieldType == FieldType.Char) {
                if (beginString.compareTo(FixVersions.BEGINSTRING_FIX41) > 0) {
                    CharConverter.convert(field.getValue());
                } else {
                    // String, for older FIX versions
                }
            } else if (fieldType == FieldType.Price) {
                DoubleConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Int) {
                IntConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Amt) {
                DoubleConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Qty) {
                DoubleConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Qty) {
                // String
            } else if (fieldType == FieldType.MultipleValueString) {
                // String
            } else if (fieldType == FieldType.Exchange) {
                // String
            } else if (fieldType == FieldType.Boolean) {
                BooleanConverter.convert(field.getValue());
            } else if (fieldType == FieldType.LocalMktDate) {
                // String
            } else if (fieldType == FieldType.Data) {
                // String
            } else if (fieldType == FieldType.Float) {
                DoubleConverter.convert(field.getValue());
            } else if (fieldType == FieldType.PriceOffset) {
                DoubleConverter.convert(field.getValue());
            } else if (fieldType == FieldType.MonthYear) {
                // String
            } else if (fieldType == FieldType.DayOfMonth) {
                // String
            } else if (fieldType == FieldType.UtcDate) {
                UtcDateOnlyConverter.convert(field.getValue());
            } else if (fieldType == FieldType.UtcTimeOnly) {
                UtcTimeOnlyConverter.convert(field.getValue());
            } else if (fieldType == FieldType.UtcTimeStamp
                    || fieldType == FieldType.Time) {
                UtcTimestampConverter.convert(field.getValue());
            } else if (fieldType == FieldType.NumInGroup) {
                IntConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Percentage) {
                DoubleConverter.convert(field.getValue());
            } else if (fieldType == FieldType.SeqNum) {
                IntConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Length) {
                IntConverter.convert(field.getValue());
            } else if (fieldType == FieldType.Country) {
                // String
            }
        } catch (final FieldConvertError e) {
            throw new IncorrectDataFormat(field.getTag(), field.getValue());
        }
    }

    private void checkValue(StringField field) throws IncorrectTagValue {

        final int tag = field.getField();
        if (!hasFieldValue(tag)) {
            return;
        }

        final String value = field.getValue();
        if (!isFieldValue(tag, value)) {
            throw new IncorrectTagValue(tag);
        }
    }

    // / Check if a field has a value.
    private void checkHasValue(StringField field) {

        if (checkFieldsHaveValues && field.getValue().length() == 0) {
            throw new FieldException(
                    SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE,
                    field.getField());
        }
    }

    // / Check if a field is in this message type.
    private void checkIsInMessage(Field<?> field, String msgType) {

        if (!isMsgField(msgType, field.getField())
                && !allowUnknownMessageFields) {
            throw new FieldException(
                    SessionRejectReason.TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE,
                    field.getField());
        }
    }

    // / Check if group count matches number of groups in
    private void checkGroupCount(StringField field, FieldMap fieldMap,
            String msgType) {

        final int fieldNum = field.getField();
        if (isGroup(msgType, fieldNum)) {
            if (fieldMap.getGroupCount(fieldNum) != Integer.parseInt(field
                    .getValue())) {
                throw new FieldException(
                        SessionRejectReason.INCORRECT_NUMINGROUP_COUNT_FOR_REPEATING_GROUP,
                        fieldNum);
            }
        }
    }

    // / Check if a message has all required fields.
    void checkHasRequired(FieldMap header, FieldMap body, FieldMap trailer,
            String msgType, boolean bodyOnly) {

        if (!bodyOnly) {
            checkHasRequired(HEADER_ID, header, bodyOnly);
            checkHasRequired(TRAILER_ID, trailer, bodyOnly);
        }

        checkHasRequired(msgType, body, bodyOnly);
    }

    private void checkHasRequired(String msgType, FieldMap fields,
            boolean bodyOnly) {

        final Set<Integer> requiredFieldsForMessage = requiredFields
                .get(msgType);
        if (requiredFieldsForMessage == null
                || requiredFieldsForMessage.size() == 0) {
            return;
        }

        for (int field : requiredFieldsForMessage) {
            if (!fields.isSetField(field)) {
                throw new FieldException(
                        SessionRejectReason.REQUIRED_TAG_MISSING, field);
            }
        }

        final Map<Integer, List<Group>> groups = fields.getGroups();
        if (groups.size() > 0) {
            for (Map.Entry<Integer, List<Group>> entry : groups.entrySet()) {
                final GroupInfo p = getGroup(msgType, entry.getKey());
                if (p != null) {
                    for (Group groupInstance : entry.getValue()) {
                        p.getDataDictionary()
                                .checkHasRequired(groupInstance, groupInstance,
                                        groupInstance, msgType, bodyOnly);
                    }
                }
            }
        }
    }

    private void read(String location) throws ConfigError {

        final InputStream inputStream = FileUtil.open(getClass(), location,
                URL, FILESYSTEM, CONTEXT_RESOURCE, CLASSLOADER_RESOURCE);
        if (inputStream == null) {
            throw new DataDictionary.Exception(
                    "Could not find data dictionary: " + location);
        }

        try {
            loadJaxb(inputStream);
        } catch (final java.lang.Exception e) {
            throw new ConfigError(location + ": " + e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
                throw new ConfigError(e);
            }
        }
    }

    private void loadJaxb(InputStream inputStream) throws ConfigError {

        DataDictionaryConfig config = null;

        try {

            SchemaFactory sf = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Schema schema = sf.newSchema(getClass().getResource(
                    "/META-INF/xsd/fix-dictionary.xsd"));

            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

            Unmarshaller unmarshaller = jc.createUnmarshaller();

            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(new DefaultValidationEventHandler());

            config = unmarshaller.unmarshal(new StreamSource(inputStream),
                    DataDictionaryConfig.class).getValue();

        } catch (SAXException e) {

            throw new ConfigError("Could not load data dictionary schema", e);

        } catch (JAXBException e) {

            throw new ConfigError("Could not bind data dictionary file", e);
        }

        if (config == null) {

            throw new ConfigError(
                    "Could not parse data dictionary file - no <fix> node found at root ");
        }

        String dictionaryType = String.format("%s.%s.%s", config.getType(),
                config.getMajor(), config.getMinor());

        setVersion(dictionaryType);

        // FIELDS
        Fields fieldsList = config.getFields();

        if (fieldsList.getField().isEmpty()) {

            throw new ConfigError("No fields defined");
        }

        for (FieldDefinition fieldNode : fieldsList.getField()) {

            int num = fieldNode.getNumber();

            String type = fieldNode.getType().value();

            addField(num);
            addFieldType(num, FieldType.fromName(getVersion(), type));
            addFieldName(num, fieldNode.getName());

            for (FieldDefinition.Value valueNode : fieldNode.getValue()) {

                addFieldValue(num, valueNode.getEnum());

                if (valueNode.getDescription() != null) {

                    addValueName(num, valueNode.getEnum(),
                            valueNode.getDescription());
                }
            }

            if (!fieldNode.getValue().isEmpty()
                    && fieldNode.isAllowOtherValues()) {

                addFieldValue(num, ANY_VALUE);
            }
        }

        if (beginString.startsWith(FIXT_PREFIX)
                || beginString.compareTo(FixVersions.FIX50) < 0) {

            // HEADER
            loadJaxb(config, config.getHeader(), HEADER_ID);

            // TRAILER
            loadJaxb(config, config.getTrailer(), TRAILER_ID);
        }

        // MSGTYPE
        Messages messages = config.getMessages();

        for (MessageDefinition messageNode : messages.getMessage()) {

            String msgtype = messageNode.getMsgtype();
            String msgcat = messageNode.getMsgcat().value();

            messageCategory.put(msgtype, msgcat);

            addMsgType(msgtype, messageNode.getName());
            addValueName(MsgType.FIELD, msgtype, messageNode.getName());

            loadJaxb(config, messageNode);
        }
    }

    private void loadJaxb(DataDictionaryConfig config,
            DataDictionarySubsection subsection, String msgtype)
            throws ConfigError {

        for (MessageParticle particle : subsection.getParticles()) {

            if ((particle instanceof FieldReference)
                    || (particle instanceof GroupDefinition)) {

                final int num = names.get(particle.getName());

                addMsgField(msgtype, num);

                if (particle.getRequired() == EnumBoolean.Y) {

                    addRequiredField(msgtype, num);
                }

            } else if (particle instanceof ComponentReference) {

                addXMLComponentFields(config, (ComponentReference) particle,
                        msgtype, this, particle.getRequired() == EnumBoolean.Y);
            }

            if (particle instanceof GroupDefinition) {

                addXMLGroup(config, (GroupDefinition) particle, msgtype, this,
                        particle.getRequired() == EnumBoolean.Y);
            }
        }
    }

    private void loadJaxb(DataDictionaryConfig config,
            MessageDefinition messageNode) throws ConfigError {

        for (MessageParticle particle : messageNode.getParticles()) {

            if ((particle instanceof FieldReference)
                    || (particle instanceof GroupDefinition)) {

                String name = particle.getName();

                final int num = names.get(name);

                addMsgField(messageNode.getMsgtype(), num);

                EnumBoolean required = particle.getRequired();

                if (required == EnumBoolean.Y) {

                    addRequiredField(messageNode.getMsgtype(), num);
                }

            } else if (particle instanceof ComponentReference) {

                addXMLComponentFields(config, (ComponentReference) particle,
                        messageNode.getMsgtype(), this,
                        particle.getRequired() == EnumBoolean.Y);
            }

            if (particle instanceof GroupDefinition) {

                addXMLGroup(config, (GroupDefinition) particle,
                        messageNode.getMsgtype(), this,
                        particle.getRequired() == EnumBoolean.Y);
            }
        }
    }

    private void addXMLGroup(DataDictionaryConfig config,
            GroupDefinition group, String msgtype, DataDictionary dd,
            boolean groupRequired) throws ConfigError {

        String name = group.getName();

        final int num = names.get(name);

        int delim = 0;
        int field = 0;

        final DataDictionary groupDD = new DataDictionary();
        groupDD.setVersion(dd.getVersion());

        for (MessageParticle particle : group.getParticles()) {

            if (particle instanceof FieldReference) {

                field = names.get(particle.getName());
                groupDD.addField(field);
                EnumBoolean required = particle.getRequired();
                if (required == EnumBoolean.Y && groupRequired) {
                    groupDD.addRequiredField(msgtype, field);
                }

            } else if (particle instanceof ComponentReference) {

                field = addXMLComponentFields(config,
                        (ComponentReference) particle, msgtype, groupDD, false);

            } else if (particle instanceof GroupDefinition) {

                field = names.get(particle.getName());
                groupDD.addField(field);

                EnumBoolean required = particle.getRequired();

                if (required == EnumBoolean.Y && groupRequired) {

                    groupDD.addRequiredField(msgtype, field);
                }

                addXMLGroup(config, (GroupDefinition) particle, msgtype,
                        groupDD, required == EnumBoolean.Y);
            }

            if (delim == 0) {

                delim = field;
            }
        }

        if (delim != 0) {

            dd.addGroup(msgtype, num, delim, groupDD);
        }
    }

    private int addXMLComponentFields(DataDictionaryConfig config,
            ComponentReference componentReference, String msgtype,
            DataDictionary dd, boolean componentRequired) throws ConfigError {

        int firstField = 0;

        ComponentDefinition component = findComponent(config,
                componentReference.getName());

        for (MessageParticle particle : component.getParticles()) {

            if ((particle instanceof FieldReference)
                    || (particle instanceof GroupDefinition)) {

                final int field = names.get(particle.getName());

                if (firstField == 0) {
                    firstField = field;
                }

                if (particle.getRequired() == EnumBoolean.Y
                        && componentRequired) {
                    addRequiredField(msgtype, field);
                }

                dd.addField(field);
                dd.addMsgField(msgtype, field);
            }

            if (particle instanceof GroupDefinition) {

                addXMLGroup(config, (GroupDefinition) particle, msgtype, dd,
                        particle.getRequired() == EnumBoolean.Y);

            } else if (particle instanceof ComponentReference) {

                addXMLComponentFields(config, (ComponentReference) particle,
                        msgtype, dd, particle.getRequired() == EnumBoolean.Y);
            }
        }

        return firstField;
    }

    private ComponentDefinition findComponent(DataDictionaryConfig config,
            String name) throws ConfigError {

        if (config.getComponents() != null) {

            for (ComponentDefinition component : config.getComponents()
                    .getComponent()) {

                if (component.getName().equals(name)) {

                    return component;
                }
            }
        }

        throw new ConfigError("No component for reference name " + name);
    }

    public int[] getOrderedFields() {

        if (orderedFieldsArray == null) {
            orderedFieldsArray = new int[fields.size()];

            final Iterator<Integer> fieldItr = fields.iterator();
            int i = 0;
            while (fieldItr.hasNext()) {
                orderedFieldsArray[i++] = fieldItr.next();
            }
        }

        return orderedFieldsArray;
    }

    /**
     * Data dictionary-related exception.
     */
    public static class Exception extends RuntimeException {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        public Exception(Throwable cause) {

            super(cause);
        }

        public Exception(String message) {

            super(message);
        }
    }

    private static final class IntStringPair {

        private final int intValue;

        private final String stringValue;

        public IntStringPair(int value, String value2) {

            intValue = value;
            stringValue = value2;
        }

        // public int getIntValue() {
        // return intValue;
        // }

        // public String getStringValue() {
        // return stringValue;
        // }

        @Override
        public boolean equals(Object other) {

            return this == other || other instanceof IntStringPair
                    && intValue == ((IntStringPair) other).intValue
                    && stringValue.equals(((IntStringPair) other).stringValue);
        }

        @Override
        public int hashCode() {

            return stringValue.hashCode() + intValue;
        }

        /**
         * For debugging
         */
        @Override
        public String toString() {

            final StringBuilder b = new StringBuilder();
            b.append('(').append(intValue).append(',').append(stringValue)
                    .append(')');
            return b.toString();
        }
    }

    /**
     * Contains meta-data for FIX repeating groups
     */
    public static final class GroupInfo {

        private final int delimiterField;

        private final DataDictionary dataDictionary;

        private GroupInfo(int field, DataDictionary dictionary) {

            delimiterField = field;
            dataDictionary = dictionary;
        }

        public DataDictionary getDataDictionary() {

            return dataDictionary;
        }

        /**
         * Returns the delimiter field used to start a repeating group instance.
         *
         * @return delimiter field
         * @deprecated use getDelimiterField() instead
         */
        @Deprecated
        public int getDelimeterField() {

            return delimiterField;
        }

        /**
         * Returns the delimiter field used to start a repeating group instance.
         *
         * @return delimiter field
         */
        public int getDelimiterField() {

            return delimiterField;
        }

        @Override
        public boolean equals(Object other) {

            return this == other
                    || other instanceof GroupInfo
                    && delimiterField == ((GroupInfo) other).delimiterField
                    && dataDictionary
                            .equals(((GroupInfo) other).dataDictionary);
        }

        @Override
        public int hashCode() {

            return delimiterField;
        }
    }
}
