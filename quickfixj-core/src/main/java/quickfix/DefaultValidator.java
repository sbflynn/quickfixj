/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 13 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import java.util.Set;

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXFieldType;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.FIXMessage;
import org.quickfixj.FIXMessageHeader;
import org.quickfixj.FIXMessageTrailer;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.GraphDictionary;
import org.quickfixj.engine.GraphDictionary.FieldDictionary;
import org.quickfixj.engine.GraphDictionary.GroupDictionary;
import org.quickfixj.engine.SessionRejectReason;
import org.quickfixj.engine.Validator;
import org.quickfixj.field.BooleanConverter;
import org.quickfixj.field.CharConverter;
import org.quickfixj.field.DoubleConverter;
import org.quickfixj.field.FieldConversionException;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.IntConverter;
import org.quickfixj.field.UtcDateOnlyConverter;
import org.quickfixj.field.UtcTimeOnlyConverter;
import org.quickfixj.field.UtcTimestampConverter;

/**
 * DefaultValidator - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class DefaultValidator implements Validator {

	private static final int USER_DEFINED_TAG_MIN = 5000;

	private final FIXBeginString beginString;

	private boolean checkFieldsOutOfOrder = true;

	private boolean checkFieldsHaveValues = true;

	private boolean checkUserDefinedFields = true;

	private boolean checkUnorderedGroupFields = true;

	private boolean allowUnknownMessageFields = false;

	public DefaultValidator(FIXBeginString beginString) {
		this.beginString = beginString;
	}

	public DefaultValidator(FIXSessionID sessionID, SessionSettings settings)
			throws FieldConversionException, ConfigError {

		this(sessionID.getBeginString());

		if (settings.isSetting(sessionID,
				Session.SETTING_VALIDATE_FIELDS_OUT_OF_ORDER)) {
			checkFieldsOutOfOrder = settings.getBool(sessionID,
					Session.SETTING_VALIDATE_FIELDS_OUT_OF_ORDER);
		}

		if (settings.isSetting(sessionID,
				Session.SETTING_VALIDATE_FIELDS_HAVE_VALUES)) {
			checkFieldsHaveValues = settings.getBool(sessionID,
					Session.SETTING_VALIDATE_FIELDS_HAVE_VALUES);
		}

		if (settings.isSetting(sessionID,
				Session.SETTING_VALIDATE_USER_DEFINED_FIELDS)) {
			checkUserDefinedFields = settings.getBool(sessionID,
					Session.SETTING_VALIDATE_USER_DEFINED_FIELDS);
		}

		if (settings.isSetting(sessionID,
				Session.SETTING_VALIDATE_UNORDERED_GROUP_FIELDS)) {
			checkUnorderedGroupFields = settings.getBool(sessionID,
					Session.SETTING_VALIDATE_UNORDERED_GROUP_FIELDS);
		}

		if (settings.isSetting(sessionID,
				Session.SETTING_ALLOW_UNKNOWN_MSG_FIELDS)) {
			allowUnknownMessageFields = settings.getBool(sessionID,
					Session.SETTING_ALLOW_UNKNOWN_MSG_FIELDS);
		}
	}

	/**
	 * Get the checkFieldsOutOfOrder property.
	 *
	 * @return Returns the checkFieldsOutOfOrder.
	 * @since 2.0
	 */
	@Override
	public boolean isCheckFieldsOutOfOrder() {
		return checkFieldsOutOfOrder;
	}

	/**
	 * Set the checkFieldsOutOfOrder property.
	 *
	 * @param checkFieldsOutOfOrder
	 *            The checkFieldsOutOfOrder to set.
	 * @since 2.0
	 */
	public void setCheckFieldsOutOfOrder(boolean checkFieldsOutOfOrder) {
		this.checkFieldsOutOfOrder = checkFieldsOutOfOrder;
	}

	/**
	 * Get the checkFieldsHaveValues property.
	 *
	 * @return Returns the checkFieldsHaveValues.
	 * @since 2.0
	 */
	public boolean isCheckFieldsHaveValues() {
		return checkFieldsHaveValues;
	}

	/**
	 * Set the checkFieldsHaveValues property.
	 *
	 * @param checkFieldsHaveValues
	 *            The checkFieldsHaveValues to set.
	 * @since 2.0
	 */
	public void setCheckFieldsHaveValues(boolean checkFieldsHaveValues) {
		this.checkFieldsHaveValues = checkFieldsHaveValues;
	}

	/**
	 * Get the checkUserDefinedFields property.
	 *
	 * @return Returns the checkUserDefinedFields.
	 * @since 2.0
	 */
	public boolean isCheckUserDefinedFields() {
		return checkUserDefinedFields;
	}

	/**
	 * Set the checkUserDefinedFields property.
	 *
	 * @param checkUserDefinedFields
	 *            The checkUserDefinedFields to set.
	 * @since 2.0
	 */
	public void setCheckUserDefinedFields(boolean checkUserDefinedFields) {
		this.checkUserDefinedFields = checkUserDefinedFields;
	}

	/**
	 * Get the checkUnorderedGroupFields property.
	 *
	 * @return Returns the checkUnorderedGroupFields.
	 * @since 2.0
	 */
	@Override
	public boolean isCheckUnorderedGroupFields() {
		return checkUnorderedGroupFields;
	}

	/**
	 * Set the checkUnorderedGroupFields property.
	 *
	 * @param checkUnorderedGroupFields
	 *            The checkUnorderedGroupFields to set.
	 * @since 2.0
	 */
	public void setCheckUnorderedGroupFields(boolean checkUnorderedGroupFields) {
		this.checkUnorderedGroupFields = checkUnorderedGroupFields;
	}

	/**
	 * Get the allowUnknownMessageFields property.
	 *
	 * @return Returns the allowUnknownMessageFields.
	 * @since 2.0
	 */
	public boolean isAllowUnknownMessageFields() {
		return allowUnknownMessageFields;
	}

	/**
	 * Set the allowUnknownMessageFields property.
	 *
	 * @param allowUnknownMessageFields
	 *            The allowUnknownMessageFields to set.
	 * @since 2.0
	 */
	public void setAllowUnknownMessageFields(boolean allowUnknownMessageFields) {
		this.allowUnknownMessageFields = allowUnknownMessageFields;
	}

	/**
	 * Validate a mesasge, including the header and trailer fields.
	 *
	 * @param message
	 *            the message
	 * @throws IncorrectTagValue
	 *             if a field value is not valid
	 * @throws FieldNotFound
	 *             if a field cannot be found
	 * @throws IncorrectDataFormat
	 */
	@Override
	public void validate(FIXMessageDictionary dictionary, FIXMessage message) {

		validate(dictionary, message, false);
	}

	/**
	 * Validate the message body, with header and trailer fields being validated
	 * conditionally.
	 *
	 * @param message
	 *            the message
	 * @param bodyOnly
	 *            whether to validate just the message body, or to validate the
	 *            header and trailer sections as well.
	 * @throws IncorrectTagValue
	 *             if a field value is not valid
	 * @throws FieldNotFound
	 *             if a field cannot be found
	 * @throws IncorrectDataFormat
	 */
	@Override
	public void validate(FIXMessageDictionary dictionary, FIXMessage message,
			boolean bodyOnly) {

		if (message.getParseException() != null) {
			throw message.getParseException();
		}

		final String msgType = message.getHeader().getFieldValue(
				FIXTag.MSG_TYPE);

		// if (isVersionSpecified(applicationDataDictionary)) {
		// applicationDataDictionary.checkMsgType(msgType);
		// applicationDataDictionary.checkHasRequired(message.getHeader(),
		// message,
		// message.getTrailer(), msgType, bodyOnly);
		// }

		if (!bodyOnly) {
			iterate(dictionary, message.getHeader());
			iterate(dictionary, message.getTrailer());
		}

		iterate(dictionary, message);
	}

	/**
	 *
	 * @since 2.0
	 */
	private void iterate(FIXMessageDictionary dictionary, FIXMessage graph) {

		checkHasRequired(dictionary, graph);

		for (FIXField<?> field : graph) {

			FieldDictionary fieldDictionary = dictionary
					.getFieldDictionary(field.getTag());

			checkHasValue(field);

			if (shouldCheckTag(field)) {
				checkValidTagNumber(dictionary, field);
				checkIsInMessage(dictionary, field);
				checkValidFormat(fieldDictionary, field);
				checkValue(fieldDictionary, field);

				if (field instanceof FIXGroupField<?>) {
					FIXGroupField<?> groupField = (FIXGroupField<?>) field;
					GroupDictionary groupDictionary = dictionary
							.getGroupDictionary(groupField.getTag());
					checkGroupCount(groupField);
					for (FIXGroup group : groupField) {
						iterate(dictionary, groupDictionary, group);
					}
				}
			}
		}
	}

	private void iterate(FIXMessageDictionary dictionary,
			FIXMessageHeader header) {

		FIXBeginString msgBeginString = FIXBeginString.parse(header
				.getFieldValue(FIXTag.BEGIN_STRING));

		if (msgBeginString != beginString) {
			throw new UnsupportedVersion("Message version '"
					+ msgBeginString.getValue()
					+ "' does not match the data dictionary version '"
					+ beginString.getValue() + "'");
		}

		for (FIXField<?> field : header) {

			FieldDictionary fieldDictionary = dictionary
					.getFieldDictionary(field.getTag());

			checkHasValue(field);

			checkValidFormat(fieldDictionary, field);
			checkValue(fieldDictionary, field);

			if (shouldCheckTag(field)) {

				if (!dictionary.isHeaderField(field.getTag())) {
					throw new FieldException(
							SessionRejectReason.INVALID_TAG_NUMBER,
							SessionRejectReason
									.getMessage(SessionRejectReason.INVALID_TAG_NUMBER),
							field.getTag());
				}

				if (field instanceof FIXGroupField<?>) {
					FIXGroupField<?> groupField = (FIXGroupField<?>) field;
					GroupDictionary groupDictionary = dictionary
							.getGroupDictionary(groupField.getTag());
					checkGroupCount(groupField);
					for (FIXGroup group : groupField) {
						iterate(dictionary, groupDictionary, group);
					}
				}
			}
		}
	}

	private void iterate(FIXMessageDictionary dictionary,
			FIXMessageTrailer header) {

		for (FIXField<?> field : header) {

			FieldDictionary fieldDictionary = dictionary
					.getFieldDictionary(field.getTag());

			checkHasValue(field);

			checkValidFormat(fieldDictionary, field);
			checkValue(fieldDictionary, field);

			if (shouldCheckTag(field)) {

				if (!dictionary.isTrailerField(field.getTag())) {
					throw new FieldException(
							SessionRejectReason.INVALID_TAG_NUMBER,
							SessionRejectReason
									.getMessage(SessionRejectReason.INVALID_TAG_NUMBER),
							field.getTag());
				}

				if (field instanceof FIXGroupField<?>) {
					FIXGroupField<?> groupField = (FIXGroupField<?>) field;
					GroupDictionary groupDictionary = dictionary
							.getGroupDictionary(groupField.getTag());
					checkGroupCount(groupField);
					for (FIXGroup group : groupField) {
						iterate(dictionary, groupDictionary, group);
					}
				}
			}
		}
	}

	/**
	 * @since 2.0
	 */
	private void iterate(FIXMessageDictionary messageDictionary,
			GroupDictionary dictionary, FIXGroup group) {

		for (FIXField<?> field : group) {

			FieldDictionary fieldDictionary = dictionary
					.getFieldDictionary(field.getTag());

			checkHasValue(field);

			checkValidFormat(fieldDictionary, field);
			checkValue(fieldDictionary, field);

			if (shouldCheckTag(field)) {
				checkValidTagNumber(dictionary, field);
				checkIsInMessage(dictionary, field);

				if (field instanceof FIXGroupField<?>) {
					FIXGroupField<?> groupField = (FIXGroupField<?>) field;
					GroupDictionary groupDictionary = dictionary
							.getGroupDictionary(groupField.getTag());
					checkGroupCount(groupField);
					for (FIXGroup subGroup : groupField) {
						iterate(messageDictionary, groupDictionary, subGroup);
					}
				}
			}
		}

		checkHasRequired(dictionary, group);
	}

	/**
	 *
	 * @since 2.0
	 */
	public void checkValidTagNumber(GraphDictionary dictionary,
			FIXField<?> field) {

		if (!dictionary.isValid(field.getTag()) && !allowUnknownMessageFields) {
			throw new FieldException(
					SessionRejectReason.INVALID_TAG_NUMBER,
					SessionRejectReason
							.getMessage(SessionRejectReason.INVALID_TAG_NUMBER),
					field.getTag());
		}
	}

	/**
	 * @since 2.0
	 */
	private void checkHasRequired(GraphDictionary dictionary,
			FIXFieldGraph graph) {

		if (dictionary.getRequiredFields().isEmpty()) {
			return;
		}

		for (int field : dictionary.getRequiredFields()) {
			if (!graph.isFieldSet(field)) {
				throw new FieldException(
						SessionRejectReason.REQUIRED_TAG_MISSING,
						SessionRejectReason
								.getMessage(SessionRejectReason.REQUIRED_TAG_MISSING),
						field);
			}
		}
	}

	/**
	 * Check if a field has a value.
	 * 
	 * @since 2.0
	 */
	private void checkHasValue(FIXField<?> field) {

		if (checkFieldsHaveValues && field.getCharacters().length() == 0) {
			throw new FieldException(
					SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE,
					SessionRejectReason
							.getMessage(SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE),
					field.getTag());
		}
	}

	private void checkValue(FieldDictionary dictionary, FIXField<?> field)
			throws IncorrectTagValue {

		if (dictionary == null) {
			return;
		}

		Set<String> validValues = dictionary.getFieldValues();

		if (validValues == null || validValues.isEmpty()) {
			return;
		}

		if (dictionary.allowOtherFieldValues()) {
			return;
		}

		final String value = field.getCharacters().toString();
		if (!isFieldValue(dictionary, field, value)) {
			throw new IncorrectTagValue(field.getTag());
		}
	}

	/**
	 * Predicate for determining if a field value is valid
	 *
	 * @param field
	 *            the tag
	 * @param value
	 *            a possible field value
	 * @return true if field value is valid, false otherwise
	 */
	public boolean isFieldValue(FieldDictionary dictionary, FIXField<?> field,
			String value) {

		if (dictionary.getType() != FIXFieldType.MULTIPLESTRINGVALUE) {
			return dictionary.getFieldValues().contains(value);
		}

		// MultipleValueString - every one has to match
		for (String val : value.split(" ")) {
			if (!dictionary.getFieldValues().contains(val)) {
				return false;
			}
		}

		return true;
	}

	private void checkValidFormat(FieldDictionary fieldDictionary,
			FIXField<?> field) {

		try {

			if (fieldDictionary == null || fieldDictionary.getType() == null) {
				return;
			}

			switch (fieldDictionary.getType()) {
			case CHAR:
				if (beginString.gt(FIXBeginString.FIX41)) {
					CharConverter.convert(field.getCharacters().toString());
				}
				break;

			case BOOLEAN:
				BooleanConverter.convert(field.getCharacters().toString());
				break;

			case FLOAT:
			case PRICE:
			case PRICEOFFSET:
			case AMT:
			case QTY:
			case PERCENTAGE:
				DoubleConverter.convert(field.getCharacters().toString());
				break;

			case INT:
			case NUMINGROUP:
			case SEQNUM:
			case LENGTH:
				IntConverter.convert(field.getCharacters().toString());
				break;

			case UTCDATE:
				UtcDateOnlyConverter.convert(field.getCharacters().toString());
				break;

			case UTCTIMEONLY:
				UtcTimeOnlyConverter.convert(field.getCharacters().toString());
				break;

			case UTCTIMESTAMP:
			case TIME:
				UtcTimestampConverter.convert(field.getCharacters().toString());
				break;

			default:
				// all others are valid as strings
				break;
			}

		} catch (final FieldConversionException e) {

			throw new IncorrectDataFormat(field.getTag(), field.getCharacters()
					.toString());
		}
	}

	/**
	 * If we need to check for the tag in the dictionary
	 * 
	 * @since 2.0
	 */
	private boolean shouldCheckTag(FIXField<?> field) {

		return checkUserDefinedFields || field.getTag() < USER_DEFINED_TAG_MIN;
	}

	private void checkIsInMessage(GraphDictionary dictionary, FIXField<?> field) {

		if (!dictionary.isField(field.getTag()) && !allowUnknownMessageFields) {
			throw new FieldException(
					SessionRejectReason.TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE,
					SessionRejectReason
							.getMessage(SessionRejectReason.TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE),
					field.getTag());
		}
	}

	/**
	 * @since 2.0
	 */
	private void checkGroupCount(FIXGroupField<?> field) {

		if (field.size() != field.getValue()) {

			throw new FieldException(
					SessionRejectReason.INCORRECT_NUMINGROUP_COUNT_FOR_REPEATING_GROUP,
					SessionRejectReason
							.getMessage(SessionRejectReason.INCORRECT_NUMINGROUP_COUNT_FOR_REPEATING_GROUP),
					field.getTag());
		}
	}
}
