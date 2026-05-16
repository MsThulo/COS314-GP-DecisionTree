import java.io.OutputStream;
import java.io.PrintStream;


public class TeePrintStream extends PrintStream {

    private final PrintStream second;

    public TeePrintStream(PrintStream primary, PrintStream second) {
        super((OutputStream) primary);
        this.second = second;
    }

    @Override
    public void write(int b) {
        super.write(b);
        second.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        second.write(buf, off, len);
    }

    @Override
    public void flush() {
        super.flush();
        second.flush();
    }
}
