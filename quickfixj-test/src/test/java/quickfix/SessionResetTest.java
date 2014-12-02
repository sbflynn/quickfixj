package quickfix;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;
import org.quickfixj.FIXBeginString;

import quickfix.fix44.TestRequest;
import quickfix.fix44.field.MsgType;
import quickfix.fix44.field.TestReqID;

public class SessionResetTest {

    private static final int NUMBER_OF_ADMIN_MESSAGES = 50;

    @Test
    // QFJ-645, QFJ-716
    public void testSessionResetDeadlock() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = SessionFactoryTestSupport.createSession(sessionID, application,
                true, false);
        final UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);
        session.addStateListener(responder);
        session.logon();
        session.next();

        assertFalse(responder.onResetCalled);

        final Message logonRequest = new Message(responder.sentMessageData);
        final Message logonResponse = new DefaultMessageFactory().create(
                sessionID.getBeginString(), FixMessageTypes.LOGON);
        logonResponse.setInt(FixTags.ENCRYPT_METHOD, 0);
        logonResponse.setInt(FixTags.HEART_BT_INT, logonRequest.getInt(FixTags.HEART_BT_INT));

        final Message.Header header = logonResponse.getHeader();
        header.setString(FixTags.BEGIN_STRING, sessionID.getBeginString().getValue());
        header.setString(FixTags.SENDER_COMP_ID, sessionID.getSenderCompID());
        header.setString(FixTags.TARGET_COMP_ID, sessionID.getTargetCompID());
        header.setInt(FixTags.MSG_SEQ_NUM, 1);
        header.setUtcTimeStamp(FixTags.SENDING_TIME, SystemTime.getDate(), true);

        Thread resetThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    session.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "SessionReset");

        Thread messageSender = new Thread(new Runnable() {

            @Override
            public void run() {

                for (int i = 2; i <= NUMBER_OF_ADMIN_MESSAGES; i++) {
                    session.send(createAdminMessage(i));
                }
            }
        }, "SessionSend");

        // submit threads to pausable executor and try to let them start at the
        // same time
        PausableThreadPoolExecutor ptpe = new PausableThreadPoolExecutor();
        ptpe.pause();
        ptpe.submit(messageSender);
        ptpe.submit(resetThread);
        ptpe.resume();
        ptpe.awaitTermination(2, TimeUnit.SECONDS);

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] threadIds = bean.findDeadlockedThreads();
        assertNull("no threads should be deadlocked", threadIds);
        assertTrue("session should have been reset", responder.onResetCalled);
    }

    private Message createAdminMessage(int sequence) {

        final TestRequest msg = new TestRequest(new TestReqID("SessionResetTest"));
        msg.getHeader().setField(MsgType.TEST_REQUEST);
        msg.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        msg.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        msg.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        msg.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());
        return msg;
    }

    private class UnitTestResponder implements Responder, SessionStateListener {

        public String sentMessageData;

        public boolean onResetCalled;

        @Override
        public boolean send(String data) {

            sentMessageData = data;
            return true;
        }

        @Override
        public String getRemoteAddress() {

            return null;
        }

        @Override
        public void disconnect() {
            //no-op
        }

        @Override
        public void onConnect() {
            //no-op
        }

        @Override
        public void onDisconnect() {
            //no-op
        }

        @Override
        public void onLogon() {
            //no-op
        }

        @Override
        public void onLogout() {
            //no-op
        }

        @Override
        public void onReset() {

            onResetCalled = true;
        }

        @Override
        public void onRefresh() {
            //no-op
        }

        @Override
        public void onMissedHeartBeat() {
            //no-op
        }

        @Override
        public void onHeartBeatTimeout() {
            //no-op
        }
    }

    private class PausableThreadPoolExecutor extends ThreadPoolExecutor {

        private boolean isPaused;

        private ReentrantLock pauseLock = new ReentrantLock();

        private Condition unpaused = pauseLock.newCondition();

        public PausableThreadPoolExecutor() {

            super(2, 2, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10000));
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {

            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.await();
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        public void pause() {

            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        public void resume() {

            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }
}
