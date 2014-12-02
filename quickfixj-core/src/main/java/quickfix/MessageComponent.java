package quickfix;

/**
 * Represents a FIX message component.
 */
@Deprecated
public abstract class MessageComponent extends FieldMap {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    protected abstract int[] getFields();

    protected abstract int[] getGroupFields();

    protected MessageComponent() {
        super();
    }

    protected MessageComponent(int[] fieldOrder) {
        super(fieldOrder);
    }

    public void copyFrom(FieldMap fields) {
        try {
            for (int componentField : getFields()) {
                if (fields.isFieldSet(componentField)) {
                    setField(componentField, fields.getField(componentField));
                }
            }
            for (int groupField : getGroupFields()) {
                if (fields.isFieldSet(groupField)) {
                    setField(groupField, fields.getField(groupField));
                    // setGroups(groupField, fields.getGroups(groupField));
                }
            }
        } catch (FieldNotFound e) {
            // should not happen
        }
    }

    public void copyTo(FieldMap fields) {
        try {
            for (int componentField : getFields()) {
                if (isFieldSet(componentField)) {
                    fields.setField(componentField, getField(componentField));
                }
            }
            for (int groupField : getGroupFields()) {
                if (isFieldSet(groupField)) {
                    fields.setField(groupField, getField(groupField));
                    //    fields.setGroups(groupField, getGroups(groupField));
                }
            }
        } catch (FieldNotFound e) {
            // should not happen
        }
    }

}
