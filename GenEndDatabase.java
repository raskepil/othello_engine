public class GenEndDatabase {
  
  final static int THREAD  = 4;
  final static int SEC_PRT = 20;
  final static int PRT_SAV = 5;

  static Board.Eval m_eval;

  static class mThread implements Runnable {
/*    class Eval2Step implements Board.Eval {
      public int score(Board.Node n) {
        BitBoard board = new BitBoard(n);
        Integer i = m_cache.get(board);
        if (i != null)
          return i;
        if (board.count() != DEPTH1) 
          throw new RuntimeException("Depth-error");
        int depth = 64 - DEPTH1;
        int ply0  = Math.min(7, depth/2-2); //GC mem limit
        long time = System.nanoTime();
        Searcher.PosBoard pb = Searcher.PosBoard.preeval(ply0-1, board.node(Board.e_next).setEval(m_eval, ply0));
        int value = pb.eval(-9999, 9999, depth-2, board.node(Board.e_next).setEval(null,depth-1), 0);
        time = System.nanoTime() - time;
        if (value % 100 != 0)
          throw new RuntimeException("100");
        m_cache.put(board,value);
        db1.Store(n, value);
        int score = m_eval.score(n);
        synchronized (_OLD) {
          _OLD.add(score,value);
          _REF.add(value,value);
          _TIM.add((int)(time/1000), 0);
        }
        return value;
      }
      private TreeMap<BitBoard,Integer> m_cache = new TreeMap<BitBoard,Integer>(new Comparator<BitBoard>() {
        public int compare(BitBoard a, BitBoard b) {
          return a.compare(b);
        }
      });
    };*/
    mThread(int d1) {
      DEPTH1 = d1;
      MyDb db = new MyDb(String.valueOf(DEPTH1)).read();
      db1 = new MyDb(String.valueOf(DEPTH1)+"a").read();
      db1.test(0,0);
      db .test(0,0);
      int sz = db.size();
      for (BoardDb.Iterator entry = db1.new Iterator(); entry.next();)
        db.insert(entry.raw(),entry.raw_value());
      System.out.printf("Bases concat %d + %d -> dropped %d\n", sz, db1.size(), sz+db1.size()-db.size());
      db.write();
      db1.clear();
      db1.write();
    }
    static int scoreToShort(int score, int lowest) {
      if (score == 0) return 15;
      score += 10000;
      if (score > lowest+5000) return 14;
      if (score > lowest+4400) return 13;
      if (score > lowest+3800) return 12;
      if (score > lowest+3400) return 11;
      if (score > lowest+3000) return 10;
      if (score > lowest+2600) return 9;
      if (score > lowest+2200) return 8;
      if (score > lowest+1800) return 7;
      if (score > lowest+1400) return 6;
      if (score > lowest+1000) return 5;
      if (score > lowest+ 600) return 4;
      if (score > lowest+ 400) return 3;
      if (score > lowest+ 200) return 2;
      if (score > lowest     ) return 1;
      if (lowest != score)
        throw new RuntimeException("lowest");
      return 0;
    }
    static byte b_arr[] = {
         0, 1, 1, 1, 2, 2, 2, 3,
         4, 0,11,-1,-1,12, 3,15,
         4,11,-1,-1,-1,-1,12,15,
         4,-1,-1,-1,-1,-1,-1,15,
         5,-1,-1,-1,-1,-1,-1, 6,
         5,14,-1,-1,-1,-1,13, 6,
         5, 7,14,-1,-1,13, 8, 6,
         7, 9, 9, 9,10,10,10, 8
    };
    long rawEval(Board.Node node) {
      BitBoard node_bb = new BitBoard(node); 
      if (node_bb.count() != DEPTH1) 
        throw new RuntimeException("Depth-error");
      int t = new BitBoard(((BitBoard.bMoveIterator)node).m_top.m_top).normalize(Board.e_next);
      for (int k = 0; k <= t; k++)
        node_bb.map(k);
      node = node_bb.node(Board.e_next);
      int depth = 64 - DEPTH1;
      long alpha = 9999;
      Board.Moves  myit = node.moves();
      Board.Node   iter;
      if (DEPTH1 != 48 || null == (iter = myit.next())) {
        int ply0  = Math.min(6, depth/2-2); //GC mem limit
        Searcher.PosBoard pb = Searcher.PosBoard.preeval(ply0-1, node.setEval(m_eval, ply0));
        alpha = pb.eval(-9999, 9999, depth-2, node.setEval(null,depth-1), 0);
      }
      else {
        depth--;
        int ply0  = Math.min(6, depth/2-2); //GC mem limit
        int[] arr = new int[16];
        do {
          Searcher.PosBoard pb = Searcher.PosBoard.preeval(ply0-1, iter.setEval(m_eval, ply0));
          int num = -pb.eval(-9999, 9999, depth-2, iter.setEval(null,depth-1), 0);
          int ba = b_arr[myit.createPos().bitpos()];
          if (ba != -1)
            arr[ba] = Math.min(arr[ba], num-10000);
          alpha = Math.min(num, alpha);
        } while (null != (iter = myit.next()));
        long s = 1L << Short.SIZE;
        alpha &= s-1;
        alpha |= (s << 4*0) * scoreToShort(Math.min(arr[0],arr[1]),(short)alpha);
        alpha |= (s << 4*1) * scoreToShort(Math.min(arr[2],arr[3]),(short)alpha);
        alpha |= (s << 4*2) * scoreToShort(Math.min(arr[3],arr[15]),(short)alpha);
        alpha |= (s << 4*3) * scoreToShort(Math.min(arr[6],arr[8]),(short)alpha);
        alpha |= (s << 4*4) * scoreToShort(Math.min(arr[8],arr[10]),(short)alpha);
        alpha |= (s << 4*5) * scoreToShort(Math.min(arr[9],arr[7]),(short)alpha);
        alpha |= (s << 4*6) * scoreToShort(Math.min(arr[7],arr[5]),(short)alpha);
        alpha |= (s << 4*7) * scoreToShort(Math.min(arr[0],arr[4]),(short)alpha);
        alpha |= (s << 4*8) * scoreToShort(arr[11],(short)alpha);
        alpha |= (s << 4*9) * scoreToShort(arr[12],(short)alpha);
        alpha |= (s << 4*10)* scoreToShort(arr[13],(short)alpha);
        alpha |= (s << 4*11)* scoreToShort(arr[14],(short)alpha);
      }
      return alpha;
    }
    void Game() {
      Board.Node node = BoardUtil.genRandomBoards(DEPTH1);
      long time = System.nanoTime();
      long value = rawEval(node);
      time = System.nanoTime() - time;
      if ((short)value % 100 != 0)
        throw new RuntimeException("100");
      db1.Store(node, value);
      int score = m_eval.score(node);
      synchronized (_OLD) {
        _OLD.add(score,(short)value);
        _REF.add((short)value,(short)value);
        _TIM.add(time/1000, 0);
      }
    }

    public void run() {
      while (running) {
        Game();
      }
    }
    boolean running = true;
    final MeanDev _OLD = new MeanDev();
    final MeanDev _REF = new MeanDev();
    final MeanDev _TIM = new MeanDev();
    private MyDb  db1;
    private int   DEPTH1;
  }
  public static void main(String[] args) {
    if (args.length != 1)
      System.out.println("Depth");
    int d0 = Integer.parseInt(args[0]);
    if (d0 < 30 || d0 > 60)
      System.out.println("Range: 30-d0-d1-60");
    else {
      m_eval = new SuperEvalFast();
      final mThread ptread = new mThread(d0);
      System.out.printf("Eval: %s.\n", m_eval);
      Thread thread[] = new Thread[THREAD];
      for (int k = 1; k < THREAD; k++)
        (thread[k] = new Thread(ptread)).start();
      long dispMs = System.currentTimeMillis();
      while (true) {
        long prtMs = dispMs;
        long time;
        do {
          while ((time = System.currentTimeMillis()) < dispMs + 1000*SEC_PRT)
            ptread.Game();
          dispMs = time;
          System.out.printf("@Depth %s.  \t", ptread.db1);
          synchronized (ptread._OLD) {
            System.out.printf("Qeval: %.3f. Time: %.1fms(%.1f)\n", ptread._OLD.quality(ptread._REF), ptread._TIM.mean()/1000,ptread._TIM.stddev()/1000);
          }
        } while (time < prtMs + 1000*SEC_PRT*PRT_SAV);
        System.out.print(".s.t.o.r.e......");
        ptread.db1.write();
        System.out.printf("done\n");
       }
    }
  }
}

class MyDb extends BoardDb {
  MyDb(String moves) { m_moves = Integer.parseInt(moves.substring(0,2)); m_name = moves; }
  MyDb read()  { readAll(toFile()); return this; }
  void write() { synchronized(this) { writeAll(toFile()); } }
  Reader reader() { return new Reader(toFile()); }
  public  String toString() { return ""+m_moves+":  "+size()+":+"+(m_mean.m_ct)+"(-"+m_lost+")("+(int)m_mean.mean()+"/"+(int)m_mean.stddev()+")";}
  private String toFile()   { return "rawboards/"+m_name+"_coll"; }
  void test(int ppt, int cnt) {
    BitBoard last = new BitBoard(Long.MIN_VALUE,Long.MIN_VALUE);
    long time = 0;
    int count = 0;
    for (Iterator entry = new Iterator(); entry.next();) {
      int comp = last.compare(entry.raw());
      last = entry.raw();
      if (comp != -1)
        throw new RuntimeException();
      if (Board.rand.nextInt(1000) < ppt || Board.rand.nextInt(size()) < cnt) {
        Board.Node it = fromCodedBoard(entry.raw());
        BitBoard board = new BitBoard(it);
        if (board.count() != m_moves)
          throw new RuntimeException();
        time -= System.nanoTime();
        //
        //int ply0  = Math.min(6, (64-board.count())/2-2); //GC mem limit
        //Searcher.PosBoard pb = Searcher.PosBoard.preeval(ply0-1, board.node(Board.e_next).setEval(GenEndDatabase.m_eval, ply0));
        //int val = pb.eval(-entry.value()-1,-entry.value()+1, 62-board.count(), board.node(Board.e_next).setEval(null,63-board.count()), 0);
        int ply0  = Math.min(6, (64-board.count())/2-2); //GC mem limit
        Searcher.PosBoard pb = Searcher.PosBoard.preeval(ply0-1, board.node(Board.e_next).setEval(GenEndDatabase.m_eval, ply0));
        int val = pb.eval(-entry.value()-1,-entry.value()+1, 62-board.count(), board.node(Board.e_next).setEval(null,63-board.count()), 0);
        if (val != entry.value())
          throw new RuntimeException();
        if (entry.raw_value() != entry.value()) {
          TrainEval tr = new TrainEval();
          tr.trainLongSlow(it, entry.raw_value(), true);
        }
        time += System.nanoTime();
        
        if (++count == 1)
          time = 0;
        double elapsed = time/(double)count;
        if (count%10 == 0)
          System.out.printf("%.0f ", elapsed*1e-6);
      }
    }
    System.out.printf("\n");
  }
  void Store(Board.Node node, long raw_result) {
    if (new BitBoard(node).count() != m_moves)
      throw new RuntimeException("Store.exp");
    BitBoard.bMoveIterator par = (BitBoard.bMoveIterator)((BitBoard.bMoveIterator)node).m_top;
    Store(new BitBoard(par.m_top), par.createPos(), ((BitBoard.bMoveIterator)node).createPos(), raw_result);
  }
  private void Store(BitBoard old_board, Board.Pos p0, Board.Pos p1, long raw_result) {
    BitBoard board = new BitBoard(old_board);
    int norm = board.normalize(Board.e_next);
    p0 = p0.map(norm);
    p1 = p1.map(norm);
    Board.Node resit = board.node(Board.e_next);
    if (p0.ok()) {
      resit = resit.moves().doLegal(p0);
      if (null == resit)
        throw new RuntimeException("Store.p0");
    }
    else
      resit = resit.moves().forfeit();
    if (p1.ok() && (resit = resit.moves().doLegal(p1)) == null)
      throw new RuntimeException("Store.p1");
    long[] store = board.toStore();
    long flag    = p0.ok() ? (1L << p0.bitpos()) : 0;
         flag   |= p1.ok() ? (1L << p1.bitpos()) : 0;
    Board.Node res2 = board.node(Board.e_next);
    if (p1.ok() && (res2 = res2.moves().doLegal(p1)) != null && p0.ok() && res2.moves().doLegal(p0) != null) {
      if (board.count() >= 60)
        throw new RuntimeException("62 filled store, not allowed");
      if (p1.bitpos() < p0.bitpos())
        flag = ~(store[0]|store[1]) & ~flag;
    }
    store[0] |= flag;
    store[1] |= flag;
    board.fromStore(store);
    BitBoard back = new BitBoard(fromCodedBoard(board));
    if (back.count() != m_moves || !back.equals(new BitBoard(resit)))
      throw new RuntimeException("Store.copyback");
    synchronized(this) {
      m_mean.add((short)raw_result, (short)raw_result);
      m_lost += insert(board,raw_result) != null ? 1 : 0;
    }
  }
  static Board.Node fromCodedBoard(BitBoard board_org) {
    long[] a = board_org.toStore();
    return fromCodedBoard(a[0],a[1]);
  }
  static Board.Node fromCodedBoard(long a0, long a1) {
    long b = a0 & a1;
    BitBoard board = new BitBoard(a0&~b,a1&~b);
    boolean fl = Long.bitCount(b) > 2;
    if (fl)
      b = ~(a0 | a1);
    int n0 = Long.numberOfTrailingZeros(b);
    int n1 = n0 + 1 + Long.numberOfTrailingZeros((b) >>> 1+n0);
    if (fl) {
      int e = n0;
      n0 = n1;
      n1 = e;
    }
    Board.Pos   p0 = new Board.Pos(n0%8,n0/8);
    Board.Pos   p1 = new Board.Pos(n1%8,n1/8);
    Board.Moves ma = board.node(Board.e_next).moves();
    Board.Node ret;
    if ((ret = ma.doLegal(p0)) != null && (!p1.ok() || (ret = ret.moves().doLegal(p1)) != null))
      return ret;
    if ((ret = ma.doLegal(p1)) != null && (ret = ret.moves().doLegal(p0)) != null)
      return ret;
    ret = ma.forfeit();
    if ((ret = ret.moves().doLegal(p0)) != null)
      return ret;
    throw new RuntimeException("oldstyle boards not loaded");
    //return (Board.MoveIterator)board.node(Board.e_next, 0,  null);
  }
  public void clear() {
    super.clear();
  }
  private MeanDev m_mean = new MeanDev();
  private int  m_lost  = 0;
  private int  m_moves;
  private String m_name;
}
