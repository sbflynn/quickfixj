package quickfix;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.spi.MessageBuilderServiceLoader;

public class SessionFactoryTestSupport implements SessionFactory {
    private static final SessionFactoryTestSupport instance = new SessionFactoryTestSupport();

    @Override
    public Session create(SessionID sessionID, SessionSettings settings) throws ConfigError {
        if (sessionID == null) {
            sessionID = new SessionID(FIXBeginString.FIX42, "SENDER", "TARGET");
        }
        return createSession(sessionID, new UnitTestApplication(), false);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator) {
        return new Session(application, new MemoryStoreFactory(), sessionID, null, null,
                new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0);
    }

    public static Session createFileStoreSession(SessionID sessionID, Application application,
            boolean isInitiator, SessionSettings settings, SessionSchedule sessionSchedule) {
        return new Session(application, new FileStoreFactory(settings), sessionID, null,
                sessionSchedule, new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator, boolean resetOnLogon, boolean validateSequenceNumbers) {
        return new Session(application, new MemoryStoreFactory(), sessionID, null, null,
                new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0,
                false, 30, true, resetOnLogon, false, false, false, false, false, true, false, 1.5,
                null, validateSequenceNumbers, new int[] { 5 }, false, false, false, true, false,
                true, false, null, true, 0, false, false);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator, boolean resetOnLogon, boolean validateSequenceNumbers,
            boolean useDataDictionary, FIXApplication senderDefaultApplVerID) {
        return new Session(application, new MemoryStoreFactory(), sessionID,
                new DefaultDataDictionaryProvider(), null, new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0,
                false, 30, true, resetOnLogon, false, false, false, false, false, true, false, 1.5,
                senderDefaultApplVerID, validateSequenceNumbers, new int[] { 5 }, false, false,
                false, true, false, true, false, null, true, 0, false, false);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator, boolean resetOnLogon) {
        return createSession(sessionID, application, isInitiator, resetOnLogon, false);
    }

    public static Session createNonpersistedSession(SessionID sessionID, Application application,
            boolean isInitiator) {
        return new Session(application, new MemoryStoreFactory(), sessionID, null, null,
                new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0,
                false, 30, true, true, false, false, false, false, false,
                false/*persistMessages*/, false, 1.5, null, true, new int[] { 5 }, false, false,
                false, true, false, true, false, null, true, 0, false, false);
    }

    public static Session createSession() throws ConfigError {
        return instance.create(null, null);
    }
}
