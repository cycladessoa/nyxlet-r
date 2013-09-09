package org.cyclades.nyxlet.r.actionhandler.api;

import java.io.OutputStream;
import org.math.R.Logger;

public class RsessionOutput implements Logger {
    
    public RsessionOutput (Level maintainedLevel, OutputStream out) {
        this.maintainedLevel = maintainedLevel;
        this.out = out;
    }

    @Override
    public void close() {
        // XXX - No need for this, it is the callers responsibility for the life cycle of the OutputStream
    }

    @Override
    public void println(String message, Level requestedLevel) {
        if (out == null) return;
        // XXX - See order of enums in org.math.R.Logger.java 
        if (requestedLevel.ordinal() >= maintainedLevel.ordinal()) {
            try {
                out.write((message == null) ? "".getBytes() : message.getBytes());
                out.write("\n".getBytes());
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private final OutputStream out;
    private final Level maintainedLevel;

}
