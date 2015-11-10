class IndEv implements Board.Eval {
  public int   score(Board.Node it)    {
    BitBoard.bMoveIterator bim = (BitBoard.bMoveIterator)it;
    BitBoard bb = new BitBoard(bim.m_cur,bim.m_emy);
    int ct = 0;
    for (Board.Pos p = new Board.Pos(); p.Next();)
      if (Evaluator.tri_idx(p) == m_index)
        ct += bb.get(p);
    return ct;
  }
  public byte  m_index = 0;
}
class Scorer {
  static class Cou1nter {
    final static int s_W = 64;
    int count() { int ct = 0; for (int j : m_hits) ct += j; return ct; }
    void add(int ref) {
      if (ref < -6400 || ref > 6400 || ref%100 != 0)
        throw new RuntimeException("illegal range");
      ref = (ref+6400)/100;
      if ((ref&1) != 0)
        ref = ref + (Board.rand.nextBoolean() ? 1 : -1);
      ref >>= 1;
      m_hits[ref]++;
    }
    static double S_S=1;
    static int calc(Action...actions) {
      /*
      int[] hits = new int[s_W+1];
      for (Action a : actions) {
        for (; a != null; a = a.m_another) {
          int tot = 0;
          Counter ct = a.ct();
          for (int j = ct.m_hits.length; --j >= 0;)
            tot += ct.m_hits[j];
          tot = 100*tot/(tot+10);//(int)Math.log(tot+1);
          for (int j = ct.m_hits.length; --j >= 0;)
            hits[j] += tot*ct.m_hits[j];
        }
      }
      int r=0, c=0;
      for (int j = hits.length; --j >= 0;)
        if (hits[j] > r) {
          r = hits[j];
          if (r < 0)
            throw new RuntimeException();
          c = j;
        }
      int tot = 0;
      for (int j = 0; j <= s_W; ++j) {
        int val = hits[j];
        tot += val;
      }
      tot /= 2;
      int j = -1;
      for (; tot > 0; ) {
        int val = hits[++j];
        tot -= val;
      }
      return j*128/s_W*100 - 6400;
//      return c*128/s_W*100 - 6400;
       */
      int r=0, c=0;
      for (Action a : actions) {
        for (; a != null; a = a.m_another) {
          Counter ct = a.ct();
          double f = ct.confidence();
          c += f;
          r += f* ct.mean();
        }
      }
      return r / (c == 0 ? 1 : c);
    }
    int mean() {
      int tot = 0;
      for (int j = 0; j <= s_W; ++j) {
        int val = m_hits[j];
        tot += val;
      }
      tot /= 2;
      int j = -1;
      for (; tot > 0; ) {
        int val = m_hits[++j];
        tot -= val;
      }
      return j*128/s_W*100 - 6400;
    }
    double confidence() {
      int tot = 0;
      for (int j = 0; j <= s_W; ++j) {
        int val = m_hits[j];
        tot += val;
      }
      if (tot == 0)
        tot+=0;
      if (tot == 1)
        tot+=0;
      if (tot == 2)
        tot+=0;
      int s=-1,e=-1;
      int t = 0;
      for (int j = 0; j <= s_W; ++j) {
        int val = m_hits[j];
        t += val;
        if (t*4 > tot && s == -1)
          s = j;
        if (t*4 > 3*tot && e == -1)
          e = j;
      }
      return Math.pow(500.0/(tot <= 1 ? 5 : 1+e-s),S_S)*tot/(tot+10);
    }
    int[] m_hits = new int[s_W+1];
  }
  static class Counter {
    void add(Counter r) {
      m_ct  += r.m_ct;
      m_sum += r.m_sum;
      m_squ += r.m_squ;
    }
    void add(int ref) {
      m_ct++;
      m_sum += ref;
      m_squ += ref*ref;
    }
    static int calc(Action...actions) {
      double r=0, c=0;
      for (Action a : actions) {
        for (; a != null; a = a.m_another) {
          Counter ct = a.ct();
          double f = ct.confidence();
          c += f;
          r += f* ct.mean();
        }
      }
      return (int)(r / (c == 0 ? 1 : c));
    }
    int mean() {
      return (int)(m_ct==0 ? 0 : m_sum/m_ct);
    }
    double confidence() {
      double stddev = m_ct <= 1 ? 909 : 0.1 + Math.sqrt(m_squ*m_ct-m_sum*m_sum);
      return 100000/stddev*m_ct*m_ct/(m_ct+10);
    }
    double stddev() {
      return Math.sqrt(m_squ*m_ct-m_sum*m_sum)/m_ct;
    }
    static int c1 = 0, c2 = 0, c3 = 0, c4=0;
    public double equality(Counter other) {
      //S(r/rs-t/ts)2 = S(r*r/rs/rs-2*r/rs*t/ts+t*t/ts/ts)
      double rs = stddev();
      double ts = other.stddev();
      double m = (mean() - other.mean()) / (1 + rs + ts);
      double ff = (rs-ts)/(1 + rs + ts);
      double r = 1 / (1 + 4*m*m + ff*ff);
      if (r >= 1)
        c1++;
      else if (r > 0.9)
        c2++;
      else if ( r > 0.8)
        c3++;
      else
        c4++;
      return r;
    }
    int count() { return m_ct; }
    private int  m_ct;
    private double m_sum;
    private double m_squ;
  }
  Scorer(int len) { m_a = new Counter[len]; }
  static int guessAt(Scorer.Action...devs) {
    return Counter.calc(devs);
  }
  class Action {
    Action() { m_i = -1; }
    Action(int index) { m_i = index; if (m_a[m_i] == null)  m_a[m_i] = new Counter(); }
    Action append(Action o) {
      o.m_another = m_another;
      m_another = o;
      return o;
    }
    Counter ct() { return m_a[m_i]; }
    public boolean equals(Object a) {
      return ((Action)a).m_i == m_i;
    }
    int calc() {
      return Counter.calc(this);
    }
    void add(int trueres) {
      Scorer.this.add(m_i,trueres);
    }
    int            m_i;
    Action         m_another;
  }
  void reSize(int s) {
    if (s >= m_a.length) {
      Counter [] a = new Counter[s+1];
      for (int j = 0; j < m_a.length; j++)
        a[j] = m_a[j];
      m_a = a;
    }
  }
  static int ge2(BitBoard.bMoveIterator x, int b) {
    return 2*((int)(x.m_cur >> b) & 1) + ((int)(x.m_emy >> b) & 1);
  }
  synchronized private void add(int i, int res) {
    m_a[i].add(res);
  }
  int count() {
    int z = 0;
    for (int k = 0; k < m_a.length; k++)
      z += m_a[k] != null ? 1 : 0;
    return z;
  }
  Counter[] m_a;
}
class FastIndexer {
  FastIndexer(Board.Node node) {
    m_b   = (BitBoard.bMoveIterator) node;
    corn0 = corn0(m_b);
    corn1 = corn1(m_b);
    corn2 = corn2(m_b);
    corn3 = corn3(m_b);
    v00   = vgetH(m_b, 8);
    v01   = vgetL(m_b, 32);
    v10   = vgetH(m_b, 7+8);
    v11   = vgetL(m_b, 7+32);
    h00   = hgetH(m_b, 1);
    h10   = hgetL(m_b, 4);
    h01   = hgetH(m_b, 1+56);
    h11   = hgetL(m_b, 4+56);
  }
  static final byte[] s_up = {0,1,3,4,9,10,12,13};
  static final byte[] s_dn = {0,9,3,12,1,10,4,13};
  public int ge2(int b) { return Scorer.ge2(m_b,b); }
  static int vgetL(BitBoard.bMoveIterator m_b, int b) {
    return (2*(int)(m_b.m_cur>>>b&0x10101) + (int)(m_b.m_emy>>>b&0x10101))*0x10309 >> 16 & 255;
  }
  static int vgetH(BitBoard.bMoveIterator m_b, int b) {
    return (2*(int)(m_b.m_cur>>>b&0x10101) + (int)(m_b.m_emy>>>b&0x10101))*0x90301 >> 16 & 255;
  }
  static int hgetL(BitBoard.bMoveIterator m_b, int b) {
    return 2*s_up[(int)(m_b.m_cur >> b) & 7] + s_up[(int)(m_b.m_emy >> b) & 7];
  }
  static int hgetH(BitBoard.bMoveIterator m_b, int b) {
    return 2*s_dn[(int)(m_b.m_cur >> b) & 7] + s_dn[(int)(m_b.m_emy >> b) & 7];
  }
  static int corn0(BitBoard.bMoveIterator m_b) {
    return (2*(int)(m_b.m_cur     &513) + (int)(m_b.m_emy     &513))*(3+  512) >> 9 & 511;
  }
  static int corn1(BitBoard.bMoveIterator m_b) {
    return (2*(int)(m_b.m_cur>>> 7&129) + (int)(m_b.m_emy>>> 7&129))*(3+  128) >> 7 & 127;
  }
  static int corn2(BitBoard.bMoveIterator m_b) {
    return (2*(int)(m_b.m_cur>>>54&513) + (int)(m_b.m_emy>>>54&513))*(1+3*512) >> 9 & 511;
  }
  static int corn3(BitBoard.bMoveIterator m_b) {
    return (2*(int)(m_b.m_cur>>>49&129) + (int)(m_b.m_emy>>>49&129))*(1+3*128) >> 7 & 127;
  }
  int corn0;
  int corn1;
  int corn2;
  int corn3;
  int v00;
  int v01;
  int v10;
  int v11;
  int h00;
  int h10;
  int h01;
  int h11;
  BitBoard.bMoveIterator m_b;
}
class CornerIndexer extends Scorer {
  CornerIndexer(int w) { super((int) Math.pow(3, w*(w+1)/2)); m_w = w;
    m_pow = (int)Math.pow(3, (m_w*(m_w+1)/2 - (m_w+1)/2)/2); }
  Action index(BitBoard.bMoveIterator b) {
    int upper = 0;
    int lower = 0;
    int diag  = 0;
    for (int y = (m_w+1)/2; --y >= 0;) {
      diag = diag*3 + ge2(b,y*9);
      for (int x = y+1; x < m_w-y; x++) {
        upper = upper*3 + ge2(b,y+x*8);
        lower = lower*3 + ge2(b,x+y*8);
      }
    }
    int m = Math.min(lower, upper);
    return new Action(upper + lower - m + m_pow * (m + m_pow * diag));
  }
  Action index4fast(FastIndexer fi, int which) {
    if (which == 0) {
      int upper = fi.ge2(17);
      upper = upper * 27 + fi.v00;
      int lower = fi.ge2(10);
      lower = lower * 27 + fi.h00;
      int m = Math.min(lower, upper);
      int diag  = fi.corn0;
      return new Action(upper + lower - m + 81 * (m + 81 * diag));
    }
    else if (which == 3) {
      int upper = fi.ge2(56-15);
      upper = upper * 27 + fi.v01;
      int lower = fi.ge2(56-6);
      lower = lower * 27 + fi.h01;
      int m = Math.min(lower, upper);
      int diag  = fi.corn3;
      return new Action(upper + lower - m + 81 * (m + 81 * diag));
    }
    else if (which == 1) {
      int upper = fi.ge2(22);
      upper = upper * 27 + fi.v10;
      int lower = fi.ge2(13);
      lower = lower * 27 + fi.h10;
      int m = Math.min(lower, upper);
      int diag  = fi.corn1;
      return new Action(upper + lower - m + 81 * (m + 81 * diag));
    }
    else {
      int upper = fi.ge2(63-16-1);
      upper = upper * 27 + fi.v11;
      int lower = fi.ge2(63-8-2);
      lower = lower * 27 + fi.h11;
      int m = Math.min(lower, upper);
      int diag  = fi.corn2;
      return new Action(upper + lower - m + 81 * (m + 81 * diag));
    }
  }
  private int m_pow;
  private int m_w;
}
class EdgeIndexer extends Scorer {
  EdgeIndexer() { super((int) Math.pow(3, 10));}
  Action index(BitBoard.bMoveIterator b, int d) {
    int upper = ge2(b,9+d*5);
    int lower = ge2(b,9);
    //upper += 3*ge2(b,9+d*6);
    //lower += 3*ge2(b,9-d);
    for (int y = 0; y < 4; y++) {
      upper = upper*3 + ge2(b,d*(7-y));
      lower = lower*3 + ge2(b,   d*y);
    }
    int m = Math.min(lower, upper);
    int l = 9*9*3;
    return new Action(upper + lower - m + l * m);
  }
  Action indexfast(FastIndexer fi, int which) {
    if (which == 3) {
      int upper = fi.corn3;
      upper = 27*upper + fi.v01;
      int lower = fi.corn0;
      lower = 27*lower + fi.v00;
      int m = Math.min(lower, upper);
      return new Action(upper + lower - m + 243 * m);
    }
    else if (which == 0) {
      int lower = fi.corn0;
      lower = lower * 27 + fi.h00;
      int upper = fi.corn1;
      upper = upper * 27 + fi.h10;
      int m = Math.min(lower, upper);
      return new Action(upper + lower - m + 243 * m);
    }
    else if (which == 2) {
      int lower = fi.corn3;
      lower = lower * 27 + fi.h01;
      int upper = fi.corn2;
      upper = upper * 27 + fi.h11;
      int m = Math.min(lower, upper);
      return new Action(upper + lower - m + 243 * m);
    }
    else {
      int upper = fi.corn2;
      upper = 27*upper + fi.v11;
      int lower = fi.corn1;
      lower = 27*lower + fi.v10;
      int m = Math.min(lower, upper);
      return new Action(upper + lower - m + 243 * m);
    }
  }
}
class WedgeIndexer extends Scorer {
  WedgeIndexer() { super(4*(int) Math.pow(3, 18));}
  Action index(BitBoard.bMoveIterator b, int d) {
    int upper = 0;//ge2(b,9+d*5)-1;
    int lower = 0;//ge2(b,9*3)-1;
    for (int x = 0; x < 3; x++)
      for (int y = 0; y < 4-x; y++) {
        upper = upper*3 + ge2(b, 9*x + (7-2*x)*d - d*y);
        lower = lower*3 + ge2(b, 9*x + d*y);
      }
    upper = upper*2 + ge2(b,9*3+d)-1;
    lower = lower*2 + ge2(b,9*3) -1;
    int m = Math.min(lower, upper);
    int l = 27*27*27*2;
    return new Action(upper + lower - m + l * m);
  }
}
class PieIndexer extends Scorer {
  PieIndexer() { super((int) Math.pow(3, 10)); }
  Action index(BitBoard.bMoveIterator b, int d) {
    int m = 0;
    for (int y = 0; y < 4; y++) {
      for (int x = y; x < 4; x++)
        m = 3*m + ge2(b,9*y+d*x);
    }
    return new Action(m);
  }
}
class EdgeIndexer8 extends Scorer {
  EdgeIndexer8() { super((int) Math.pow(3, 8));}
  Action index(BitBoard.bMoveIterator b, int d) {
    int upper = 0;
    int lower = 0;
    for (int y = 4; --y >= 0;) {
      upper = upper*3 + ge2(b,d*(7-y));
      lower = lower*3 + ge2(b,   d*y);
    }
    int m = Math.min(lower, upper);
    int l = 9*9;
    return new Action(upper + lower - m + l * m);
  }
}
class EdgeIndexer14 extends Scorer {
  EdgeIndexer14() { super((int) Math.pow(3, 14));}
  Action index(BitBoard.bMoveIterator b, int d) {
    int upper = 0;
    int lower = 0;
    for (int y = 3; --y >= 0;) {
      upper = 3*upper + ge2(b,9+d*(5-y));
      lower = 3*lower + ge2(b,9+d*y);
      
    }
    for (int y = 4; --y >= 0;) {
      upper = upper*3 + ge2(b,d*(7-y));
      lower = lower*3 + ge2(b,    d*y);
    }
    int m = Math.min(lower, upper);
    int l = 9*9*3;
    return new Action(upper + lower - m + l * m);
  }
}
//AllEdge 23069830
class AllEdgeIndexer /*extends Scorer*/ {
  AllEdgeIndexer(int size) {  m_corn = new short[4*size]; m_vals = new short[size]; }
  static final int rsize = 32768;//(long)Math.pow(3, 10);
  int count() {
    return m_size;
  }
  Scorer.Action index(BitBoard.bMoveIterator move) {
    return null;//new Action(m_map.get(lindex(move)));
  }
  void train(Board.Node mv, int score) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)mv;
    BitBoard n = new BitBoard(move.m_cur&0xffe7c38181c3e7ffL,
                              move.m_emy&0xffe7c38181c3e7ffL);
    n.normalize(Board.e_next);
    move = (BitBoard.bMoveIterator)n.node(Board.e_next);
    m_corn[4*m_size+0] = add(move,score);
    TrainEval.map(move,0);
    m_corn[4*m_size+1] = add(move,score);
    TrainEval.map(move,1);
    m_corn[4*m_size+2] = add(move,score);
    TrainEval.map(move,2);
    m_corn[4*m_size+3] = add(move,score);
    TrainEval.map(move,1);
    m_vals[m_size++] = (short)score;
  }
  int calc(Board.Node mv) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)mv;
    move.m_cur &= 0xffe7c38181c3e7ffL;
    move.m_emy &= 0xffe7c38181c3e7ffL;
    BitBoard n = new BitBoard(move);
    n.normalize(Board.e_next);
    move = (BitBoard.bMoveIterator)n.node(Board.e_next);
    int tt = m_imp[iindex(move)];
    TrainEval.map(move,0);
    tt += WDT*m_imp[iindex(move)];
    TrainEval.map(move,1);
    tt += WDT*WDT*m_imp[iindex(move)];
    TrainEval.map(move,2);
    tt += WDT*WDT*WDT*m_imp[iindex(move)];
    TrainEval.map(move,1);
    if (m_scores[tt] == null)
      return 0;
    return m_scores[tt].mean();
  }
  short add(BitBoard.bMoveIterator move, int score) {
    short idx = iindex(move);
    if (m_corn_ct[idx] == null)
      m_corn_ct[idx] = new Scorer.Counter();
    m_corn_ct[idx].add(score);
    return idx;
  }
  static final int WDT=64;
  void optimize() {
    System.out.printf("AEI-corner_count: %d", s_corner_count);
    m_imp = new byte[s_corner_count];
    for (int j = 0; j < m_imp.length; j++)
      m_imp[j] = -1;
    byte impc = 0;
    for (double dmin = 0.99; dmin >= 0.8; dmin -=0.01)
      for (int k = 0; k < m_imp.length; k++) {
        if (m_imp[k] != -1)
          continue;
        if (impc == WDT)
          break;
        for (int j = k+1; j < m_imp.length; j++)
          if (m_imp[j] == -1) {
            double diff = m_corn_ct[k].equality(m_corn_ct[j]);
            if (diff >= dmin)
              m_imp[k] = m_imp[j] = impc;
          }
        if (m_imp[k] != -1)
          impc++;
      }
    
    for (int j = 0; j < m_imp.length; j++)
      if (m_imp[j] == -1)
        m_imp[j] = (byte)Board.rand.nextInt(WDT);

    implement();
  }
  void optimizer() {
    m_imp = new byte[s_corner_count];
    for (int j = 0; j < m_imp.length; j++)
      m_imp[j] = (byte)Board.rand.nextInt(WDT);

    implement();
  }

  void implement() {
    m_scores = new Scorer.Counter[WDT*WDT*WDT*WDT];
    for (int j = 0; j < m_size; j++) {
      int tt = m_imp[m_corn[4*j+0]] + WDT*(m_imp[m_corn[4*j+1]] + WDT*(m_imp[m_corn[4*j+2]]+WDT*m_imp[m_corn[4*j+3]]));
      if (m_scores[tt] == null)
        m_scores[tt] = new Scorer.Counter();
      m_scores[tt].add(m_vals[j]);
    }
    MeanDev md = new MeanDev();
    MeanDev tr = new MeanDev();
    for (int j = 0; j < m_size; j++) {
      int tt = m_imp[m_corn[4*j+0]] + WDT*(m_imp[m_corn[4*j+1]] + WDT*(m_imp[m_corn[4*j+2]]+WDT*m_imp[m_corn[4*j+3]]));
      int y = m_scores[tt].mean();
      md.add(y, m_vals[j]);
      tr.add(m_vals[j], m_vals[j]);
    }
    System.out.printf("   score: %f\n", md.quality(tr));
  }
  static short[]_iindex = new short[65536];
  static short  s_corner_count = 0;
  static short iindex(BitBoard.bMoveIterator b) {
    int upper = 0;
    int lower = 0;
    int diag  = 0;
    for (int y = (4+1)/2; --y >= 0;) {
      diag = diag*3 + Scorer.ge2(b,y*9);
      for (int x = y+1; x < 4-y; x++) {
        upper = upper*3 + Scorer.ge2(b,y+x*8);
        lower = lower*3 + Scorer.ge2(b,x+y*8);
      }
    }
    int f = upper + 81 * (lower + 81 * diag);
    if (_iindex[f] == 0)
      _iindex[f] = ++s_corner_count;
    return (short)(_iindex[f]-1);
  }
  Scorer.Counter[] m_corn_ct = new Scorer.Counter[32768];
  short[] m_corn;
  short[] m_vals;
  Scorer.Counter[] m_scores;
  byte [] m_imp;
  int     m_size = 0;
}

/*class EdgeBlobIndexer extends Scorer {
  EdgeBlobIndexer() { super((int) Math.pow(3, 11));}
  Action index(BitBoard.bMoveIterator b, int d) {
    int     res = ge2(b, 3 * d - 1);
    res = 3*res + ge2(b,2*d-1);
    res = 3*res + ge2(b,9+d*5/8);

    for (int y = 8; --y >= 0;) {
      res = res*3 + ge2(b,d*y);
    }
    return new Action(res);
  }
}
class Block9Indexer extends Scorer {
  static final int SIZE = 36-16;
//  Block9Indexer(int j) { super((int) Math.pow(3, 9)); j += j > 17 ? 4 : 0; j += j > 25 ? 4 : 0; j += j > 33 ? 4 : 0; j += j > 41 ? 4 : 0; m_x = j%8; m_y = j/8; }
  Block9Indexer(int j) { super((int) Math.pow(3, 9)); for(int k=6;k<=4*6;k+=6)j+=j>k?4:0;m_x = 1+j%6; m_y = 1+j/6; }
  Action index(BitBoard.bMoveIterator b) {
    int res = 0;
    for (int y = m_y-1; y <= m_y+1; y++)
      for (int x = m_x-1; x <= m_x+1; x++)
        res = 3*res + ge2(b,(x&7)+8*(y&7));
    return new Action(res);
  }
  private int m_x, m_y;
}*/
class IntIndexer extends Scorer {
  IntIndexer() { super(0); }
  Action index(int i) {
    i = 2 * i ^ (i >> 31);
    reSize(i);
    return new Action(i);
  }
  double average() {
    long tot = 0;
    long ct  = 0;
    for (int k = 0; k < m_a.length; k++) {
      int ind = (k ^ ((k & 1) == 1 ? -1 : 0)) / 2;
      if (m_a[k] == null)
        continue;
      long c = m_a[k].count();
      tot += ind * c;
      ct += c;
    }
    return 1.0 * tot / ct;
  }
}
class MeanDev {
  void add(double res, double trueRes) {
    m_ct  ++;
    m_tot += res;
    m_squ += (double)res*res;
    m_2rt += (double)res*trueRes;
  }
  double stddev() {
    return Math.sqrt((m_squ-m_tot*m_tot/m_ct)/m_ct);
  }
  double mean() {
    return m_tot / m_ct;
  }
  double qualityWithMean(MeanDev trueres) {
    //S(r/rs-t/ts)2 = S(r*r/rs/rs-2*r/rs*t/ts+t*t/ts/ts)
    double rs = stddev();
    double ts = trueres.stddev();
    if (trueres.m_ct != m_ct)
      throw new RuntimeException();
    return (m_squ / (rs*rs) - 2*m_2rt/(rs*ts) + trueres.m_squ/(ts*ts)) / m_ct;
  }
  double quality(MeanDev trueres) {
    //S(r/rs-t/ts)2 = S(r*r/rs/rs-2*r/rs*t/ts+t*t/ts/ts)
    double rs = stddev();
    double ts = trueres.stddev();
    double rm = mean();
    double tm = trueres.mean();
    if (trueres.m_ct != m_ct)
      throw new RuntimeException();
    return ((m_squ-m_tot*rm) / (rs*rs) - 2*(m_2rt-m_tot*tm)/(rs*ts) + (trueres.m_squ-tm*trueres.m_tot)/(ts*ts)) / m_ct;
  }
  int m_ct;
  double m_tot;
  double m_squ;
  double m_2rt;
}
class MeanAccum {
  void addArr(int result, Scorer.Action...devs) {
    int max = 1<<devs.length;
    if (m_data == null)
      m_data = new MeanDev[max-1];
    for (int m = 1; m < max; m++) {
      Scorer.Action[] act = new Scorer.Action[Integer.bitCount(m)];
      for (int k = 0, j = 0; j < devs.length; j++) {
        if (((m >> j) & 1) != 0)
          act[k++] = devs[j];
      }
      if (m_data[m-1] == null)
        m_data[m-1] = new MeanDev();
      m_data[m-1].add(Scorer.Counter.calc(act),result);
    }
  }
  void print(MeanDev REF) {
    int len = Integer.numberOfTrailingZeros(Integer.highestOneBit(m_data.length));
    String nulls = "0000000".substring(0,len+1); 
    for (int j = 0; j < m_data.length; j++) {
      String s = Integer.toBinaryString(j+1);
      s = (nulls + s).substring(s.length());
      s = new StringBuilder(s).reverse().toString();
      System.out.printf(" %-7s", s);
    }
    System.out.println();
    for (int j = 0; j < m_data.length; j++)
      System.out.printf(" %.5f", m_data[j].quality(REF));
    System.out.println();
  }
  MeanDev[] m_data;
}

//***************************

/*
4193821 Fail: 927614, old: 1482440 new: 784576
OLD: 1.132. CUr: 0.451. NEW4: 0.405. NEW5: 0.321. EDGE: 0.376
 1000000 0100000 1100000 0010000 1010000 0110000 1110000 0001000 1001000 0101000 1101000 0011000 1011000 0111000 1111000 0000100 1000100 0100100 1100100 0010100 1010100 0110100 1110100 0001100 1001100 0101100 1101100 0011100 1011100 0111100 1111100 0000010 1000010 0100010 1100010 0010010 1010010 0110010 1110010 0001010 1001010 0101010 1101010 0011010 1011010 0111010 1111010 0000110 1000110 0100110 1100110 0010110 1010110 0110110 1110110 0001110 1001110 0101110 1101110 0011110 1011110 0111110 1111110
 0.86806 1.12526 0.88712 1.11734 0.77540 0.96524 0.81097 0.37634 0.32299 0.32702 0.32666 0.35866 0.32660 0.33674 0.33925 0.40529 0.38324 0.38814 0.41603 0.43503 0.41175 0.43441 0.44224 0.33780 0.30610 0.30867 0.30084 0.33467 0.31124 0.31672 0.31145 0.32128 0.34072 0.33634 0.38026 0.36880 0.37479 0.38673 0.40959 0.28908 0.27443 0.27254 0.27626 0.29530 0.28509 0.28675 0.29058 0.32992 0.32040 0.32051 0.32987 0.34754 0.33892 0.34463 0.35011 0.30108 0.28597 0.28631 0.28241 0.30505 0.29294 0.29507 0.29217
865,311,-89,170,-277,46,150,-179,-96,105,355,76,49,
*/