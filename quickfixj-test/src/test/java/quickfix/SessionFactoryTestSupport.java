package quickfix;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.Validator;

public class SessionFactoryTestSupport implements SessionFactory {
    private static final SessionFactoryTestSupport instance = new SessionFactoryTestSupport();

    @Override
    public Session create(FIXSessionID sessionID, SessionSettings settings) throws ConfigError {
        if (sessionID == null) {
            sessionID = new SessionID(FIXBeginString.FIX42, "SENDER", "TARGET");
        }
        return createSession(sessionID, new UnitTestApplication(), false);
    }

    public static Session createSession(FIXSessionID sessionID, Application application,
            boolean isInitiator) {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageDictionaryFactory dataDictionary = engine.getMessageDictionaryFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = engine.getMessageBuilderFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        Validator validator = new DefaultValidator(sessionID.getBeginString());

        return new Session(application, new MemoryStoreFactory(), sessionID, dataDictionary, null,
                new ScreenLogFactory(true, true, true), builderFactory, validator, isInitiator
                        ? 30
                        : 0);
    }

    public static Session createFileStoreSession(SessionID sessionID, Application application,
            boolean isInitiator, SessionSettings settings, SessionSchedule sessionSchedule)
            throws ConfigError {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageBuilderFactory builderFactory = engine.getMessageBuilderFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        Validator validator = new DefaultValidator(sessionID, settings);

        return new Session(application, new FileStoreFactory(settings), sessionID, null,
                sessionSchedule, new ScreenLogFactory(true, true, true), builderFactory, validator,
                isInitiator ? 30 : 0);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator, boolean resetOnLogon, boolean validateSequenceNumbers) {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageDictionaryFactory dataDictionary = engine.getMessageDictionaryFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = engine.getMessageBuilderFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        Validator validator = new DefaultValidator(sessionID.getBeginString());

        return new Session(application, new MemoryStoreFactory(), sessionID, dataDictionary, null,
                new ScreenLogFactory(true, true, true), builderFactory, validator, isInitiator
                        ? 30
                        : 0, false, 30, true, resetOnLogon, false, false, false, false, false,
                true, false, 1.5, null, validateSequenceNumbers, new int[] { 5 }, false, false,
                false, true, false, true, false, null, true, 0, false, false);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator, boolean resetOnLogon, boolean validateSequenceNumbers,
            boolean useDataDictionary, FIXApplication senderDefaultApplVerID) {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageDictionaryFactory dataDictionary = engine.getMessageDictionaryFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = engine.getMessageBuilderFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        Validator validator = new DefaultValidator(sessionID.getBeginString());

        return new Session(application, new MemoryStoreFactory(), sessionID, dataDictionary, null,
                new ScreenLogFactory(true, true, true), builderFactory, validator, isInitiator
                        ? 30
                        : 0, false, 30, true, resetOnLogon, false, false, false, false, false,
                true, false, 1.5, senderDefaultApplVerID, validateSequenceNumbers, new int[] { 5 },
                false, false, false, true, false, true, false, null, true, 0, false, false);
    }

    public static Session createSession(SessionID sessionID, Application application,
            boolean isInitiator, boolean resetOnLogon) {
        return createSession(sessionID, application, isInitiator, resetOnLogon, false);
    }

    public static Session createNonpersistedSession(SessionID sessionID, Application application,
            boolean isInitiator) {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageBuilderFactory builderFactory = engine.getMessageBuilderFactory(
                sessionID.getBeginString(), "org.quickfixj.messages.bd");
        Validator validator = new DefaultValidator(sessionID.getBeginString());

        return new Session(application, new MemoryStoreFactory(), sessionID, null, null,
                new ScreenLogFactory(true, true, true), builderFactory, validator, isInitiator
                        ? 30
                        : 0, false, 30, true, true, false, false, false, false, false,
                false/*persistMessages*/, false, 1.5, null, true, new int[] { 5 }, false, false,
                false, true, false, true, false, null, true, 0, false, false);
    }

    public static Session createSession() throws ConfigError {
        return instance.create(null, null);
    }
}
