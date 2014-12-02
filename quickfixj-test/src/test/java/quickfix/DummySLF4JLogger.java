package quickfix;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Dymmy noop implementation of a {@link Logger} class that doesn't do anything
 *
 * @author toli
 * @version $Id$
 */
public class DummySLF4JLogger implements Logger {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String s) {
        // no-op
    }

    @Override
    public void trace(String s, Object o) {
        // no-op
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        // no-op
    }

    @Override
    public void trace(String s, Object[] objects) {
        // no-op
    }

    @Override
    public void trace(String s, Throwable throwable) {
        // no-op
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String s) {
        // no-op
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        // no-op
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        // no-op
    }

    @Override
    public void trace(Marker marker, String s, Object[] objects) {
        // no-op
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        // no-op
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {
        // no-op
    }

    @Override
    public void debug(String format, Object arg) {
        // no-op
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void debug(String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void debug(String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {
        // no-op
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        // no-op
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(String msg) {
        // no-op
    }

    @Override
    public void info(String format, Object arg) {
        // no-op
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void info(String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void info(String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {
        // no-op
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        // no-op
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(String msg) {
        // no-op
    }

    @Override
    public void warn(String format, Object arg) {
        // no-op
    }

    @Override
    public void warn(String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void warn(String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {
        // no-op
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        // no-op
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String msg) {
        // no-op
    }

    @Override
    public void error(String format, Object arg) {
        // no-op
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void error(String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void error(String msg, Throwable t) {
        // no-op
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {
        // no-op
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        // no-op
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        // no-op
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        // no-op
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        // no-op
    }
}
