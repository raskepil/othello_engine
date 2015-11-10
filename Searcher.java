public class Searcher {
  public Searcher(Board b) {
    this(b,new Evaluator());
  }
  public Searcher(Board b, Board.Eval e) {
    m_board = b;
    m_eval  = e;
  }
  public boolean doPlay(byte col, Board.Pos p) {
    p = p == null ? play() : p;
    return p != null && m_board.dolegal(p, col);
  }

  private int alphaBetaSearch(int alpha, int beta, int ply, Board.Node node) {
    int num = -9999;
    Board.Pos[] fut   = m_future;
    m_future          = m_future.clone();
    Board.Moves  myit = node.moves();
    Board.Node   iter;
    while (null != (iter = myit.next())) {
      num = ply == 0 ? iter.score() :
                       alphaBetaSearch(-beta, -alpha, ply - 1, iter);
      if (alpha < num) {
        if (beta <= num) {
          m_future = fut;
          return -num;
        }
        alpha = num;
        m_future[ply] = myit.createPos();
      }
    }
    if (num == -9999) {
      iter            = myit.forfeit();
      boolean more    = iter.moves().next() != null;
      num =  !more    ? iter.finalScore() :
                        alphaBetaSearch(-beta, -alpha, ply, iter);
      if (alpha < num) {
        if (beta <= num) {
          m_future = fut;
          return -num;
        }
        alpha = num;
      }
    }
    return -alpha;
  }
  private int minMaxSearch(int ply, Board.Node node) {
    int alpha = -9999;
    Board.Moves  myit = node.moves();
    Board.Node   iter;
    if (ply == 0) {
      while (null != (iter = myit.next())) {
        int num = iter.score();
        if (alpha < num) {
          alpha = num;
          m_future[0] = myit.createPos();
        }
      }
    }
    else {
      Board.Pos[] fut        = m_future.clone();
      while (null != (iter = myit.next())) {
        int num = minMaxSearch(ply - 1, iter);
        if (alpha < num) {
          alpha = num;
          for (int j = 0; j < ply; j++)
            fut[j] = m_future[j];
          fut[ply] = myit.createPos();
        }
      }
      m_future = fut;
    }
    if (alpha == -9999) {
      m_future[ply] = null;
      iter            = myit.forfeit();
      boolean more    = iter.moves().next() != null;
      alpha =!more    ? iter.finalScore() :
                        minMaxSearch(ply, iter);
    }
    return -alpha;
  }
  private int negascout(int alpha, int beta, int ply, Board.Node node) {
    Board.Moves myit = node.moves();
    Board.Node  iter;
    if (null != (iter = myit.next())) {
      if (ply == 0) {
        do {
          int num = iter.score();
          if (alpha < num) {
            alpha = num;
            if (beta <= alpha)
              break;
            m_future[0] = myit.createPos();
          }
        } while (null != (iter = myit.next()));
      }
      else {
        int b = beta;
        do {
          int num = negascout(-b, -alpha, ply - 1, iter);
          if (alpha < num && num < beta && b != beta)
            num = negascout(-beta, -alpha, ply - 1, iter);
          if (alpha < num) {
            alpha = num;
            if (beta <= alpha)
              break;
            m_future[ply] = myit.createPos();
          }
          b = alpha + 1;
        } while (null != (iter = myit.next()));
      }
    }
    else {
      iter             = myit.forfeit();
      boolean more     = iter.moves().next() != null;
      int num =  !more ? iter.finalScore() :
                         negascout(-beta, -alpha, ply, iter);
      if (alpha < num)
        alpha = num;
    }
    return -alpha;
  }
  static int negascoutq(int alpha, int beta, int ply, Board.Node node) {
    Board.Moves myit = node.moves();
    Board.Node  iter = myit.next();
    if (null != iter) {
      if (ply == 0) {
        do {
          int num = iter.score();
          if (alpha < num) {
            alpha = num;
            if (beta <= alpha)
              break;
          }
        } while (null != (iter = myit.next()));
      }
      else {
        int b = beta;
        do {
          int num = negascoutq(-b, -alpha, ply - 1, iter);
          if (alpha < num && num < beta && b != beta)
            num = negascoutq(-beta, -num, ply - 1, iter); //< -num due to no future.
          if (alpha < num) {
            alpha = num;
            if (beta <= alpha)
              break;
          }
          b = alpha + 1;
        } while (null != (iter = myit.next()));
      }
    }
    else {
      iter            = myit.forfeit();
      boolean    more = iter.moves().next() != null;
      int num = !more ? iter.finalScore() :
                        negascoutq(-beta, -alpha, ply, iter);
      if (alpha < num)
        alpha = num;
    }
    return -alpha;
  }
  //***************************************************************************************
  static int PE_NODES = 0;
  static final class PosBoard {
    int compare(PosBoard o) {
      return m_node.compare(o.m_node);
    }
    PosBoard(Board.Node n) {m_node = n.clone();}
    public boolean equals(Object a) {
      PosBoard aa = (PosBoard)a;
      return (m_child == aa.m_child || m_child.equals(aa.m_child))
          && (m_sib == aa.m_sib || m_sib.equals(aa.m_sib));
    }
    void store(PosBoard pb) {
      if (m_child == null || m_child.m_value < pb.m_value) {
        pb.m_sib = m_child;
        m_child = pb;
      }
      else {
        PosBoard p = m_child;
        for (; p.m_sib != null && pb.m_value < p.m_sib.m_value;)
          p = p.m_sib;
        pb.m_sib = p.m_sib;
        p.m_sib  = pb;
      }
    }
    Board.Pos[] wrapUp(boolean random) {
      int r = 1;
      for (PosBoard p = m_child; (p = p.m_sib) != null;)
        r++;
      PosBoard c = m_child;
      if (random && r > 1)
        for (r = Board.rand.nextInt(r); --r >= 0;)
          c = c.m_sib;
      int n = 0;
      for (PosBoard p = c; p != null; p = p.m_child)
        n++;
      Board.Pos[] ret = new Board.Pos[n];
      for (PosBoard pp = this; c != null; pp = c, c = c.m_child) {
        Board.Moves mv = pp.m_node.moves();
        Board.Node nx;
        while ((nx = mv.next()) != null && !nx.equals(c.m_node))
          ;
        ret[--n] = nx == null ? null : mv.createPos();
      }
      return ret;
    }
    static PosBoard preeval(int ply, Board.Node node) {
      PosBoard ret = new PosBoard(node);
      if (ply < 0) {
        ret.m_value = node.score();
      }
      else {
        int alpha = -9999;
        Board.Moves myit = node.moves();
        Board.Node  iter;
        if (null != (iter = myit.next())) {
          do {
            PE_NODES++;
            PosBoard pb = preeval(ply - 1, iter);
            ret.store(pb);
            if (alpha < pb.m_value)
              alpha = pb.m_value;
          } while (null != (iter = myit.next()));
        }
        else {
          PE_NODES++;
          iter            = myit.forfeit();
          boolean  more   = iter.moves().next() != null;
          if (!more) {
            ret.m_child   = new PosBoard(iter);
            alpha         = iter.finalScore();
          }
          else {
            ret.m_child = preeval(ply, iter);
            alpha = ret.m_child.m_value;
          }
        }
        ret.m_value = -alpha;
      }
      return ret;
    }
    int eval(int alpha, int beta, int ply, Board.Node node, int random) {
      if (m_child == null) {
        m_node.evalFrom(node);
        return m_value = negascoutq(alpha, beta, ply, m_node);
      }
      PosBoard   p = m_child;
      if (m_node.moves().next() != null) {
        int b                  = beta;
        do {
          PosBoard   next = p.m_sib;
          int num = p.eval(-b, -alpha, ply - 1, node, 0);
          if (alpha < num && num < beta && b != beta)
            num = p.eval(-beta, -alpha, ply - 1, node, 0);
          if (alpha < num) {
            if (beta <= num)
              return m_value = -beta;
            p.m_sib = null;
            if (alpha == num-random)
              p.m_sib = m_child;
            m_child = p;
            alpha = num - random;
          }
          b = alpha + 1;
          p = next;
        } while (p != null);
      }
      else {
        int num = p.eval(-beta, -alpha, ply, node, 0);
        if (alpha < num)
          alpha = num - random;
      }
      return m_value = -alpha - random;
    }
    Board.Node m_node;
    PosBoard   m_sib;
    PosBoard   m_child;
    int        m_value;
  }
  public int combined_search(byte col, int ply, boolean random) {
    return combined_search_fixedPly(col, Math.max(1,Math.min(5,ply/2)), ply, random);
    // + -> col wins .... - col lose. note -col is in the move!
  }
  public int combined_search_fixedPly(byte col, int ply0, int ply1, boolean random) {
    if (ply1 < 3) //< todo...
      return negascout(ply1,col);
    PosBoard pb = PosBoard.preeval(ply0-1, m_board.node(col).setEval(new Evaluator(), ply0));
    int r = pb.eval(-9999, 9999, ply1-1, m_board.node(col).setEval(m_eval, ply1), random ? 1 : 0);
    m_future = ply1 == 0 ? new Board.Pos[0] : pb.wrapUp(random && ply1 >= 2);
    return r;
    // + -> col wins .... - col lose. note -col is in the move!
  }
  // quality 0-2
  public int android_search(byte col, int quality) {
    int ply0 = quality == 0 ? 1 : 3;
    PE_NODES = 0;
    PosBoard pb = PosBoard.preeval(ply0-1, m_board.node(col).setEval(s_level1eval, ply0));
    int r = pb.m_value;
    double per = Math.log(PE_NODES) / ply0;
    int bc = m_board.count();
    int ply = (int)Math.round(Math.log(150000)/(per*0.3 + Math.max(0,0.82-Math.max(0,0.008*(bc-22)))));
    if (ply + bc >= 63)
      ply = 65 - bc;
    if (quality == 2 && pb.m_child.m_sib != null) {
      Board.Pos p = BoardDb.precalc(m_board,col);
      if (p != null) {
        //pb.m_child.m_pos = p;
        pb.m_child.m_sib = null;
      }
      else
        r = pb.eval(-9999, 9999, Math.max(ply - 1, ply0), m_board.node(col).setEval(m_eval, ply), 1);
    }
    else {
      PosBoard c = pb.m_child;
      for (; c.m_sib != null && c.m_value == c.m_sib.m_value; c = c.m_sib)
        ;
      c.m_sib = null;
    }
    m_future = pb.wrapUp(true);
    return r;
  }
  public int negascout(int ply, byte col) {
    m_future = new Board.Pos[ply];
    Board.Node it = m_board.node(col).setEval(m_eval, ply);
    int r = negascout(-9999,9999,ply-1,it);
    extend(it);
    return r;
  }
  public int negascout(int ply, byte col, int eval) {
    m_future = new Board.Pos[ply];
    Board.Node it = m_board.node(col).setEval(m_eval, ply);
    int r = negascout(-eval-1,-eval+1,ply-1,it);
    extend(it);
    return r;
  }
  public int alphaBetaSearch(int ply, byte col) {
    m_future = new Board.Pos[ply];
    Board.Node it = m_board.node(col).setEval(m_eval,ply);
    int r = alphaBetaSearch(-9999,9999,ply-1, it);
    extend(it);
    return r;
  }
  public int minMaxSearch(int ply, byte col) {
    m_future = new Board.Pos[ply];
    Board.Node it = m_board.node(col).setEval(m_eval, ply);
    int t = minMaxSearch(ply - 1, it);
    extend(it);
    return t;
  }
  public int adjustedPly(int ply) {
    int unknown_moves = 64 - m_board.count() - ply;
    if (unknown_moves <= (OthelloView.demoMode ? 1 : 4))
      ply += unknown_moves;
    return ply;
  }
  private Board.Node extend(Board.Node node) {
    Board.Pos[] src = m_future.clone();
    for (int d = m_future.length, s = d-1; --d >= 0;) {
      Board.Moves  it = node.moves();
      Board.Node   iter;
      if (src[s] != null && (iter = it.doLegal(src[s])) != null) {
        node        = iter;
        m_future[d] = src[s--];
      }
      else {
        s -= src[s] == null ? 1 : 0;
        node = it.forfeit();
        m_future[d] = null;
      }
    }
    return node;
  }
  Board.Pos[] deepPlayArray(int ply, byte col, int eval) {
    Board.Node node = m_board.node(col).setEval(m_eval,ply);
    int extra = 0;
    node = extend(node);
    for (int t = m_future.length; --t >= 0;) {
      extra += m_future[t] == null ? 1 : 0;
      eval = -eval;
    }
    Board.Pos[] fut = m_future;
    Board.Pos[] ret = new Board.Pos[ply];
    ply -= fut.length;
    if (ply+extra > 0) {
      m_future = new Board.Pos[ply+extra];
      int r = negascout(-eval - 1, -eval + 1, ply + extra - 1, node);
      if (r != eval)
        throw new RuntimeException("Illegal future");
      extend(node);
    }
    for (int j = 0; j < fut.length; j++)
      ret[j+ply] = fut[j];
    for (int j = 0; j < ply; j++)
      ret[j] = m_future[j+extra];
    m_future = fut;
    return ret;
  }

  void setEval(Board.Eval s)    { m_eval = s; }
  Board.Pos   play()            { return m_future.length == 0 ? null : m_future[m_future.length-1]; }
  Board.Pos[] playArray()       { return m_future; }

  private Board         m_board;
  private Board.Pos[]   m_future;
  private Board.Eval    m_eval;
  private static Board.Eval s_level1eval = new SuperEvalFast();
}
