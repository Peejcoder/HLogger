package com.peej.logger;


import org.jetbrains.annotations.Nullable;
import com.peej.logger.org.JSONArray;
import com.peej.logger.org.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.peej.logger.Logger.*;
import static com.peej.logger.Utils.checkNotNull;

class LoggerPrinter implements Printer {

    /**
     * It is used for json pretty print
     */
    private static final int JSON_INDENT = 2;

    /**
     * Provides one-time used tag for the log message
     */
    private final ThreadLocal<String> localTag = new ThreadLocal<>();

    private final List<LogAdapter> logAdapters = new ArrayList<>();

    @Override
    public Printer t(String tag) {
        if (tag != null) {
            localTag.set(tag);
        }
        return this;
    }

    @Override
    public void d(String message, @Nullable Object... args) {
        log(DEBUG, null, message, args);
    }

    @Override
    public void d(@Nullable Object object) {
        log(DEBUG, null, Utils.toString(object));
    }

    @Override
    public void e(String message, @Nullable Object... args) {
        e(null, message, args);
    }

    @Override
    public void e(@Nullable Throwable throwable, String message, @Nullable Object... args) {
        log(ERROR, throwable, message, args);
    }

    @Override
    public void w(String message, @Nullable Object... args) {
        log(WARN, null, message, args);
    }

    @Override
    public void i(String message, @Nullable Object... args) {
        log(INFO, null, message, args);
    }

    @Override
    public void i(@Nullable Object object) {
        log(INFO, null, Utils.toString(object));

    }

    @Override
    public void v(String message, @Nullable Object... args) {
        log(VERBOSE, null, message, args);
    }

    @Override
    public void wtf(String message, @Nullable Object... args) {
        log(ASSERT, null, message, args);
    }

    @Override
    public void json(@Nullable String json) {
        if (Utils.isEmpty(json)) {
            d("Empty/Null json content");
            return;
        }
        try {
            json = json.trim();
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                String message = jsonObject.toString(JSON_INDENT);
                i(message);
                return;
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                String message = jsonArray.toString(JSON_INDENT);
                i(message);
                return;
            }
            e("Invalid Json");
        } catch (Exception e) {
            e("Invalid Json");
        }
    }

    @Override
    public void xml(@Nullable String xml) {
        if (Utils.isEmpty(xml)) {
            d("Empty/Null xml content");
            return;
        }
//        try {
//
//            Source xmlInput = new StreamSource(new StringReader(xml));
//            StreamResult xmlOutput = new StreamResult(new StringWriter());
//
//            Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//            transformer.transform(xmlInput, xmlOutput);
//            d(xmlOutput.getWriter().toString().replaceFirst(">", ">\n"));
//        } catch (TransformerException e) {
//            e("Invalid xml");
//        }
    }

    @Override
    public synchronized void log(int priority,
                                 @Nullable String tag,
                                 @Nullable String message,
                                 @Nullable Throwable throwable) {
        if (throwable != null && message != null) {
            message += " : " + Utils.getStackTraceString(throwable);
        }
        if (throwable != null && message == null) {
            message = Utils.getStackTraceString(throwable);
        }
        if (Utils.isEmpty(message)) {
            message = "Empty/NULL log message";
        }

        for (LogAdapter adapter : logAdapters) {
            if (adapter.isLoggable(priority, tag)) {
                adapter.log(priority, tag, message);
            }
        }
    }

    @Override
    public void clearLogAdapters() {
        logAdapters.clear();
    }

    @Override
    public void addAdapter(LogAdapter adapter) {
        logAdapters.add(checkNotNull(adapter));
    }

    /**
     * This method is synchronized in order to avoid messy of logs' order.
     */
    private synchronized void log(int priority,
                                  @Nullable Throwable throwable,
                                  String msg,
                                  @Nullable Object... args) {
        checkNotNull(msg);

        String tag = getTag();
        String message = createMessage(msg, args);
        log(priority, tag, message, throwable);
    }

    /**
     * @return the appropriate tag based on local or global
     */
    @Nullable
    private String getTag() {
        String tag = localTag.get();
        if (tag != null) {
            localTag.remove();
            return tag;
        }
        return null;
    }

    private String createMessage(String message, @Nullable Object... args) {
        return args == null || args.length == 0 ? message : String.format(message, args);
    }
}
