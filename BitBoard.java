public final class BitBoard extends Board {
  public BitBoard(   Board b    )   { fromStore(b.toStore()); }
  public BitBoard(long w, long b)   { m_wht = w;  m_blk = b; }
  public BitBoard(Node node) {
    bMoveIterator it = (bMoveIterator)node;
    m_wht = it.m_cur; //< root-node
    m_blk = it.m_emy;
  }

  public byte get(Pos p) {
    return (byte) (e_white * (int)(m_wht >>> p.bitpos() & 1) +
                   e_black * (int)(m_blk >>> p.bitpos() & 1));
  }

  void dump() {
    System.out.println("");
    Moves it = node(Board.e_white).moves();
    it.next();
    for (Pos p = new Pos(); p.Next();) {
      byte c = get(p);
      boolean l = false;// it.pos().equals(p);
      if (l)
        it.next();
      if (c == 0)
        System.out.print(l ? "o" : ".");
      else
        System.out.print(c == -1 ? "M" : "p");
      if (p.x() == 7)
        System.out.println("");
    }
  }
  public int compare(BitBoard o) {
    int r = Long.compare(m_wht, o.m_wht);
    return r == 0 ? Long.compare(m_blk, o.m_blk) : r;
  }
  public String toString() {
    return String.format("0x%xL, 0x%xL", m_wht, m_blk);
  }

  void map(int n) {
    if ((n & 3) == 3) {
      long cc = 0, nn = 0;
      for (int j = 0; j < 8; j++) {
        cc = (cc >>> 1) | (m_wht >>> 56) * 0x8040201008040201L & 0x8080808080808080L;
        m_wht <<= 8;
        nn = (nn >>> 1) | (m_blk >>> 56) * 0x8040201008040201L & 0x8080808080808080L;
        m_blk <<= 8;
      }
      m_wht = cc;
      m_blk = nn;
    }
    if ((n & 3) == 1) {
      m_wht = Long.reverse(m_wht);
      m_blk = Long.reverse(m_blk);
    }
    if ((n & 1) == 0) {
      m_wht = Long.reverseBytes(m_wht);
      m_blk = Long.reverseBytes(m_blk);
    }
  }

  public int normalize(byte col) {
    col >>= 1;
    long l = m_wht & ~col | m_blk & col;
    m_blk  = m_blk & ~col | m_wht & col;
    m_wht  = l;
    BitBoard b = new BitBoard(this);
    int bj = 0;
    for (int j = 0; j < 8; j++) {
      b.map(j);
      if (compare(b) >= 0) {
        m_blk = b.m_blk;
        m_wht = b.m_wht;
        bj = j;
      }
    }
    return bj;
  }
  public int count() {
    return Long.bitCount(m_wht | m_blk);
  }
  public boolean equals(Object o) {
    BitBoard b = (BitBoard) o;
    long ms = m_blk ^ m_wht;
    long os = b.m_blk ^ b.m_wht;
    long mp = m_blk & ms;
    return (ms == os) && (mp == (os & b.m_blk));
  }
  public long[] toStore() {
    return new long[] {m_wht,m_blk};
  }
  public void fromStore(long[] arr) {
    m_wht = arr[0];
    m_blk = arr[1];
  }
  static public class QEval2step implements Eval {
    public int score(Node it) {
      BitBoard bb = new BitBoard(it);
      int ply = 64-bb.count(); 
      Searcher search = new Searcher(bb);
      return search.combined_search_fixedPly(e_next, 2, ply-1, false);
    }
  }
  static final Eval s_end_eval = new Eval() {
    public int score(Node it) {
      bMoveIterator bim = (bMoveIterator)it;
      return toFinal(Long.bitCount(bim.m_cur) - Long.bitCount(bim.m_emy));
    }
  };
  static private long eval1_line(long mask,long emy,long T,int stride) {
    long res = 0;
    long t = T;
    long m = mask & emy;
    while (((t <<= stride) & m) != 0)
      res |= t;
    if ((t&mask) == 0)
      res = 0;
    mask = stride != 7 ? mask >>> 1 : mask << 1;
    t = 0;
    emy &= mask;
    while (((T >>>= stride) & emy) != 0)
      t |= T;
    if ((T&mask) != 0)
      res |= t;
    return res;
  }
  static final Eval s_end_eval1 = new Eval() {
    public int score(Node it) {
      bMoveIterator bim = (bMoveIterator)it;
      long cur = bim.m_cur;
      long emy = bim.m_emy;
      long clr = ~cur ^ emy;
      if (Long.bitCount(clr) != 1)
        throw new RuntimeException();
      //new BitBoard(emy,cur).dump();
      long res = eval1_line(-1, cur, clr, 8);
      long mask = 0xfefefefefefefefeL;
      res |= eval1_line(mask, cur, clr, 1);
      res |= eval1_line(mask, cur, clr, 9);
      res |= eval1_line(mask>>>1, cur, clr, 7);
      int totc = 62;
      emy |= res;
      if (res == 0) {
        res  = eval1_line(-1, emy, clr, 8);
        res |= eval1_line(mask, emy, clr, 1);
        res |= eval1_line(mask, emy, clr, 9);
        res |= eval1_line(mask>>>1, emy, clr, 7);
        emy &= ~res;
        totc += res != 0 ? 2 : 1;
      }
      return toFinal(totc - 2*Long.bitCount(emy));
    }
  };
  static private final class QEvalOld implements Eval {
    QEvalOld(EvalBoardOld ev, int rem) {
      m_eboard = new byte[64];
      m_sboard = new short[4 * 256];
      for (Board.Pos p = new Board.Pos(); p.Next();)
        m_eboard[p.bitpos()] = ev.evalAt(p,rem);
      for (int y = 0; y < 4; y++)
        for (int x = 0; x < 256; x++) {
          short sum = 0;
          for (int j = 0; j < 8; j++)
            sum += m_eboard[8 * y + j] * (x >> j & 1);
          m_sboard[256 * y + x] = sum;
        }
    }
    public int score(Node it) {
      bMoveIterator bim = (bMoveIterator)it;
      return scoreFunc(bim.m_cur) - scoreFunc(bim.m_emy);
    }
    int scoreFunc(long l) {
      int r;
      r  = m_sboard[32 *  0 + ((int) (l       ) & 255)];
      r += m_sboard[32 *  8 + ((int) (l >>>= 8) & 255)];
      r += m_sboard[32 * 16 + ((int) (l >>>= 8) & 255)];
      r += m_sboard[32 * 24 + ((int) (l >>>= 8) & 255)];
      r += m_sboard[32 * 24 + ((int) (l >>>= 8) & 255)];
      r += m_sboard[32 * 16 + ((int) (l >>>= 8) & 255)];
      r += m_sboard[32 *  8 + ((int) (l >>>= 8) & 255)];
      r += m_sboard[32 *  0 + ((int) (l >>>= 8) & 255)];
      return r;
    }
    byte [] m_eboard;
    short[] m_sboard;
  }
/*  static long s_con_time = 0;
  s_con_time -= System.nanoTime();
  s_con_time += System.nanoTime();/**/
  // !* BitIterator
  static abstract class bNode {
    bNode() {
      m_1 = m_7 = m_8 = m_9 = 0;
      m_top = null;
    }
    bNode(bNode b, boolean f) {
      m_1   = b.m_1;
      m_7   = b.m_7;
      m_8   = b.m_8;
      m_9   = b.m_9;
      m_res = b.m_res;
      m_top = b.m_top;
      m_pos = b.m_pos;
    }
    bNode(ParNode b) {
      m_top    = b;
      long clr = ~m_top.m_emy ^ m_top.m_cur;
      m_8      = lineSet(m_top.m_cur, clr, 8);
      long cur = m_top.m_cur & 0x7e7e7e7e7e7e7e7eL;
      m_1      = lineSet(cur, clr, 1);
      m_7      = lineSet(cur, clr, 7);
      m_9      = lineSet(cur, clr, 9);  //< iterateBench: 10 / 139ms
      m_pos    = -1;
    }
/*
 *     .
 *  sss
 * 1000
 * 
 *     .
 *    
 *    1
 */
    private long lineSet(long cur, long clr, int stride) {
      long d = m_top.m_emy;
      long c = d <<  stride & cur;
      d  = d >>> stride & cur;
      c |= c <<  stride & cur;
      d |= d >>> stride & cur;
      c |= c <<  stride & cur;
      d |= d >>> stride & cur;
      c |= c <<  stride & cur;
      d |= d >>> stride & cur;
      c |= c <<  stride & cur;
      d |= d >>> stride & cur;
/*      if (stride == 1) {
        long cc = c | (c <<  stride);
//        cc |= ((cc&cur)<<stride)&clr;
        long cx = (cur&(m_top.m_emy<<1)) + cur;
        cx ^= cur;
       // cx &= (cur|clr);
        if (cx%256 != cc%256) {
          System.out.printf("\n%x\n%x\n",cc, cx);
          System.out.printf("\n%x\n%x\n",m_top.m_cur, m_top.m_emy);
          c |= 0;
        }
      }/**/
      d |= c | (c <<  stride | d >>> stride) & cur;
      c = (d << stride | d >>> stride) & clr;
      m_res |= c;
      return d | c;
    }
    protected int           m_pos;
    protected long          m_res;
    protected final long    m_1, m_7, m_8, m_9;
    protected final ParNode m_top;
  }

  // !* BitDoIterator
  static abstract class ParNode extends bNode implements Node {
    private ParNode() {}
    ParNode(ParNode b) {
      super(b);
      m_qeval = b.m_qeval;
    }
    public Node setEval(Eval eval, int ply) {
      int remaining = 64 - count() - ply;
      if (remaining == 0)
        m_qeval = s_end_eval;
      else if (remaining == 1)
        m_qeval = s_end_eval1;
      else if (eval instanceof EvalBoardOld)
        m_qeval = new QEvalOld((EvalBoardOld)eval, remaining);
      else if (eval != null)
        m_qeval = eval;
      return this;
    }
    public void evalFrom(Node node) {
      m_qeval = ((ParNode)node).m_qeval;
    }
    ParNode(ParNode r, boolean f) {
      super(r,f);
      m_cur   = r.m_cur;
      m_emy   = r.m_emy;
      m_qeval = r.m_qeval;
    }

    public Node clone() {
      //return new bMoveIterator(this,false);
      bMoveIterator r = new bMoveIterator();
      r.m_cur   = m_cur;
      r.m_emy   = m_emy;
      r.m_qeval = m_qeval;
      return r;
    }
    public Moves moves() {
      return new bMoveIterator(this);
    }
    int movesDelta() {
      int movs = Long.bitCount(m_res);
      return movs - Long.bitCount(m_top.m_res);
    }
    public int count() {
      return Long.bitCount(m_emy|m_cur);
    }
    int movesDelta2() {
      int movs = Long.bitCount(m_res);
      return movs - Long.bitCount(new bMoveIterator(this).m_res);
    }
    int movesDelta3() {
      if ((m_emy|m_cur)==0)
        throw new RuntimeException();
      Moves it = moves();
      int our = 0, thr = 0;
      Node iter;
      if (null != (iter = it.next())) {
        do {
          our++;
          bMoveIterator i2 = (bMoveIterator)(iter.moves());
          thr += Long.bitCount(i2.m_res);
        } while (null != (iter = it.next()));
        return 10*thr/our - 10*our;
      }
      else {
        bMoveIterator i2 = (bMoveIterator)(it.forfeit());
        thr += Long.bitCount(i2.m_res);
        return -10*thr;
      }
    }
    public int compare(Node o) {
      ParNode b = (ParNode) o;
      int r = Long.compare(m_cur, b.m_cur);
      return r == 0 ? Long.compare(m_emy, b.m_emy) : r;
    }
    public boolean equals(Object o) {
      ParNode b = (ParNode) o;
      return (m_emy == b.m_emy) && (m_cur == b.m_cur);
    }
    public int score() {
      //int t = 0;
      //for (int n = 64; --n >= 0;)
      //  t += ((int)(m_emy >>> n & 1) - (int)(m_cur >>> n & 1)) *
      //    m_qeval.m_eboard[n];
      //return t;
      return m_qeval.score(this); //< iterateBench: 1 / 139
    }
    public int finalScore() {
      return toFinal(tileDelta());
    }
    int tileDelta() {
      return Long.bitCount(m_cur) - Long.bitCount(m_emy);
    }
    int frontTileDelta() {
      /*
       * int ftl = 0; for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
       * int xy = 8*y+x; if (((m_cur|m_emy)>>>xy & 1) == 0) continue; int up =
       * -1+2*((int)(m_cur>>>xy) & 1); for (int dx = -1; dx <= 1; dx++) for (int dy
       * = -1; dy <= 1; dy++) { if (dx == 0 && dy == 0) continue; if (dx+x < 0
       * || dx+x >= 8) continue; if (dy+y < 0 || dy+y >= 8) continue; if
       * (((m_cur|m_emy)>>>(8*(y+dy)+x+dx) & 1) == 0) ftl += up; } }
       */
      long notb = ~m_cur ^ m_emy;
      long l = ~0x8080808080808080L;
      long r = ~0x0101010101010101L;
      int ft = 0;
      long t = (notb & l) << 1;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = (notb & r) >>> 1;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = notb << 8;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = notb >>> 8;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = (notb & l) >>> 7;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = (notb & r) << 7;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = (notb & r) >>> 9;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      t = (notb & l) << 9;
      ft += Long.bitCount(t & m_cur) - Long.bitCount(t & m_emy);
      return ft;
    }
    int neighborTile() {
      long l = ~0x8080808080808080L;
      int a = 0;
      int b = 0;
      a += Long.bitCount(((m_cur & l) << 1) & m_cur);
      a += Long.bitCount((m_cur << 8) & m_cur);
      a += Long.bitCount(((m_cur & l) >>> 7) & m_cur);
      a += Long.bitCount(((m_cur & l) << 9) & m_cur);
      b += Long.bitCount(((m_emy & l) << 1) & m_emy);
      b += Long.bitCount((m_emy << 8) & m_emy);
      b += Long.bitCount(((m_emy & l) >>> 7) & m_emy);
      b += Long.bitCount(((m_emy & l) << 9) & m_emy);
      int c = Long.bitCount(m_cur);
      int e = Long.bitCount(m_emy);
      return a - b - 2*c+2*e;// - c*c + e*e - 2*e*c;// - 8*tileDelta();
    }
    int frontTileDeltaRel() {
      long notb = ~m_cur ^ m_emy;
      long l = ~0x8080808080808080L;
      long r = ~0x0101010101010101L;
      long t = (notb & l) << 1;
      int ftc = Long.bitCount(t & m_cur);
      int ftn = Long.bitCount(t & m_emy);
      t = (notb & r) >>> 1;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      t = notb << 8;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      t = notb >>> 8;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      t = (notb & l) >>> 7;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      t = (notb & r) << 7;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      t = (notb & r) >>> 9;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      t = (notb & l) << 9;
      ftc += Long.bitCount(t & m_cur);
      ftn += Long.bitCount(t & m_emy);
      ftc *= Long.bitCount(m_emy);
      ftn *= Long.bitCount(m_cur);
      return (ftc - ftn) / 20;
    }
    long  m_emy;
    long  m_cur;
    Eval  m_qeval;
    private static Eval s_def_evalu = new QEvalOld(new DummyEval(),0);
  }

  static final class bMoveIterator extends ParNode implements Moves {
    private bMoveIterator() {}
    bMoveIterator(ParNode b) { super(b); }
    bMoveIterator(ParNode r, boolean f) { super(r,f); }
    public Node next() {
      m_pos++;
/*      m_pos = m_res & -m_res;
      m_res &= ~m_pos;
      if (m_pos != 0) {
*/
      m_pos += Long.numberOfTrailingZeros(m_res>>>m_pos);
      if (m_pos < 64) {
        dolegal(); //< iterateBench: 55 / 139 ms
        return this;
      }
      return null;
    }
    public Pos createPos() {
      return new Pos(m_pos);
    }
    public bMoveIterator reset() {
      m_pos = -1;
      return this;
    }
    boolean legal(Pos p) {
      return (m_res >>> p.bitpos() & 1) != 0;
    }
    public Node forfeit() {
      m_emy = m_top.m_cur;
      m_cur = m_top.m_emy;
      return this;
    }
    static private long getLine(long m, long T, int stride) {

      long acc;
      T = (T << stride | T >>> stride) & m;
      //if (T == 0)
      //  throw new RuntimeException();
      do {
        acc = T;
        T |= (T << stride | T >>> stride) & m;
      } while(acc != T);
      
 
/*      T  = (T << stride | T >>> stride) & m;
      T |= (T << stride | T >>> stride) & m;
      T |= (T << stride | T >>> stride) & m;
      T |= (T << stride | T >>> stride) & m;
      T |= (T << stride | T >>> stride) & m;
      T |= (T << stride | T >>> stride) & m;
*/      return T;
    }
    static private long getLine2(long m, long T, int stride) {
    /*  long acc;
      do {
        acc = T;
        T |= (T >>> stride) & m;
      } while(acc != T);/**/
/*      T  = (T >>> stride) & m;
      T |= (T >>> stride) & m;
      T |= (T >>> stride) & m;
      T |= (T >>> stride) & m;
      T |= (T >>> stride) & m;
      T |= (T >>> stride) & m;/**/
      m &=  m  >>> stride;
      T |=  T  >>> stride;
      T  = (T >>> stride) & m;
      stride  <<= 1;
      T |= (T >>> stride) & m;
      T |= (T >>> stride) & m;/**/
      return T;
    }
    /*static long s_shift7[] = new long[64];
    static long s_shift9[] = new long[64];
    static {
      for (int j = 0; j < s_shift7.length; j++) {
        s_shift7[j] = 0x0102040810204081L << j%7;
        s_shift9[j] = 0x8040201008040201L << j%9;
      }
    }/**/
    private void dolegal() {
      long bit = 1L << m_pos;
      long set = m_top.m_cur ^ m_top.m_emy | bit;
      long res = 0;
/*      if ((bit & m_1) != 0)
        res |= getLine(m_1 & set, bit, 1);
      if ((bit & m_7) != 0)
        res |= getLine(m_7 & set, bit, 7);
      if ((bit & m_8) != 0)
        res |= getLine(m_8 & set, bit, 8);
      if ((bit & m_9) != 0)
        res |= getLine(m_9 & set, bit, 9); /**/
      if ((bit & m_1) != 0) {
        long ms = set & m_1;
        //ms = (ms + Long.highestOneBit(~((ms|-bit)<<1)) ^ ms) & ms;
        //ms = (ms + getLine3(ms<<1,bit,1) ^ ms) & ms;
/*        long r = bit;// | (bit >>> 1);
        long mss = bit | ms & (ms >>> 1);
        r = r - ((r>>>1)&mss);
        r = r - ((r>>>1)&mss);
        r = r - ((r>>1)&mss);
        r = r - ((r>>1)&mss);
        r = r - ((r>>1)&mss);
        r = r - ((r>>1)&mss);
        r = (ms + r ^ ms) & ms;/**/
        ms = ~(ms + bit) & ms | getLine2(ms,bit,1);
//        if (r != ms)
//          r++;
        res |= ms;
      }
      if ((bit & m_7) != 0) {
        long m = 0x0102040810204081L << m_pos; //s_shift7[m_pos];
        long ms = set & m_7;
        ms = ~((~m | ms) + bit) & ms & m | getLine2(ms, bit, 7);
        //ms = ((ms|=~m) + Long.highestOneBit(~((ms|-bit)<<1)) ^ ms) & ms & m;
        res |= ms;
      }
      if ((bit & m_8) != 0) {
        long m = 0x0101010101010101L << (m_pos);
        long ms = set & m_8;
        ms = ~((~m | ms) + bit) & ms & m | getLine2(ms, bit, 8);
        //ms = ((ms|=~m) + Long.highestOneBit(~((ms|-bit)<<1)) ^ ms) & ms & m;
        res |= ms;
      }
      if ((bit & m_9) != 0) {
        long m = 0x8040201008040201L << m_pos; //s_shift9[m_pos];
        long ms = set & m_9;
        ms = ~((~m | ms) + bit) & ms & m | getLine2(ms, bit, 9);
        //ms = ((ms|=~m) + Long.highestOneBit(~((ms|-bit)<<1)) ^ ms) & ms & m;
        res |= ms;
      }/**/
      m_cur   = m_top.m_emy | res;
      m_emy   = m_top.m_cur & ~res;
    }
    public Node doLegal(Pos pos) {
      if (!legal(pos))
        return null;
      m_pos = pos.bitpos();
      dolegal();
      return this;
    }
  }
  bMoveIterator node(byte col) {
    bMoveIterator r = new bMoveIterator();
    col >>= 1;
    r.m_emy = ~col & m_blk | col & m_wht;
    r.m_cur = ~col & m_wht | col & m_blk;
    r.m_qeval = ParNode.s_def_evalu;
    return r;
  }
  boolean dolegal(Pos p, byte col) {
    bMoveIterator b = new bMoveIterator(node((byte)-col));
    if (null == b.doLegal(p))
      return false;
    m_wht = b.m_cur;
    m_blk = b.m_emy;
    b = node((byte)-col);
    m_wht = b.m_emy;
    m_blk = b.m_cur;
    return true;
  }

  Board played(Pos p, byte col)     { BitBoard b = new BitBoard(this);   return p.ok() && b.dolegal(p, col) ? b : this;  }
  boolean legal(Pos p, byte col)    { return node((byte)-col).moves().doLegal(p) != null;  }
  int evaluate()                    { return node(Board.e_next)  .score(); }

  private long m_wht;
  private long m_blk;
}
