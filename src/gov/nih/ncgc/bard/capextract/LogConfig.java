package gov.nih.ncgc.bard.capextract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A simplistic, all-in-one-line, formatter for logging.
 * <p/>
 * This is primarily used by the CAP extraction code, but can be reused by
 * other classes. If so it will probably move to the tools package in the
 * future.
 *
 * @author Rajarshi Guha
 */
public class LogConfig {
    private final LogManager logManager;
    private final Logger rootLogger;
    private final Handler defaultHandler = new ConsoleHandler();
    private final SimpleFormatter defaultFormatter = new SimpleFormatter();

    public LogConfig() {
        super();

        this.logManager = LogManager.getLogManager();
        this.rootLogger = Logger.getLogger("");

        configure();
    }

    final void configure() {
        defaultHandler.setFormatter(new LineFormatter());
        defaultHandler.setLevel(Level.INFO);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addHandler(defaultHandler);
        logManager.addLogger(rootLogger);
    }


}

class LineFormatter extends Formatter {
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String FIELD_SEP = " ";
    private ThreadLocal dateFormat = new ThreadLocal() {
        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        }
    };

    @Override
    public String format(LogRecord logRecord) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append(((DateFormat) dateFormat.get()).format(new Date(logRecord.getMillis())));
        logEntry.append(FIELD_SEP);
        logEntry.append(logRecord.getLoggerName());
        logEntry.append(FIELD_SEP);
        logEntry.append(logRecord.getLevel().getName());
        logEntry.append(FIELD_SEP);
        logEntry.append(logRecord.getMessage());
        logEntry.append(LINE_SEP);
        return logEntry.toString();
    }
}

