/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 6 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import java.util.List;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.GraphBuilder;
import org.quickfixj.engine.GraphDictionary;
import org.quickfixj.engine.GraphDictionary.FieldDictionary;
import org.quickfixj.engine.GraphDictionary.GroupDictionary;
import org.quickfixj.engine.SessionRejectReason;
import org.quickfixj.engine.Validator;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.GenericGroupField;
import org.quickfixj.field.IntConverter;

/**
 * MessageParser - parses a string into a {@link FIXMessage}. Intended to be
 * threadsafe iff constructed with threadsafe args.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class MessageParser {

	private final FIXMessageBuilderFactory builderFactory;
	private final FIXMessageDictionaryFactory dataDictionary;
	private final Validator validator;

	MessageParser(FIXMessageBuilderFactory builderFactory,
			FIXMessageDictionaryFactory dataDictionary, Validator validator) {
		this.builderFactory = builderFactory;
		this.dataDictionary = dataDictionary;
		this.validator = validator;
	}

	public FIXMessage parse(FIXApplication application, String input,
			boolean doValidation) {

		MessageTokenIterator tokenIterator = new MessageTokenIterator(input);

		// this should be done more efficiently by the tokenizer
		String msgType = MessageUtils.getMessageType(input);

		FIXMessageDictionary messageDictionary;
		FIXMessageBuilder builder;
		FIXMessage message;

		messageDictionary = dataDictionary.getMessageDictionary(application,
				msgType);

		if (messageDictionary == null) {
			messageDictionary = new GenericMessageDictionaryFactory("UNKNOWN",
					dataDictionary.getBeginString()).getMessageDictionary(
					application, msgType);
			message = new Message("UNKNOWN");
			message.setParseException(new FieldException(
					SessionRejectReason.INVALID_MSGTYPE, SessionRejectReason
							.getMessage(SessionRejectReason.INVALID_MSGTYPE)));
			parseHeader(message, tokenIterator, new GenericMessageBuilder(
					dataDictionary.getBeginString(), messageDictionary),
					messageDictionary, doValidation);
			return message;
		}

		builder = builderFactory.getMessageBuilder(messageDictionary);
		message = builder.create();

		try {

			parseHeader(message, tokenIterator, builder, messageDictionary,
					doValidation);
			parseBody(message, tokenIterator, builder, messageDictionary,
					doValidation);
			parseTrailer(message, tokenIterator, builder, messageDictionary);
			if (doValidation) {
				validateCheckSum(message, tokenIterator);
			}
		} catch (final FieldException e) {
			// TODO - shouldn't really be storing exceptions
			message.setParseException(e);
		}

		return message;
	}

	public <T extends FIXMessage> T parse(FIXApplication application,
			String input, boolean doValidation, T message) {

		MessageTokenIterator tokenIterator = new MessageTokenIterator(input);

		// this should be done more efficiently by the tokenizer
		String msgType = MessageUtils.getMessageType(input);

		FIXMessageDictionary messageDictionary;
		FIXMessageBuilder builder;

		messageDictionary = dataDictionary.getMessageDictionary(application,
				msgType);
		builder = builderFactory.getMessageBuilder(messageDictionary);

		try {
			parseHeader(message, tokenIterator, builder, messageDictionary,
					doValidation);
			parseBody(message, tokenIterator, builder, messageDictionary,
					doValidation);
			parseTrailer(message, tokenIterator, builder, messageDictionary);
			if (doValidation) {
				validateCheckSum(message, tokenIterator);
			}
		} catch (final FieldException e) {
			// TODO - shouldn't really be storing exceptions
			message.setParseException(e);
		}

		return message;
	}

	private void parseHeader(FIXMessage message,
			MessageTokenIterator tokenIterator,
			FIXMessageBuilder messageBuilder,
			FIXMessageDictionary messageDictionary, boolean doValidation) {

		if (doValidation) {
			final boolean validHeaderFieldOrder = isNextField(
					message.getHeader(), tokenIterator, messageBuilder,
					messageDictionary, FIXTag.BEGIN_STRING)
					&& isNextField(message.getHeader(), tokenIterator,
							messageBuilder, messageDictionary,
							FIXTag.BODY_LENGTH)
					&& isNextField(message.getHeader(), tokenIterator,
							messageBuilder, messageDictionary, FIXTag.MSG_TYPE);
			if (!validHeaderFieldOrder) {
				// Invalid message preamble (first three fields) is a serious
				// condition and is handled differently from other message
				// parsing errors.
				throw new InvalidMessage("Header fields out of order in "
						+ tokenIterator.getMessage());
			}
		}

		while (!tokenIterator.finished()
				&& isHeaderField(tokenIterator.getTag(), messageDictionary)) {

			FIXField<?> field = parseNextField(message.getHeader(),
					tokenIterator, messageBuilder, messageDictionary);

			if (field instanceof FIXGroupField<?>) {
				parseGroup(message.getHeader(), tokenIterator, messageBuilder,
						messageDictionary, (FIXGroupField<?>) field);
			}
		}
	}

	private void parseBody(FIXMessage message,
			MessageTokenIterator tokenIterator,
			FIXMessageBuilder messageBuilder,
			FIXMessageDictionary messageDictionary, boolean doValidation) {

		while (!tokenIterator.finished()) {

			if (messageDictionary.isTrailerField(tokenIterator.getTag())) {
				return;
			}

			if (messageDictionary.isHeaderField(tokenIterator.getTag())) {

				// An acceptance test requires the sequence number to
				// be available even if the related field is out of order
				FIXField<?> field = parseNextField(message.getHeader(),
						tokenIterator, messageBuilder, messageDictionary);

				// Group case
				if (field instanceof FIXGroupField<?>) {
					parseGroup(message.getHeader(), tokenIterator,
							messageBuilder, messageDictionary,
							(FIXGroupField<?>) field);
				}
				if (doValidation && validator.isCheckFieldsOutOfOrder())
					throw new FieldException(
							SessionRejectReason.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER,
							SessionRejectReason
									.getMessage(SessionRejectReason.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER),
							field.getTag());
			} else {

				FIXField<?> field = parseNextField(message, tokenIterator,
						messageBuilder, messageDictionary);

				// Group case
				if (field instanceof FIXGroupField<?>) {
					parseGroup(message, tokenIterator, messageBuilder,
							messageDictionary, (FIXGroupField<?>) field);
				}
			}
		}
	}

	private void parseTrailer(FIXMessage message,
			MessageTokenIterator tokenIterator,
			FIXMessageBuilder messageBuilder,
			FIXMessageDictionary messageDictionary) {

		// while (field != null) {
		while (!tokenIterator.finished()) {
			if (!messageDictionary.isTrailerField(tokenIterator.getTag())) {
				throw new FieldException(
						SessionRejectReason.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER,
						SessionRejectReason
								.getMessage(SessionRejectReason.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER),
						tokenIterator.getTag());
			}

			parseNextField(message.getTrailer(), tokenIterator, messageBuilder,
					messageDictionary);
		}
	}

	private FIXField<?> parseNextField(FIXFieldGraph fields,
			MessageTokenIterator iterator, GraphBuilder<?> builder,
			GraphDictionary dictionary) {

		FIXField<?> field = extractField(fields, iterator, builder, dictionary);

		if (fields.isFieldSet(field.getTag())) {
			throw new FieldException(
					SessionRejectReason.TAG_APPEARS_MORE_THAN_ONCE,
					SessionRejectReason
							.getMessage(SessionRejectReason.TAG_APPEARS_MORE_THAN_ONCE),
					field.getTag());
		}
		fields.setField(field);

		// We have consumed the field so lets move on
		iterator.next();

		return field;
	}

	private boolean isNextField(FIXFieldGraph fields,
			MessageTokenIterator iterator, GraphBuilder<?> builder,
			GraphDictionary dictionary, int tag) {

		if (iterator.getTag() != tag) {
			return false;
		}

		FIXField<?> field = extractField(fields, iterator, builder, dictionary);
		fields.setField(field);

		// We have consumed the field so lets move on
		iterator.next();

		return true;
	}

	private FIXField<?> extractField(FIXFieldGraph fields,
			MessageTokenIterator iterator, GraphBuilder<?> builder,
			GraphDictionary dictionary) {

		int tag = iterator.getTag();

		FieldDictionary fieldDictionary = dictionary.getFieldDictionary(tag);

		if (fieldDictionary != null && fieldDictionary.isDataField()) {

			/* Assume length field is 1 less. */
			int lengthField = tag - 1;

			/* Special case for Signature which violates above assumption. */
			if (tag == 89) {
				lengthField = 93;
			}

			try {
				int fieldLength = getInt(fields, lengthField);
				iterator.expandToByteLength(fieldLength);
			} catch (final FieldNotFound e) {
				throw new InvalidMessage("Tag " + e.field + " not found in "
						+ iterator.getMessage());
			}
		}

		FIXField<?> field = builder.create(tag, iterator.getBuffer(),
				iterator.getCurrentSeparator() + 1, iterator.length());

		if (dictionary.isGroupField(tag)
				&& !(field instanceof FIXGroupField<?>)) {
			// builder doesn't know this should be a group field
			// but we do - so lets substitute
			GroupDictionary info = dictionary.getGroupDictionary(tag);

			field = new GenericGroupField<FIXGroup>(tag,
					info.getDelimiterField(), iterator.getBuffer(),
					iterator.getCurrentSeparator() + 1, iterator.length());
		}

		validator.checkValidTagNumber(dictionary, field);

		return field;
	}

	private boolean isHeaderField(int tag, FIXMessageDictionary dd) {

		return dd.isHeaderField(tag);
	}

	private <G extends FIXGroup> void parseGroup(FIXFieldGraph parent,
			MessageTokenIterator iterator, GraphBuilder<?> builder,
			GraphDictionary dictionary, FIXGroupField<G> groupField) {

		GraphBuilder<G> groupBuilder = builder.create(groupField);
		final int firstField = groupField.getDelimiterField();

		GroupDictionary groupDictionary = dictionary
				.getGroupDictionary(groupField.getTag());
		int previousOffset = -1;
		final int groupCountTag = groupField.getTag();
		boolean firstFieldFound = false;
		G group = null;

		while (!iterator.finished()) {

			if (!groupDictionary.isField(iterator.getTag())
					&& !groupBuilder.getFieldBuilders().containsKey(
							iterator.getTag())) {
				return;
			}

			FIXField<?> field = extractField(group, iterator, groupBuilder,
					groupDictionary);

			int tag = field.getTag();

			if (tag == firstField) {
				group = groupBuilder.create();
				groupField.add(group);
				group.setField(field);

				// We have consumed the field so lets move on
				iterator.next();

				firstFieldFound = true;
				previousOffset = -1;
				// QFJ-742
				if (field instanceof FIXGroupField<?>) {
					parseGroup(group, iterator, groupBuilder, groupDictionary,
							(FIXGroupField<?>) field);
				}
			} else if (field instanceof FIXGroupField<?>) {
				if (!firstFieldFound) {
					throw new InvalidMessage("The group " + groupCountTag
							+ " must set the delimiter field " + firstField
							+ " in " + iterator.getMessage());
				}
				group.setField(field);

				// We have consumed the field so lets move on
				iterator.next();

				parseGroup(group, iterator, groupBuilder, groupDictionary,
						(FIXGroupField<?>) field);
			} else if (groupDictionary.isField(iterator.getTag())) {
				if (!firstFieldFound) {
					throw new FieldException(
							SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER,
							SessionRejectReason
									.getMessage(SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER),
							tag);
				}

				if (validator.isCheckUnorderedGroupFields()) {
					List<Integer> fieldOrder = groupDictionary
							.getOrderedFields();
					int offset = indexOf(tag, fieldOrder);
					if (offset > -1) {
						if (offset <= previousOffset) {
							throw new FieldException(
									SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER,
									SessionRejectReason
											.getMessage(SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER),
									tag);
						}
						previousOffset = offset;
					}
				}
				group.setField(field);

				// We have consumed the field so lets move on
				iterator.next();
			}
		}
	}

	private int indexOf(int field, List<Integer> fieldOrder) {

		for (int i = 0; i < fieldOrder.size(); i++) {
			if (field == fieldOrder.get(i)) {
				return i;
			}
		}

		return -1;
	}

	private void validateCheckSum(FIXMessage message,
			MessageTokenIterator iterator) {

		try {
			// Body length is checked at the protocol layer
			final int checksum = getInt(message.getTrailer(), FIXTag.CHECK_SUM);
			if (checksum != MessageUtils.checksum(iterator.getMessage())) {
				// message will be ignored if checksum is wrong or missing
				throw new InvalidMessage("Expected CheckSum="
						+ MessageUtils.checksum(iterator.getMessage())
						+ ", Received CheckSum=" + checksum + " in "
						+ iterator.getMessage());
			}
		} catch (final FieldNotFound e) {
			throw new InvalidMessage("Field not found: " + e.field + " in "
					+ iterator.getMessage());
		}
	}

	private int getInt(FIXFieldGraph graph, int tag) {

		return IntConverter.convert(graph.getField(tag));
	}
}