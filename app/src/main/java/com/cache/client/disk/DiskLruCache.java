package com.cache.client.disk;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hongmingwei on 2017/11/20.
 * Email: 648600445@qq.com
 */

public class DiskLruCache implements Closeable {

    static final String JOURNAL_FILE = "journal";
    static final String JOURNAL_FILE_TEMP = "journal.tmp";
    static final String JOURNAL_FILE_BACKUP = "journal.bkp";
    static final String MAGIC = "libcore.io.DiskLruCache";
    static final String VERSION_1 = "1";

    static final long ANY_SEQUENCE_NUMBER = -1;
    static final String STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}";
    static final Pattern LEGAL_KEY_PATTERN = Pattern.compile(STRING_KEY_PATTERN);

    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    private static final String REMOVE = "REMOVE";
    private static final String READ = "READ";

    private final File directory;
    private final File journalFile;
    private final File journalFileTmp;
    private final File journalFileBackup;
    private final int appVersion;
    private long maxSize;
    private final int valueCount;
    private long size = 0;
    private Writer journalWriter;
    private final LinkedHashMap<String, Entry> lruEntries =
            new LinkedHashMap<String, Entry>(0, 0.75f, true);
    private int redundantOpCount;

    //区分新旧版本，给每条数据添加一个序列号
    private long nextSequenceNumber = 0;

    //使用一个单独的后台线程来缓存数据
    final ThreadPoolExecutor executorService =
            new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private final Callable<Void> cleanupCallable = new Callable<Void>() {
        public Void call() throws Exception {
            synchronized (DiskLruCache.this) {
                if (journalWriter == null) {
                    return null;
                }
                trimToSize();
                if (journalRebuildRequired()) {
                    rebuildJournal();
                    redundantOpCount = 0;
                }
            }
            return null;
        }
    };

    private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        this.directory = directory;
        this.appVersion = appVersion;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TEMP);
        this.journalFileBackup = new File(directory, JOURNAL_FILE_BACKUP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
    }

    /**
     * 打开缓存，没有就创建一个
     *
     * @param directory  可写文件目录
     * @param appVersion 版本号
     * @param valueCount 缓存数量
     * @param maxSize    缓存最大字节数
     * @return
     * @throws IOException
     */
    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
            throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount <= 0");
        }
        //如果有bkp文件就使用
        File backupFile = new File(directory, JOURNAL_FILE_BACKUP);
        if (backupFile.exists()) {
            File journalFile = new File(directory, JOURNAL_FILE);
            //如果日志文件存在就删除备份文件
            if (journalFile.exists()) {
                backupFile.delete();
            } else {
                renameTo(backupFile, journalFile, false);
            }
        }
        DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        if (cache.journalFile.exists()) {
            try {
                cache.readJournal();
                cache.processJournal();
                return cache;
            } catch (IOException journalIsCorrupt) {
                cache.delete();
            }
        }
        directory.mkdirs();
        cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        cache.rebuildJournal();
        return cache;
    }


    private void readJournal() throws IOException {
        StrictLineReader reader = new StrictLineReader(new FileInputStream(journalFile), Utils.US_ASCII);
        try {
            String magic = reader.readLine();
            String version = reader.readLine();
            String appVersionString = reader.readLine();
            String valueCountString = reader.readLine();
            String blank = reader.readLine();
            if (!MAGIC.equals(magic)
                    || !VERSION_1.equals(version)
                    || !Integer.toString(appVersion).equals(appVersionString)
                    || !Integer.toString(valueCount).equals(valueCountString)
                    || !"".equals(blank)) {
                throw new IOException("unexpected journal header: [" + magic + ", " + version + ", "
                        + valueCountString + ", " + blank + "]");
            }

            int lineCount = 0;
            while (true) {
                try {
                    readJournalLine(reader.readLine());
                    lineCount++;
                } catch (EOFException endOfJournal) {
                    break;
                }
            }
            redundantOpCount = lineCount - lruEntries.size();

            // If we ended on a truncated line, rebuild the journal before appending to it.
            if (reader.hasUnterminatedLine()) {
                rebuildJournal();
            } else {
                journalWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(journalFile, true), Utils.US_ASCII));
            }
        } finally {
            Utils.closeQuietly(reader);
        }
    }

    private void readJournalLine(String line) throws IOException {
        int firstSpace = line.indexOf(' ');
        if (firstSpace == -1) {
            throw new IOException("unexpected journal line: " + line);
        }

        int keyBegin = firstSpace + 1;
        int secondSpace = line.indexOf(' ', keyBegin);
        final String key;
        if (secondSpace == -1) {
            key = line.substring(keyBegin);
            if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
                lruEntries.remove(key);
                return;
            }
        } else {
            key = line.substring(keyBegin, secondSpace);
        }

        Entry entry = lruEntries.get(key);
        if (entry == null) {
            entry = new Entry(key);
            lruEntries.put(key, entry);
        }

        if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
            String[] parts = line.substring(secondSpace + 1).split(" ");
            entry.readable = true;
            entry.currentEditor = null;
            entry.setLengths(parts);
        } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
            entry.currentEditor = new Editor(entry);
        } else if (secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ)) {
            // This work was already done by calling lruEntries.get().
        } else {
            throw new IOException("unexpected journal line: " + line);
        }
    }

    /**
     * 计算初始大小并收集垃圾作为打开的一部分缓存.
     * 当条目被认为是不一致的，并且会被删除。
     *
     * @throws IOException
     */
    private void processJournal() throws IOException {
        deleteIfExists(journalFileTmp);
        for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
            Entry entry = i.next();
            if (entry.currentEditor == null) {
                for (int t = 0; t < valueCount; t++) {
                    size += entry.lengths[t];
                }
            } else {
                entry.currentEditor = null;
                for (int t = 0; t < valueCount; t++) {
                    deleteIfExists(entry.getCleanFile(t));
                    deleteIfExists(entry.getDirtyFile(t));
                }
                i.remove();
            }
        }
    }


    /**
     * 创建一个新日志文件，省略掉多余信息，
     *
     * @throws IOException IO异常
     */
    private synchronized void rebuildJournal() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
        }

        Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(journalFileTmp), Utils.US_ASCII));
        try {
            writer.write(MAGIC);
            writer.write("\n");
            writer.write(VERSION_1);
            writer.write("\n");
            writer.write(Integer.toString(appVersion));
            writer.write("\n");
            writer.write(Integer.toString(valueCount));
            writer.write("\n");
            writer.write("\n");

            for (Entry entry : lruEntries.values()) {
                if (entry.currentEditor != null) {
                    writer.write(DIRTY + ' ' + entry.key + '\n');
                } else {
                    writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
                }
            }
        } finally {
            writer.close();
        }

        if (journalFile.exists()) {
            renameTo(journalFile, journalFileBackup, true);
        }
        renameTo(journalFileTmp, journalFile, false);
        journalFileBackup.delete();

        journalWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(journalFile, true), Utils.US_ASCII));
    }

    /**
     * 删除文件
     *
     * @param file 文件
     * @throws IOException IO异常
     */
    private static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException();
        }
    }

    /**
     * 重命名文件名
     *
     * @param from              原始文件
     * @param to                新文件
     * @param deleteDestination 是否删除新文件
     * @throws IOException io异常
     */
    private static void renameTo(File from, File to, boolean deleteDestination) throws IOException {
        if (deleteDestination) {
            deleteIfExists(to);
        }
        if (!from.renameTo(to)) {
            throw new IOException();
        }
    }

    /**
     * Returns a snapshot of the entry named {@code key}, or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     */
    public synchronized Snapshot get(String key) throws IOException {
        checkNotClosed();
        validateKey(key);
        Entry entry = lruEntries.get(key);
        if (entry == null) {
            return null;
        }

        if (!entry.readable) {
            return null;
        }

        // Open all streams eagerly to guarantee that we see a single published
        // snapshot. If we opened streams lazily then the streams could come
        // from different edits.
        InputStream[] ins = new InputStream[valueCount];
        try {
            for (int i = 0; i < valueCount; i++) {
                ins[i] = new FileInputStream(entry.getCleanFile(i));
            }
        } catch (FileNotFoundException e) {
            // A file must have been deleted manually!
            for (int i = 0; i < valueCount; i++) {
                if (ins[i] != null) {
                    Utils.closeQuietly(ins[i]);
                } else {
                    break;
                }
            }
            return null;
        }

        redundantOpCount++;
        journalWriter.append(READ + ' ' + key + '\n');
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }

        return new Snapshot(key, entry.sequenceNumber, ins, entry.lengths);
    }

    /**
     * 返回一个键为Key的编辑器，如果返回为null, 编辑器正在运行
     *
     * @param key
     * @return
     * @throws IOException
     */
    public Editor edit(String key) throws IOException {
        return edit(key, ANY_SEQUENCE_NUMBER);
    }

    private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
        checkNotClosed();
        validateKey(key);
        Entry entry = lruEntries.get(key);
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
                || entry.sequenceNumber != expectedSequenceNumber)) {
            return null; // Snapshot is stale.
        }
        if (entry == null) {
            entry = new Entry(key);
            lruEntries.put(key, entry);
        } else if (entry.currentEditor != null) {
            return null; // Another edit is in progress.
        }

        Editor editor = new Editor(entry);
        entry.currentEditor = editor;

        // Flush the journal before creating files to prevent file leaks.
        journalWriter.write(DIRTY + ' ' + key + '\n');
        journalWriter.flush();
        return editor;
    }

    /**
     * 返回该缓存存储数据的目录.
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * 返回该缓存用来存储数据的最大字节数。
     */
    public synchronized long getMaxSize() {
        return maxSize;
    }

    /**
     * 更改缓存可以存储和队列的最大字节数
     * 如有必要，可以对现有的商店进行修剪。
     */
    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        executorService.submit(cleanupCallable);
    }

    /**
     * 返回当前用于存储该缓存中的值的字节数。如果后台删除正在等待，那么这个值可能大于最大的大小
     */
    public synchronized long size() {
        return size;
    }

    /**
     * 完成编辑
     */
    private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (entry.currentEditor != editor) {
            throw new IllegalStateException();
        }

        // If this edit is creating the entry for the first time, every index must have a value.
        if (success && !entry.readable) {
            for (int i = 0; i < valueCount; i++) {
                if (!editor.written[i]) {
                    editor.abort();
                    throw new IllegalStateException("Newly created entry didn't create value for index " + i);
                }
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort();
                    return;
                }
            }
        }

        for (int i = 0; i < valueCount; i++) {
            File dirty = entry.getDirtyFile(i);
            if (success) {
                if (dirty.exists()) {
                    File clean = entry.getCleanFile(i);
                    dirty.renameTo(clean);
                    long oldLength = entry.lengths[i];
                    long newLength = clean.length();
                    entry.lengths[i] = newLength;
                    size = size - oldLength + newLength;
                }
            } else {
                deleteIfExists(dirty);
            }
        }

        redundantOpCount++;
        entry.currentEditor = null;
        if (entry.readable | success) {
            entry.readable = true;
            journalWriter.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++;
            }
        } else {
            lruEntries.remove(entry.key);
            journalWriter.write(REMOVE + ' ' + entry.key + '\n');
        }
        journalWriter.flush();

        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }
    }

    /**
     * 我们只会重建日志，因为它会将期刊的大小减半，并至少消除2000个操作。
     */
    private boolean journalRebuildRequired() {
        final int redundantOpCompactThreshold = 2000;
        return redundantOpCount >= redundantOpCompactThreshold //
                && redundantOpCount >= lruEntries.size();
    }

    /**
     * 如果存在，则删除@code键的条目，并且可以删除。被编辑的条目不能被删除。
     *
     * @param key key
     * @return boolean
     * @throws IOException
     */
    public synchronized boolean remove(String key) throws IOException {
        checkNotClosed();
        validateKey(key);
        Entry entry = lruEntries.get(key);
        if (entry == null || entry.currentEditor != null) {
            return false;
        }

        for (int i = 0; i < valueCount; i++) {
            File file = entry.getCleanFile(i);
            if (file.exists() && !file.delete()) {
                throw new IOException("failed to delete " + file);
            }
            size -= entry.lengths[i];
            entry.lengths[i] = 0;
        }

        redundantOpCount++;
        journalWriter.append(REMOVE + ' ' + key + '\n');
        lruEntries.remove(key);

        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable);
        }

        return true;
    }

    /**
     * 如果该缓存已被关闭，则返回true。
     */
    public synchronized boolean isClosed() {
        return journalWriter == null;
    }

    /**
     * 检查是否关闭
     */
    private void checkNotClosed() {
        if (journalWriter == null) {
            throw new IllegalStateException("cache is closed");
        }
    }

    /**
     * 强制缓冲操作到文件系统.
     */
    public synchronized void flush() throws IOException {
        checkNotClosed();
        trimToSize();
        journalWriter.flush();
    }

    /**
     * 关闭这个缓存。存储的值将保留在文件系统中。
     */
    @Override
    public synchronized void close() throws IOException {
        if (journalWriter == null) {
            return; // Already closed.
        }
        for (Entry entry : new ArrayList<Entry>(lruEntries.values())) {
            if (entry.currentEditor != null) {
                entry.currentEditor.abort();
            }
        }
        trimToSize();
        journalWriter.close();
        journalWriter = null;
    }

    private void trimToSize() throws IOException {
        while (size > maxSize) {
            Map.Entry<String, Entry> toEvict = lruEntries.entrySet().iterator().next();
            remove(toEvict.getKey());
        }
    }

    /**
     * 关闭缓存并删除其所有的存储值
     *
     * @throws IOException
     */
    public void delete() throws IOException {
        close();
        Utils.deleteContents(directory);
    }

    /**
     * 验证Key
     *
     * @param key
     */
    private void validateKey(String key) {
        Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("keys must match regex "
                    + STRING_KEY_PATTERN + ": \"" + key + "\"");
        }
    }

    /**
     * inputStream转化为String
     *
     * @param in
     * @return
     * @throws IOException
     */
    private static String inputStreamToString(InputStream in) throws IOException {
        return Utils.readFully(new InputStreamReader(in, Utils.UTF_8));
    }

    /**
     * 一个条目的值的快照.
     */
    public final class Snapshot implements Closeable {
        private final String key;
        private final long sequenceNumber;
        private final InputStream[] ins;
        private final long[] lengths;

        private Snapshot(String key, long sequenceNumber, InputStream[] ins, long[] lengths) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.ins = ins;
            this.lengths = lengths;
        }

        /**
         * 返回这个快照的编辑器的编辑器，如果条目已经被创建或者正在进行中的另一个编辑正在进行，则返回该条目的编辑器。
         */
        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(key, sequenceNumber);
        }

        /**
         * 返回未缓冲的流，并带有@code索引的值。
         */
        public InputStream getInputStream(int index) {
            return ins[index];
        }

        /**
         * 返回@code索引的字符串值。
         */
        public String getString(int index) throws IOException {
            return inputStreamToString(getInputStream(index));
        }

        /**
         * 返回@code index的值的字节长度。
         */
        public long getLength(int index) {
            return lengths[index];
        }

        public void close() {
            for (InputStream in : ins) {
                Utils.closeQuietly(in);
            }
        }
    }

    private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            // Eat all writes silently. Nom nom.
        }
    };

    /**
     * 编辑条目的值。
     */
    public final class Editor {
        private final Entry entry;
        private final boolean[] written;
        private boolean hasErrors;
        private boolean committed;

        private Editor(Entry entry) {
            this.entry = entry;
            this.written = (entry.readable) ? null : new boolean[valueCount];
        }

        /**
         * 返回一个未缓冲的输入流来读取最后一个提交的值，
         * 如果没有值，则为null。
         */
        public InputStream newInputStream(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    return null;
                }
                try {
                    return new FileInputStream(entry.getCleanFile(index));
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        }

        /**
         * 将最后一个提交的值作为字符串返回，如果没有值，则返回null。
         */
        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            return in != null ? inputStreamToString(in) : null;
        }

        /**
         * 返回一个新的未缓冲的输出流，以在@code索引中写入值。如果底层输出流在写入文件系统时遇到错误，那么当调用@link commit时，该编辑将被中止。返回的输出流不会抛出ioexception。
         */
        public OutputStream newOutputStream(int index) throws IOException {
            if (index < 0 || index >= valueCount) {
                throw new IllegalArgumentException("Expected index " + index + " to "
                        + "be greater than 0 and less than the maximum value count "
                        + "of " + valueCount);
            }
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    written[index] = true;
                }
                File dirtyFile = entry.getDirtyFile(index);
                FileOutputStream outputStream;
                try {
                    outputStream = new FileOutputStream(dirtyFile);
                } catch (FileNotFoundException e) {
                    // Attempt to recreate the cache directory.
                    directory.mkdirs();
                    try {
                        outputStream = new FileOutputStream(dirtyFile);
                    } catch (FileNotFoundException e2) {
                        // We are unable to recover. Silently eat the writes.
                        return NULL_OUTPUT_STREAM;
                    }
                }
                return new FaultHidingOutputStream(outputStream);
            }
        }

        /**
         * 将@code索引的值显示为@code值。
         */
        public void set(int index, String value) throws IOException {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(newOutputStream(index), Utils.UTF_8);
                writer.write(value);
            } finally {
                Utils.closeQuietly(writer);
            }
        }

        /**
         * 中止这个编辑。这将释放edit锁，以便在同一个键上启动另一个编辑。
         */
        public void abort() throws IOException {
            completeEdit(this, false);
        }

        public void abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort();
                } catch (IOException ignored) {
                }
            }
        }

        /**
         * 提交此编辑，使其对读者可见。这将释放edit锁，以便在同一个键上启动另一个编辑。
         */
        public void commit() throws IOException {
            if (hasErrors) {
                completeEdit(this, false);
                remove(entry.key); // The previous entry is stale.
            } else {
                completeEdit(this, true);
            }
            committed = true;
        }

        private class FaultHidingOutputStream extends FilterOutputStream {
            private FaultHidingOutputStream(OutputStream out) {
                super(out);
            }

            @Override
            public void write(int oneByte) {
                try {
                    out.write(oneByte);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            @Override
            public void write(byte[] buffer, int offset, int length) {
                try {
                    out.write(buffer, offset, length);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            @Override
            public void close() {
                try {
                    out.close();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            @Override
            public void flush() {
                try {
                    out.flush();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
        }
    }


    private final class Entry {
        private final String key;
        //文件长度
        private final long[] lengths;
        //是否被读过
        private boolean readable;
        //没有被编辑的
        private Editor currentEditor;
        //序列号
        private long sequenceNumber;


        private Entry(String key) {
            this.key = key;
            this.lengths = new long[valueCount];
        }


        public String getLengths() throws IOException {
            StringBuilder result = new StringBuilder();
            for (long size : lengths) {
                result.append(' ').append(size);
            }
            return result.toString();
        }

        private void setLengths(String[] strings) throws IOException {
            if (strings.length != valueCount) {
                throw invalidLengths(strings);
            }

            try {
                for (int i = 0; i < strings.length; i++) {
                    lengths[i] = Long.parseLong(strings[i]);
                }
            } catch (NumberFormatException e) {
                throw invalidLengths(strings);
            }
        }

        private IOException invalidLengths(String[] strings) throws IOException {
            throw new IOException("unexpected journal line: " + java.util.Arrays.toString(strings));
        }

        public File getCleanFile(int i) {
            return new File(directory, key + "." + i);
        }

        public File getDirtyFile(int i) {
            return new File(directory, key + "." + i + ".tmp");
        }
    }


}
