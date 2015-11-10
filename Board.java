
import java.util.Random;

abstract class Board {
  static final class Pos {
    private static final byte edgeboard[] = {
        0,1,1,1,1,1,1,0,
        9,0,0,0,0,0,0,9,
        9,0,0,0,0,0,0,9,
        9,0,0,0,0,0,0,9,
        9,0,0,0,0,0,0,9,
        9,0,0,0,0,0,0,9,
        9,0,0,0,0,0,0,9,
        0,1,1,1,1,1,1,0 };
    Pos() { n_ = (int)-1;}
    Pos(int x, int y) { n_ = (int)(8*y+x); }
           Pos(int n) { n_ = n; }
    public boolean equals(Object p) { return p != null && ((Pos)p).n_ == n_; }
    boolean        Next()  { return ++n_ < 64;}
    public  Pos    clone() { return new Pos(n_);}
    public boolean ok()    { return (n_&~63) == 0; }
    Pos     map(int j)     {
      if (!ok()) //< elegant not.
        return clone();
      int t  = (++j>>2&1) * 3;
      int n  = (-(j   &1) ^ (n_<<t)) & 56;
          n += (-(j>>1&1) ^ (n_>>t)) & 7;
      return new Pos(n);
    }
    int     bitpos()       { return n_; }
    int     Int()          { return n_/8+n_+10;}
    int     x()            { return n_%8;}
    int     y()            { return n_/8;}
    int     edge()         { return edgeboard[n_];}
    void    reset()        { n_ = -1; }
    boolean nextBit(long m){
      // average 6 bits each call
      //if (++n_ >= 64 || (m = m >>> n_) == 0)
      //  return false;
      n_++;
      n_ += Long.numberOfTrailingZeros(m>>>n_);
      return n_ < 64;
    }
    private int n_;
  }

  static int n0=0,n1=0,n2=0,n3=0,ndiff=0;
  static Random rand = new Random();
  static final int toFinal(int score) {
    return score*100;//Integer.signum(score)*9000 + score;
  }
  Board() {}
  public int count() {
    int c = 0;
    for (Pos p = new Pos(); p.Next();)
      c += get(p) != 0 ? 1 : 0;
    return c;
  }

  interface Eval {
    int     score(Node n);
  }
  interface Node {
    Moves   moves();
    Node    setEval(Eval eval, int ply);
    void    evalFrom(Node node);
    Node    clone();
    int     compare(Node r);
    int     score();
    int     finalScore();
  }
  interface Moves {
    Node    next();
    Node    doLegal(Pos pos);
    Node    forfeit();
    Pos     createPos();
  }
  abstract byte        get(Pos p);
  abstract boolean     dolegal(Pos p, byte col);
  abstract Board       played (Pos p, byte col);
  abstract boolean     legal  (Pos p, byte col);
  abstract Node        node(byte col);
  abstract long[]      toStore();
  abstract void        fromStore(long[] arr);

  static final byte e_black = -1;
  static final byte e_white = -e_black;
  static final byte e_next  = 0;
}
