public class BoardUtil {
  static public int moveCount(Board b, byte col) {
    int ct = 0;
    Board.Moves n = b.node((byte)-col).moves();
    while (n.next() != null)
      ct++;
    return ct;
  }
  static public Board.Pos randomMove(Board b, byte col) {
    int ct = (int)(moveCount(b,col)*Board.rand.nextDouble());
    Board.Moves n = b.node((byte)-col).moves();
    while (n.next() != null)
      if (ct-- == 0)
        return n.createPos();
    return null;
  }
  static Board.Node genCrapBoards(int count) {
    BitBoard board  = new BitBoard(ByteBoard.New());
    byte     col    = Board.rand.nextBoolean() ? Board.e_black : Board.e_white;
    Board.Pos  lp   = null;
    Board.Node ret  = board.node((byte)-col);
    while (board.count() < count) {
      Board.Pos p = randomMove(board,col);
      if (p != null) {
        if (!board.dolegal(p, col))
          throw new RuntimeException("Not legal");
        ret = ret.moves().doLegal(p);
      }
      else if (lp == null)
        return genCrapBoards(count);
      else
        ret = ret.moves().forfeit();
      if (col==1 && !board.equals(new BitBoard(ret)))
        throw new RuntimeException("Not legal");
      lp  = p;
      col = (byte)-col;
    }
    return ret;
  }
  static Board.Node genRandomBoards(int count) {
    Board.Eval eval = new SuperEvalFast();
    BitBoard board  = new BitBoard(ByteBoard.New());
    byte     col    = Board.rand.nextBoolean() ? Board.e_black : Board.e_white;
    Board.Pos  lp   = null;
    Board.Node ret  = board.node((byte)-col);
    while (board.count() < count) {
      int ply = 11 + board.count() - count;
      double f = Board.rand.nextDouble();
      if (ply > 0) {
        //double FACT = 0.40 + -0.20*(count-48); //factor + yields + in avg.first.
        //int    fact = (int)Math.ceil((FACT*(1-2*(ply&1))-1)*f);
        //ply         = 8 - (fact>>16)*fact - Board.rand.nextInt(3);
        ply = 1 + (int)(10*(1 - f*f));
      }
      else
        ply = 1 + (int)(6*(1 - f*f));
      Board.Pos p;
      if (board.count() < 8 || ply == 0)
        p = randomMove(board,col);
      else {
        Searcher search = new Searcher(board);
        search.setEval(eval);
        int varl = search.combined_search((byte)-col, ply, true);
        if (board.count()+1 == count) {
          Board.Pos[] fut = search.deepPlayArray(ply, (byte)-col, varl);
          for (Board.Pos ff : fut)
            if (ff == null)
              return genRandomBoards(count);
        }
        p = search.play();
      }
      if (p != null) {
        if (!board.dolegal(p, col))
          throw new RuntimeException("Not legal");
        ret = ret.moves().doLegal(p);
      }
      else if (lp == null)
        return genRandomBoards(count);
      else
        ret = ret.moves().forfeit();
      if (col==1 && !board.equals(new BitBoard(ret)))
        throw new RuntimeException("Not legal");
      lp  = p;
      col = (byte)-col;
    }
    return ret;
  }
}
