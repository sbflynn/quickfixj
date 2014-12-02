package quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.quickfixj.FIXBeginString;

import quickfix.fix50.field.MsgType;
import quickfix.test.util.ExpectedTestFailure;

/**
 * Verifies the behaviour of the {@link DefaultMessageFactory} class
 *
 * @author toli
 * @version $Id$
 */
public class DefaultMessageFactoryTest {

    private DefaultMessageFactory factory = new DefaultMessageFactory();

    @Test
    public void testMessageCreate() throws Exception {

        assertMessage(
                quickfix.fix40.Advertisement.class,
                quickfix.fix40.field.MsgType.ADVERTISEMENT.getValue(),
                factory.create(FIXBeginString.FIX40,
                        quickfix.fix40.field.MsgType.ADVERTISEMENT.getValue()));
        assertMessage(
                quickfix.fix41.Advertisement.class,
                quickfix.fix41.field.MsgType.ADVERTISEMENT.getValue(),
                factory.create(FIXBeginString.FIX41,
                        quickfix.fix41.field.MsgType.ADVERTISEMENT.getValue()));
        assertMessage(
                quickfix.fix42.Advertisement.class,
                quickfix.fix42.field.MsgType.ADVERTISEMENT.getValue(),
                factory.create(FIXBeginString.FIX42,
                        quickfix.fix42.field.MsgType.ADVERTISEMENT.getValue()));
        assertMessage(
                quickfix.fix43.Advertisement.class,
                quickfix.fix43.field.MsgType.ADVERTISEMENT.getValue(),
                factory.create(FIXBeginString.FIX43,
                        quickfix.fix42.field.MsgType.ADVERTISEMENT.getValue()));
        assertMessage(
                quickfix.fix44.Advertisement.class,
                quickfix.fix44.field.MsgType.ADVERTISEMENT.getValue(),
                factory.create(FIXBeginString.FIX44,
                        quickfix.fix44.field.MsgType.ADVERTISEMENT.getValue()));
        assertMessage(
                quickfix.fix50.Advertisement.class,
                quickfix.fix50.field.MsgType.ADVERTISEMENT.getValue(),
                factory.create(FIXBeginString.FIXT11,
                        quickfix.fix50.field.MsgType.ADVERTISEMENT.getValue()));
        assertMessage(quickfix.Message.class, "7", factory.create(null, "7"));
    }

    @Test
    public void testFixtCreate() throws Exception {

        assertMessage(quickfix.fixt11.Logon.class, FixMessageTypes.LOGON,
                factory.create(FIXBeginString.FIXT11, FixMessageTypes.LOGON));
    }

    @Test
    public void testGroupCreate() throws Exception {

        new ExpectedTestFailure(IllegalArgumentException.class, "unknown") {

            @Override
            protected void execute() throws Throwable {

                factory.create(null, "B", quickfix.fix40.field.LinesOfText.TAG);
            }
        }.run();

        assertEquals(quickfix.fix40.News.LinesOfText.class,
                factory.create(FIXBeginString.FIX40, "B", quickfix.fix40.field.LinesOfText.TAG)
                        .getClass());
        assertEquals(quickfix.fix41.News.LinesOfText.class,
                factory.create(FIXBeginString.FIX41, "B", quickfix.fix41.field.LinesOfText.TAG)
                        .getClass());
        assertEquals(quickfix.fix42.News.LinesOfText.class,
                factory.create(FIXBeginString.FIX42, "B", quickfix.fix42.field.LinesOfText.TAG)
                        .getClass());
        assertEquals(quickfix.fix43.News.LinesOfText.class,
                factory.create(FIXBeginString.FIX43, "B", quickfix.fix43.field.LinesOfText.TAG)
                        .getClass());
        assertEquals(quickfix.fix44.News.LinesOfText.class,
                factory.create(FIXBeginString.FIX44, "B", quickfix.fix44.field.LinesOfText.TAG)
                        .getClass());
        assertEquals(
                quickfix.fix50.component.LinesOfTextGrp.NoLinesOfText.class,
                factory.create(FIXBeginString.FIXT11, "B",
                        quickfix.fix50.component.LinesOfTextGrp.NoLinesOfText.TAG).getClass());
        assertNull("if group can't be created return null", factory.create(FIXBeginString.FIX40,
                MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH.getValue(),
                quickfix.fix44.field.NoMDEntries.TAG));
    }

    private static void assertMessage(Class<?> expectedMessageClass, String expectedMessageType,
            Message message) throws Exception {

        assertEquals(expectedMessageClass, message.getClass());
        assertEquals(expectedMessageType, message.getHeader().getString(FixTags.MSG_TYPE));
    }
}
