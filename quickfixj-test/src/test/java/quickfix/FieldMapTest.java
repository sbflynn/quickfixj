package quickfix;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.quickfixj.FIXField;
import org.quickfixj.field.GenericField;
import org.quickfixj.field.UtcTimeOnlyConverter;
import org.quickfixj.field.UtcTimeOnlyField;
import org.quickfixj.field.UtcTimestampField;
import org.quickfixj.messages.bd.fix44.field.EffectiveTime;
import org.quickfixj.messages.bd.fix44.field.MDEntryTime;

/**
 * Tests the {@link AbstractFieldGraph} class.
 * Specifically, verifies that the setters for {@link UtcTimestampField} work correctly.
 *
 * @author toli
 * @version $Id$
 */
public class FieldMapTest {

    @Test
    public void testSetUtcTimeStampField() throws Exception {
        AbstractFieldGraph map = new Message();
        Date aDate = new Date();
        map.setField(new EffectiveTime(aDate, false));
        assertEquals("milliseconds should not be preserved", aDate.getTime()
                - (aDate.getTime() % 1000), ((UtcTimestampField) map.getField(EffectiveTime.TAG))
                .getValue().getTime());

        // now set it with preserving millis
        map.setField(new EffectiveTime(aDate, true));
        assertEquals("milliseconds should be preserved", aDate.getTime(),
                ((UtcTimestampField) map.getField(EffectiveTime.TAG)).getValue().getTime());
    }

    @Test
    public void testSetUtcTimeOnlyField() throws Exception {
        AbstractFieldGraph map = new Message();
        Date aDate = new Date();
        map.setField(new MDEntryTime(aDate, false));
        assertEquals(
                "milliseconds should not be preserved",
                UtcTimeOnlyConverter.convert(aDate, false),
                UtcTimeOnlyConverter.convert(
                        ((UtcTimeOnlyField) map.getField(MDEntryTime.TAG)).getValue(), false));

        // now set it with preserving millis
        map.setField(new MDEntryTime(aDate, true));
        assertEquals(
                "milliseconds should be preserved",
                UtcTimeOnlyConverter.convert(aDate, true),
                UtcTimeOnlyConverter.convert(
                        ((UtcTimeOnlyField) map.getField(MDEntryTime.TAG)).getValue(), true));
    }

    /**
     * Try a subclass of {@link UtcTimeOnlyField} and {@link UtcTimestampField} directly
     */
    @Test
    public void testSpecificFields() throws Exception {
        AbstractFieldGraph map = new Message();
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

    @Test
    public void testOrdering() {
        testOrdering(new int[] { 1, 2, 3 }, null, new int[] { 1, 2, 3 });
        testOrdering(new int[] { 3, 2, 1 }, null, new int[] { 1, 2, 3 });
        testOrdering(new int[] { 1, 2, 3 }, Arrays.asList(1, 2, 3), new int[] { 1, 2, 3 });
        testOrdering(new int[] { 3, 2, 1 }, Arrays.asList(1, 2, 3), new int[] { 1, 2, 3 });
        testOrdering(new int[] { 1, 2, 3 }, Arrays.asList(1, 3, 2), new int[] { 1, 3, 2 });
        testOrdering(new int[] { 3, 2, 1 }, Arrays.asList(1, 3, 2), new int[] { 1, 3, 2 });
        testOrdering(new int[] { 1, 2, 3 }, Arrays.asList(1, 3), new int[] { 1, 3, 2 });
        testOrdering(new int[] { 3, 2, 1 }, Arrays.asList(1, 3), new int[] { 1, 3, 2 });
        testOrdering(new int[] { 1, 2, 3 }, Arrays.asList(3, 1), new int[] { 3, 1, 2 });
        testOrdering(new int[] { 3, 2, 1 }, Arrays.asList(3, 1), new int[] { 3, 1, 2 });
    }

    private void testOrdering(int[] vals, List<Integer> order, int[] expected) {
        AbstractFieldGraph map = new AbstractFieldGraph(order) {
            private static final long serialVersionUID = 1L;
        };
        for (int v : vals)
            map.setField(new GenericField(v, v));
        Iterator<FIXField<?>> it = map.iterator();
        for (int e : expected)
            assertEquals(String.valueOf(e), it.next().getValue());
    }
}
