package keystrokesmod.script;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class ClassObject extends SimpleJavaFileObject
{
    private final String code;
    public final String name;
    public int extraLines;
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.code;
    }

    public ClassObject(final String name, final String code, int extraLines) {
        super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
        this.code = code;
        this.name = name;
        this.extraLines = extraLines;
    }
}