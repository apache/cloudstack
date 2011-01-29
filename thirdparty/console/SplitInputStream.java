import java.io.*;

public class SplitInputStream extends FilterInputStream {
	ByteArrayOutputStream bo;
	public SplitInputStream(InputStream in) {
		super(in);
	}
	public int read() throws IOException {
		int b = super.read();
		if (b >= 0 && bo != null) {
			bo.write(b);
		}
		return b;
	}
	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}
	public int read(byte b[], int off, int len) throws IOException {
		int res = super.read(b, off, len);
		if (res > 0 && bo != null) {
			bo.write(b, off, res);
		}
		return res;
	}
	public long skip(long n) throws IOException {
		long res = super.skip(n);
		if (res > 0 && bo != null) {
			byte[] b = new byte[(int)res];
			bo.write(b, 0, (int)res);
		}
		return res;
	}
	public int available() throws IOException {
		return super.available();
	}
	public void close() throws IOException {
		super.close();
	}
	public void mark(int readlimit) {
		super.mark(readlimit);
	}
	public void reset() throws IOException {
		super.reset();
	}
	public boolean markSupported() {
		return false;
	}
	public void setSplit() {
		bo = new ByteArrayOutputStream();
	}
	public byte[] getSplit() {
		if (bo == null) {
			return null;
		}
		byte[] res = bo.toByteArray();
		bo = null;
		return res;
	}
}
