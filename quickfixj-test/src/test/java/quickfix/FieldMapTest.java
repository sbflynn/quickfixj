package quickfix;

import java.util.Date;
import java.util.Iterator;

import org.quickfixj.FIXField;
import org.quickfixj.field.UtcTimeOnlyField;
import org.quickfixj.field.UtcTimeStampField;

import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.fix44.field.EffectiveTime;
import quickfix.fix44.field.MDEntryTime;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the {@link FieldMap} class.
 * Specifically, verifies that the setters for {@link UtcTimeStampField} work correctly.
 *
 * @author toli
 * @version $Id$
 */
public class FieldMapTest extends TestCase {
    public FieldMapTest(String inName) {
        super(inName);
    }

    public static Test suite() {
        return new TestSuite(FieldMapTest.class);
    }

    public void testSetUtcTimeStampField() throws Exception {
        FieldMap map = new Message();
        Date aDate = new Date();
        map.setField(new EffectiveTime(aDate/*, false*/));
        assertEquals("milliseconds should not be preserved", aDate.getTime()
                - (aDate.getTime() % 1000),
                ((UtcTimeStampField) map.getField(quickfix.fix44.field.EffectiveTime.TAG))
                        .getValue().getTime());

        // now set it with preserving millis
        map.setField(new EffectiveTime(aDate/*, true*/));
        assertEquals("milliseconds should be preserved", aDate.getTime(),
                ((UtcTimeStampField) map.getField(quickfix.fix44.field.EffectiveTime.TAG))
                        .getValue().getTime());
    }

    public void testSetUtcTimeOnlyField() throws Exception {
        FieldMap map = new Message();
        Date aDate = new Date();
        map.setField(new MDEntryTime(aDate/*, false*/));
        assertEquals("milliseconds should not be preserved", UtcTimeOnlyConverter.convert(aDate,
                false), UtcTimeOnlyConverter.convert(((UtcTimeOnlyField) map
                .getField(quickfix.fix44.field.MDEntryTime.TAG)).getValue(), false));

        // now set it with preserving millis
        map.setField(new MDEntryTime(aDate/*, true*/));
        assertEquals("milliseconds should be preserved", UtcTimeOnlyConverter.convert(aDate, true),
                UtcTimeOnlyConverter.convert(((UtcTimeOnlyField) map
                        .getField(quickfix.fix44.field.MDEntryTime.TAG)).getValue(), true));
    }

    /**
     * Try a subclass of {@link UtcTimeOnlyField} and {@link UtcTimeStampField} directly
     */
    public void testSpecificFields() throws Exception {
        FieldMap map = new Message();
        Date aDate = new Date();
        map.setField(new EffectiveTime(aDate));
        assertEquals("milliseconds should be preserved", aDate.getTime(),
                ((EffectiveTime) map.getField(EffectiveTime.TAG)).getValue().getTime());
        map.setField(new MDEntryTime(aDate));
        assertEquals(
                "milliseconds should be preserved",
                UtcTimeOnlyConverter.convert(aDate, true),
                UtcTimeOnlyConverter.convert(
                        ((MDEntryTime) map.getField(MDEntryTime.TAG)).getValue(), true));
    }

    private void testOrdering(int[] vals, int[] order, int[] expected) {
        FieldMap map = new Message(order);
        for (int v : vals)
            map.setInt(v, v);
        Iterator<FIXField<?>> it = map.iterator();
        for (int e : expected)
            assertEquals(String.valueOf(e), it.next().getValue());
    }

    public void testOrdering() {
        testOrdering(new int[] { 1, 2, 3 }, null, new int[] { 1, 2, 3 });
        testOrdering(new int[] { 3, 2, 1 }, null, new int[] { 1, 2, 3 });
        testOrdering(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 });
        testOrdering(new int[] { 3, 2, 1 }, new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 });
        testOrdering(new int[] { 1, 2, 3 }, new int[] { 1, 3, 2 }, new int[] { 1, 3, 2 });
        testOrdering(new int[] { 3, 2, 1 }, new int[] { 1, 3, 2 }, new int[] { 1, 3, 2 });
        testOrdering(new int[] { 1, 2, 3 }, new int[] { 1, 3 }, new int[] { 1, 3, 2 });
        testOrdering(new int[] { 3, 2, 1 }, new int[] { 1, 3 }, new int[] { 1, 3, 2 });
        testOrdering(new int[] { 1, 2, 3 }, new int[] { 3, 1 }, new int[] { 3, 1, 2 });
        testOrdering(new int[] { 3, 2, 1 }, new int[] { 3, 1 }, new int[] { 3, 1, 2 });
    }
}
