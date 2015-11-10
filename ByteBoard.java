import java.util.Arrays;

public final class ByteBoard extends Board {
    public ByteBoard(final byte first) {
        m_board          = new byte[_91];
        m_board[13+9*3]  =        first;
        m_board[13+9*4]  = (byte)-first;
        m_board[14+9*3]  = (byte)-first;
        m_board[14+9*4]  =        first;
    }
    static ByteBoard New() { return new ByteBoard(e_white); }
    static class NormalEval implements Eval {
      NormalEval(Eval e, int rem) {
        EvalBoardOld ev = (EvalBoardOld)e;
        m_evaluboard = new byte[_91];
        for (int x = 0; x < _91; x++)
          m_evaluboard[x] = 127;
        for (Board.Pos p = new Board.Pos(); p.Next(); )
          m_evaluboard[p.Int()] = ev.evalAt(p,rem);
      }
      public int score(Node b) {
        byte[] board = ((bMoveIterator)b).m_board.m_board;
        int t = 0;
        for (int x = 10; x < 81; x++)
          t += board[x] * m_evaluboard[x];
        return t;
      }
      protected byte m_evaluboard[];
    }
    static final Eval s_end_eval = new Eval() {
      public int score(Node b) {
        byte[] board = ((bMoveIterator)b).m_board.m_board;
        int t = 0;
        for (int x = 10; x < 81; x++)
          t += board[x];
        return toFinal(t);
      }
    };
    static final Eval s_end_eval1 = new Eval() {
      public int score(Node it) {
        Board.Moves  myit = it.moves();
        int ret;
        if (null != (it = myit.next()))
          ret = it.finalScore();
        else {
          it         = myit.forfeit();
          Node it2   = it.moves().next();
          ret = it2 != null ? -it2.finalScore() : it.finalScore();
        }
        return ((bMoveIterator)it).m_col*ret;
      }
    };
    public ByteBoard(ByteBoard r) {
      m_board      = r.m_board.clone();
    }
    public ByteBoard(Board b) {
      m_board      = new byte[_91];
      fromStore(b.toStore());
    }
    public long[] toStore() {
      long wht = 0;
      long blk = 0;
      for (Pos p = new Pos(); p.Next();) {
        byte c = get(p);
        long f = 1L << p.bitpos();
        wht |= c == e_white ? f : 0;
        blk |= c == e_black ? f : 0;
      }
      return new long[] {wht,blk};
    }
    public void fromStore(long[] arr) {
      for (Pos p = new Pos(); p.Next();)
        m_board[p.Int()] = (byte)((arr[0] >>> p.bitpos() & 1)*e_white +
                                  (arr[1] >>> p.bitpos() & 1)*e_black);
    }
    private boolean doExecLine(int pos, int n, byte c) {
        boolean ret = false;
        int n0 = pos-n;
        if (m_board[n0] == -c) {
            do n0-=n; while (m_board[n0] == -c);
            if (m_board[n0] == c) {
                do { n0 += n; m_board[n0] = c; } while (pos!=n0);
                ret = true;
            }
        }
        int n1 = pos+n;
        if (m_board[n1] == -c) {
            do n1+=n; while (m_board[n1] == -c);
            if (m_board[n1] == c) {
                do { n1 -= n; m_board[n1] = c; } while (pos!=n1);
                ret = true;
            }
        }
        return ret;
    }
    boolean dolegal(Pos ppos, byte c) {
        int pos = ppos.Int();
        return m_board[pos] == 0 && (
               doExecLine(pos,1,c) |
               doExecLine(pos,8,c) |
               doExecLine(pos,9,c) |
               doExecLine(pos,10,c));
    }
    boolean legal(Pos pos,byte c) {
      return new ByteBoard(this).dolegal(pos,c);
    }
    static private abstract class bNode {
      bNode(ByteBoard b, byte col) {
        m_board = b;
        m_col   = col;
        m_pos   = new Pos();
      }
      public boolean equals(Object o) {
        bNode b = (bNode)o;
        return m_col == b.m_col && Arrays.equals(m_board.m_board,b.m_board.m_board);
      }
      public Pos createPos()              { return m_pos.clone(); }
      protected byte      m_col;
      protected ByteBoard m_board;
      protected Pos       m_pos;
    }

    static private final class bMoveIterator extends bNode implements Moves, Node {
      bMoveIterator(ByteBoard b, byte c) {
        super(new ByteBoard(b),c);
        m_evaluboard = s_defEval;
        m_parent     = b;
      }
      public Moves moves()   {
        bMoveIterator r = new bMoveIterator(m_board, (byte)-m_col);
        r.m_evaluboard = this.m_evaluboard;
        return r;
      }
      public Node forfeit() {
        bMoveIterator r = new bMoveIterator(m_board, m_col);
        r.m_evaluboard = this.m_evaluboard;
        return r;
      }
      public Node setEval(Eval e, int ply) {
        int rem = 64-m_board.count()-ply;
        if (rem == 1)
          m_evaluboard = s_end_eval1;
        else if (rem == 0)
          m_evaluboard = s_end_eval;
        else if (e instanceof EvalBoardOld)
          m_evaluboard = new NormalEval(e,rem);
        else
          m_evaluboard = e != null ? e : s_defEval;
        return this;
      }
      public void evalFrom(Node node) {
        m_evaluboard = ((bMoveIterator)node).m_evaluboard;
      }
      public Node clone() {
        bMoveIterator r = new bMoveIterator(m_parent,m_col);
        r.m_board = new ByteBoard(m_board); //< performance?
        r.m_evaluboard = m_evaluboard;
        return r;
      }
      public int compare(Node r) {
        if (m_col != ((bMoveIterator)r).m_col)
          return m_col < ((bMoveIterator)r).m_col ? -1 : 1;
        byte[] board = ((bMoveIterator)r).m_board.m_board;
        for (int x = 10; x < 81; x++)
          if (board[x] != m_board.m_board[x])
            return board[x] < m_board.m_board[x] ? -1 : 1;
        return 0;
      }
      public Node next() {
        System.arraycopy(m_parent.m_board, 0, m_board.m_board, 0, _91); //< performance?
        while (m_pos.Next()) {
          if (m_board.dolegal(m_pos, m_col))
            return this;
        }
        return null;
      }
      public int finalScore() {
        int num = 0;
        for (Pos pos = new Pos(); pos.Next();)
          num += m_board.get(pos);
        return toFinal(m_col * num);
      }
      public int score() {
        return m_col*m_evaluboard.score(this);
      }
      public Node doLegal(Pos p) {
        m_board = new ByteBoard(m_parent);
        return m_board.dolegal(p,m_col) ? this : null;
      }
      protected ByteBoard m_parent;
      private   Eval      m_evaluboard;
    }
    Node node(byte col) {
      return new bMoveIterator(this,col);
    }

    byte get(Pos n) { return m_board[n.Int()]; }
    void debugSet(Pos n, byte c) { m_board[n.Int()] = c; }
    Board  played(Pos p, byte col) { ByteBoard b = new ByteBoard(this); return p.ok() && b.dolegal(p,col) ? b : this; }
    int evaluate()                 { return  node(Board.e_white)  .score(); }

    private      byte m_board[];
    final static int _91 = 92;
    final static private Eval s_defEval = new NormalEval(new DummyEval(),1);
}
