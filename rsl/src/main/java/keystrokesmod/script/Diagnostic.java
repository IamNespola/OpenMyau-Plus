package keystrokesmod.script;

import keystrokesmod.utility.Utils;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.Locale;

public class Diagnostic implements DiagnosticListener<JavaFileObject> {

    @Override
    public void report(final javax.tools.Diagnostic<? extends JavaFileObject> diagnostic) {
        final String message = diagnostic.getMessage(null);
        if (message.contains("SpongePowered")) {
            return;
        }
        if (diagnostic.getSource() != null) {
            Utils.sendDebugMessage("§cError loading script §b" + Utils.extractFileName(((ClassObject) diagnostic.getSource()).name));
        }
        final JavaFileObject javaFileObject = diagnostic.getSource();
        if (javaFileObject != null) {
            int indentIndex = message.indexOf("\n");
            String error = diagnostic.getMessage(Locale.getDefault());
            Utils.sendDebugMessage(" §7err: §c" + (indentIndex == -1 ? error : error.substring(0, indentIndex)));
            Utils.sendDebugMessage(" §7line: §c" + (diagnostic.getLineNumber() - ((ClassObject) diagnostic.getSource()).extraLines));
            String sourceContent = ((ClassObject) diagnostic.getSource()).getCharContent(true).toString();
            int startPos = (int) diagnostic.getStartPosition();
            int endPos = (int) diagnostic.getEndPosition();
            int srcIndentIndex = sourceContent.indexOf("\n", startPos);
            if (srcIndentIndex != -1) {
                Utils.sendDebugMessage(" §7src: §c" + sourceContent.substring(startPos, srcIndentIndex));
            }
            else {
                Utils.sendDebugMessage(" §7src: §c" + sourceContent.substring(startPos, endPos));
            }
        }
    }
}
