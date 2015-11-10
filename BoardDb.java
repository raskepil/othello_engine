import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class BoardDb {
  class Iterator {
    boolean next() {
      if (m_fix_cur != null) {
        m_fix_cur = null;
        m_fix+=3;
      }
      else
        m_cur = m_var.hasNext() ? m_var.next() : null;
      if (m_fix == m_loaded.length) {
        if (m_cur == null)
          return false;
      }
      else {
        m_fix_cur = new BitBoard(m_loaded[m_fix+0],m_loaded[m_fix+1]);
        if (m_cur != null) {
          int c = m_fix_cur.compare(m_cur.getKey());
          if (c == 1)
            m_fix_cur = null;
          else if (c != -1)
            throw new RuntimeException();
        }
      }
      return true;
    }
    BitBoard raw() {
      return m_fix_cur != null ? m_fix_cur : m_cur.getKey();
    }
    long raw_value() {
      return m_fix_cur != null ? m_loaded[m_fix+2] : m_cur.getValue();
    }
    public int value() {
      return (short)raw_value();
    }
    java.util.Iterator<Entry<BitBoard, Long>> m_var = m_add.entrySet().iterator();
    Entry<BitBoard, Long>                     m_cur;
    int                           m_fix = 0;
    BitBoard                      m_fix_cur;
  }
  static class Reader {
    static class Data {
      long a, b;
      int  value() { return (short) raw_value; }
      long raw_value;
    }
    Reader(String name) {
      try {
        stream = new DataInputStream(new BufferedInputStream(new FileInputStream(name)));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    int count() {
      try {
        return stream == null ? 0 : stream.available() / 24;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return 0;
    }
    Data read() {
      if (stream == null)
        return null;
      try {
        data.a   = stream.readLong();
        data.b   = stream.readLong();
        data.raw_value = stream.readLong();
      } catch (IOException ef) {
        try {
          stream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        return null;
      }
      return data;
    }
    private DataInputStream stream;
    private Data            data = new Data();
  }
  BoardDb() {}
  BoardDb(long[] data) {
    for (int j = 0; j < data.length; j += 3)
      m_add.put(new BitBoard(data[j], data[j + 1]), data[j + 2]);
  }
  static Board.Pos precalc(Board gboard, byte col) {
    BitBoard board = new BitBoard(gboard);
    if (s_pre == null)
      s_pre = new BoardDb(s_predata);
    int mapid = board.normalize(col);
    Long n = s_pre.m_add.get(board);
    if (n == null)
      return null;
    return new Board.Pos(n.intValue() % 8, n.intValue() / 8).map(mapid^((mapid&6)==4?1:0));
  }
  int fixedFind(BitBoard b) {
    int h = m_loaded.length/3;
    int l = 0;
    BitBoard bb = new BitBoard(0,0);
    long[] aa = new long[2];
    while (l < h) {
      int t = (h+l)/2;
      aa[0] = m_loaded[3*t];
      aa[1] = m_loaded[3*t+1];
      bb.fromStore(aa);
      int c = b.compare(bb);
      if (c == 0)
        return t;
      else if (c < 0)
        h = t;
      else
        l = t+1;
    }
    return -1;
  }
  public boolean insert(BitBoard b) {
    return fixedFind(b) == -1 && m_add.put(b,0L) == null;
  }
  public Long insert(BitBoard b, long n) {
    int f = fixedFind(b);
    if (f != -1)
      return m_loaded[3*f+2];
    return m_add.put(b,n);
  }
  public Long added_get(BitBoard b) {
    return m_add.get(b);
  }
  //public void remove(BitBoard key) {
  //  m_set.remove(key);
  //}
  public int size() { return m_loaded.length/3+m_add.size(); }
  public void writeAll(String name) {
    try {
      DataOutputStream s = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)));
      for (Iterator entry = new Iterator(); entry.next();) {
        s.writeLong(entry.raw().toStore()[0]);
        s.writeLong(entry.raw().toStore()[1]);
        s.writeLong(entry.raw_value());
      }
      s.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  public void readAll(String name) {
    Reader read = new Reader(name);
    Reader.Data data;
    int count = read.count();
    m_loaded = new long[3*count];
    for (int set = 0; true; set++) {
      data = read.read();
      if (data == null) {
        if (set == count)
          break;
        throw new RuntimeException();
      }
      m_loaded[3*set+0] = data.a;
      m_loaded[3*set+1] = data.b;
      m_loaded[3*set+2] = data.raw_value;
    }
  }
  public void clear() {
    m_add.clear();
    m_loaded = new long[0];
  }
  private TreeMap<BitBoard,Long> m_add = new TreeMap<BitBoard,Long>(new Comparator<BitBoard>() {
    public int compare(BitBoard a, BitBoard b) {
      return a.compare(b);
    }
  });
  private long[] m_loaded               = new long[0];
  private static BoardDb      s_pre     = null;
  private static final long[] s_predata = {
    0x8000000L, 0x3810000000L, 45, // 10
    0x810080000L, 0x1008040000L, 26, // 10
  };
}
