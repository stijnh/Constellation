package ibis.constellation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cache of direct {@link ByteBuffer}s of specific sizes. This class is not specifically part of constellation, but it may be
 * useful for applications. This class is thread-safe.
 *
 * TODO: move to a utility package?
 */
public class ByteBufferCache {

    private static final Logger logger = LoggerFactory.getLogger(ByteBufferCache.class);

    private static Map<Integer, List<ByteBuffer>> freeList = new HashMap<Integer, List<ByteBuffer>>();
    private static Map<Integer, FreelistFiller> fillers = new HashMap<Integer, FreelistFiller>();

    // Background thread creating new bytebuffers as needed.
    private static class FreelistFiller extends Thread {
        private final int sz;
        private final int increment;
        private final int threshold;

        FreelistFiller(int sz, int cnt) {
            this.sz = sz;
            this.threshold = cnt / 3;
            this.increment = cnt / 2;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            for (;;) {
                int cnt;
                synchronized (freeList) {
                    try {
                        freeList.wait();
                    } catch (Throwable e) {
                        // ignore
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Filler woke up");
                    }
                    List<ByteBuffer> l = freeList.get(sz);
                    cnt = increment - l.size();
                }
                for (int i = 0; i < cnt; i++) {
                    ByteBuffer v = ByteBuffer.allocateDirect(sz).order(ByteOrder.nativeOrder());
                    releaseByteBuffer(v);
                }
            }
        }
    }

    /**
     * Release the specified byte buffer, that is, append it to the list of available byte buffers.
     *
     * @param b
     *            the byte buffer to be released.
     */
    public static void releaseByteBuffer(ByteBuffer b) {
        if (logger.isDebugEnabled()) {
            logger.debug("Releasing bytebuffer " + System.identityHashCode(b));
        }
        int sz = b.capacity();
        synchronized (freeList) {
            List<ByteBuffer> l = freeList.get(sz);
            if (l == null) {
                l = new ArrayList<ByteBuffer>();
                freeList.put(sz, l);
            }
            l.add(b);
        }
    }

    private static byte[] initBuffer = new byte[65536];

    /**
     * Obtains a byte buffer of the specified size. If one cannot be found in the cache, a new one is allocated. If it needs to be
     * clear(ed), the <code>needsClearing</code> flag should be set to <code>true</code>.
     *
     * @param sz
     *            size of the byte buffer to be obtained.
     * @param needsClearing
     *            whether the buffer must be cleared.
     * @return the obtained byte buffer.
     */
    public static ByteBuffer getByteBuffer(int sz, boolean needsClearing) {
        ByteBuffer b;
        synchronized (freeList) {
            List<ByteBuffer> l = freeList.get(sz);
            if (l == null || l.size() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Allocating new bytebuffer");
                }
                return ByteBuffer.allocateDirect(sz).order(ByteOrder.nativeOrder());
            }
            b = l.remove(0);
            FreelistFiller f = fillers.get(sz);
            if (l.size() < f.threshold) {
                freeList.notify();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("bytebuffer " + System.identityHashCode(b) + " from cache");
            }
        }
        if (needsClearing) {
            // Clear buffer.
            b.position(0);
            while (b.position() + initBuffer.length <= b.capacity()) {
                b.put(initBuffer);
            }
            b.put(initBuffer, 0, b.capacity() - b.position());
        }
        return b;
    }

    /**
     * Initializes the byte buffer cache with the specified number of buffers of the specified size.
     *
     * @param sz
     *            the size of the byte buffers
     * @param count
     *            the number of byte buffers
     */
    public static void initializeByteBuffers(int sz, int count) {
        if (logger.isDebugEnabled()) {
            logger.debug("Allocating " + count + " buffers of size " + sz);
        }
        for (int i = 0; i < count; i++) {
            ByteBuffer v = ByteBuffer.allocateDirect(sz).order(ByteOrder.nativeOrder());
            releaseByteBuffer(v);
        }
        FreelistFiller filler = new FreelistFiller(sz, count);
        fillers.put(sz, filler);
        filler.start();
    }

}
