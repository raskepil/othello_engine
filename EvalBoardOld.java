abstract class EvalBoardOld implements Board.Eval {
  abstract byte  evalAt(Board.Pos x, int rem);
  abstract int   weight(int x, int rem);
  abstract int   flag(int x);
  public int score(Board.Node b) {
    BitBoard.bMoveIterator bim = (BitBoard.bMoveIterator)b;
    BitBoard bb = new BitBoard(bim.m_cur,bim.m_emy);
    int rem = 64-bb.count();
    int ct = 0;
    for (Board.Pos p = new Board.Pos(); p.Next();)
      ct += bb.get(p) * evalAt(p,rem);
    return ct;
  }
}

class Evaluator extends EvalBoardOld {
  static final byte s_templboard[] = {
    105,
    -23, -62,
    5,  -15, 1,
    7,  -12, 0, 4
  };
  static final byte s_templboard_org[] = {
      99,
      10,-48,
      22, -2, 4,
      22, -2, 1, 0};
  static final int[] s_weights = {0,6,0,0,0};
  public Evaluator() { m_raw = s_templboard; m_wgt = s_weights; }
  public Evaluator(byte[] b, int[] w) { m_raw = b; m_wgt = w; }
  public int  flag(int x) {
    return m_wgt[x];
  }
  public int  weight(int x, int rem) {
    return scale(rem, 0, m_wgt[x]);
  }
  static int scale(int rem, int targ, int val) {
    int brek = 14;
    double fact;
    if (rem < brek)
      fact = 0.5 * rem / brek;
    else
      fact = 0.5 + 0.5 * (rem - brek) / (53.0 - brek);
    return (int)Math.round(val*fact + targ*(1-fact));
  }
  public byte evalAt(Board.Pos x, int rem) {
    int ev = m_raw[tri_idx(x)] - m_wgt[1];
    return (byte)scale(rem, rem == 0 ? 1 : 6, ev);
  }
  byte[] m_raw;
  int [] m_wgt;
  static int tri_idx(Board.Pos x) {
    int l=x.y(), r = x.x();
    if (4 <= r) r = 7 - r;
    if (4 <= l) l = 7 - l;
    if (l < r) { int t=r; r=l; l=t; }
    return r + l*(l+1)/2;
  }
}
final class DummyEval extends EvalBoardOld {
  public int   weight(int x, int n) { return (x*257 + 1009) % 73; }
  public int   flag(int x) { return (x*131 + 997) % 73; }
  public byte  evalAt(Board.Pos x, int n) {
    return (byte)(((3 - Math.abs(7 - 2*x.y()) / 2)*257 + x.x()*1009) % 43 - 21);
  }
}
