import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Random;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class Othellotest {

  static private ByteBoard[] m_randombyte = new ByteBoard[100000];
  static private BitBoard [] m_randombit  = new BitBoard [m_randombyte.length];

  @BeforeClass
  public static void setUpBeforeClass()  throws Exception {
    System.out.println(System.getProperty("java.version"));
    genRandomBoards();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    System.out.printf("hits: %d %d\n", Board.n0, Board.n1);
    System.out.printf("hits: %d %d\n", Board.n2, Board.n3);
    System.out.printf("hits: %d\n", Board.ndiff);
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  static private void genRandomBoards() {
    int remove = 0;
    int random = 1*(int)(Math.random()*1000000000);
    Board.rand = new Random(random);
    System.out.printf("Random: %d\n", random);
    int     retp = 0;
    byte    col  = 0;
    ByteBoard board = null;
    BoardDb db = new BoardDb();
    do {
      if (board == null || board.count() >= 63) {
        col  = (byte)(Board.rand.nextBoolean() ? -1 : 1);
        board = ByteBoard.New();
        for (int j = 0; j < 4; j++) {
          Board.Pos p = BoardUtil.randomMove(board,col);
          if (p != null)
            board.dolegal(p, col);
          col = (byte)(-col);
        }
      }
      int ply = Board.rand.nextInt(5);
      Board.Pos p;
      if (ply < 3)
        p = BoardUtil.randomMove(board,col);
      else {
        Searcher search = new Searcher(board);
        search.combined_search((byte)-col, ply, false);
        p = search.play();
      }
      boolean more = BoardUtil.moveCount(board,(byte)-col) > 0;
      if (p != null) {
        if (more) {
          BitBoard nb = new BitBoard(board);
          nb.normalize(col);
         // nb.normalize((byte)1);
          ByteBoard b = new ByteBoard(nb);
          if (db.insert(nb)) {
            m_randombyte[retp]  = b;
            m_randombit [retp++] = nb;
          }
          else
            remove++;
        }
        if (!board.dolegal(p, col))
          throw new RuntimeException("Not legal");
      }
      else if (!more)
        board = null;
      col = (byte)(-col);
    } while (retp < m_randombyte.length);
    System.out.printf("Removed %d\n", remove);
  }
  @Test
  public void myFirstTest() {
    Board bb = new ByteBoard(new BitBoard(0x4,0x1a));
    Searcher search = new Searcher(bb);
    search.setEval(ByteBoard.s_end_eval);
    assertEquals(-300,search.minMaxSearch(1, Board.e_black));
    assertEquals(search.play(), new Board.Pos(5,0));
    assertTrue(bb.legal(search.play(), Board.e_white));
    assertEquals(300,search.minMaxSearch(1, Board.e_white));
    assertEquals(search.play(), null);
    assertTrue(bb.dolegal(new Board.Pos(5,0), Board.e_white));

    bb = new BitBoard(0x4,0x1a);
    search = new Searcher(bb);
    search.setEval(BitBoard.s_end_eval);
    assertEquals(-300,search.minMaxSearch(1, Board.e_black));
    assertEquals(search.play(), new Board.Pos(5,0));
    assertTrue(bb.legal(search.play(), Board.e_white));
    assertEquals(300,search.minMaxSearch(1, Board.e_white));
    assertEquals(search.play(), null);
    assertTrue(bb.dolegal(new Board.Pos(5,0), Board.e_white));
  }
  @Test
  public void evalTest() {
    Board b1 = new BitBoard(0x4,0x1a);
    Board b2 = new ByteBoard(b1);
    int res =        Searcher.negascoutq(-9999, 9999, 1, b1.node(Board.e_black));
    assertEquals(res,Searcher.negascoutq(-9999, 9999, 1, b2.node(Board.e_black)));
    assertEquals(res,Searcher.negascoutq(-9999, 9999, 1, b1.node(Board.e_black).setEval(null, 1)));
    assertEquals(res,Searcher.negascoutq(-9999, 9999, 1, b2.node(Board.e_black).setEval(null, 1)));
    Searcher s1 = new Searcher(b1,null);
    Searcher s2 = new Searcher(b2,null);
    assertEquals(res,s1.negascout(2, Board.e_black));
    assertEquals(res,s2.negascout(2, Board.e_black));
    assertEquals(res,s1.combined_search(Board.e_black,2,false));
    assertEquals(res,s2.combined_search(Board.e_black,2,false));
  }

  @Test
  public void randomWorks() {
    BitBoard bit = new BitBoard(ByteBoard.New());
    Searcher src = new Searcher(bit);
    int      [] ct  = new int[8];
    Board.Pos[] pos = new Board.Pos[8];
    for (int q = 0; q < 200*8; q++)
      for (int depth = 3; depth < 7; depth++) {
        byte col = (byte) (Board.rand.nextBoolean() ? -1 : 1);
        int score = src.negascout(depth,col);
        assertEquals(score, src.combined_search(col,depth,true));
        Board.Pos p = src.play();
        for (int t = 0;; t++) {
          if (pos[t] == null)
            pos[t] = p;
          if (p.equals(pos[t])) {
            ct[t]++;
            break;
          }
        }
      }
    for (int q = 0; q < ct.length; q++)
      assertTrue(700 < ct[q] && ct[q] < 900);
  }

  private void futureSanity(ByteBoard board2, Board.Pos[] mp, int eval, byte col0) {
    byte c = col0;
    mp = mp.length > 0 ? mp : new Board.Pos[1];
    ByteBoard board = new ByteBoard(board2);
    int extrad = 0;
    for (int j = mp.length; --j >= 1; c = (byte)-c) {
      assertTrue(mp[j] == null || board.dolegal(mp[j],(byte)-c));
      extrad += mp[j] == null ? 1 : 0;
    }
    assertEquals(col0*c*eval, new Searcher(board,null).combined_search(c,extrad+1,true));
    extrad += mp[0] == null ? 1 : 0;
    int ev = board.node(c).setEval(null,0).score();
    if (mp[0]!=null) {
      ByteBoard bb2 = new ByteBoard(board);
      bb2.dolegal(mp[0], (byte)-c);
      ev = -bb2.node((byte)-c).setEval(null,0).score();
    }
    if (col0*c*eval != ev) {
      assertTrue(extrad > 0);
      ByteBoard bb2 = new ByteBoard(board);
      assertTrue(mp[0] == null || bb2.dolegal(mp[0],(byte)-c));
      int nval = -c*col0*new Searcher(bb2,null).negascout(extrad,(byte)-c);
      if (eval!=nval)
        new Searcher(bb2).negascout(extrad,(byte)-c);
      assertEquals(eval,nval);
    }
  }
  @Test
  public void searchEquals() {
   do {
     long random = (long) (Math.random() * 10000000000L);
     Board.rand = new Random(random);
     System.out.printf("SearchEquals: %d\n", random);
     for (int test = 0; test < 10; test++) {
       ByteBoard byr = ByteBoard.New();
       Searcher  by = new Searcher(byr,null);
       Searcher  bi = new Searcher(new BitBoard(byr),null);
       byte col = (byte) (Board.rand.nextBoolean() ? -1 : 1);
       int ply = 3 + Board.rand.nextInt(5);
       int hasm = 2;
       while (hasm > 0) {
         //int pl = by.pre_search(ply, null);
         //int re = col*by.combined_search(col, false);
         int re = by.combined_search(col,ply,false);
         //pl = bi.pre_search(ply, null);
         assertEquals(re, bi.combined_search(col,ply,false));
         assertArrayEquals(bi.playArray(),by.playArray());
         assertEquals(re, bi.combined_search(col,ply,true));
         futureSanity(byr, bi.deepPlayArray(ply,col,re), re, col);

         col = (byte) -col;
         Board.Pos p = BoardUtil.randomMove(byr, col);
         assertEquals(p == null, by.play() == null);
         hasm = p != null ? 2 : hasm - 1;
         if (p != null) {
           assertTrue(bi.doPlay(col,p));
           assertTrue(by.doPlay(col,p));
         }
       }
     }
     break;
   } while (true);
  }

/*
  interface Pred {
    boolean test2(int x, int y);
  }
  void koko(Pred h) {
    System.out.printf("Java8test: %b\n", h.test2(8,2));
  }
  @Test
  public void ert() {
    koko((x,y) -> {
      for (int j = 0; j < x; j++)
        y -= 0;
      return x+y == 10;
    });
  }
*/
  private void dosearchStable(int maxply, ByteBoard board, byte col) {
    ByteBoard byt = new ByteBoard(board);
    Searcher search = new Searcher(byt);
    int adjusted_ply = search.adjustedPly(maxply);
    int score = search.combined_search(col, adjusted_ply, false);
    assertTrue(search.play() != null);
    BitBoard bib = new BitBoard(byt);
    Searcher bsearch = new Searcher(bib);
    assertEquals(score, bsearch.combined_search(col, adjusted_ply, false));
    assertArrayEquals(search.playArray(), bsearch.playArray());
    Board.Pos[] future = null;
    for (int k = adjusted_ply; k > 0; k--) {
      int ply = bsearch.adjustedPly(maxply);
      assertEquals(score,bsearch.negascout(ply,col));
      if (future == null)
        future = bsearch.playArray().clone();
      assertArrayEquals(Arrays.copyOfRange(bsearch.playArray(),ply-k,ply), Arrays.copyOf(future, k));

      assertEquals(score, bsearch.alphaBetaSearch(ply,col));
      assertArrayEquals(Arrays.copyOfRange(bsearch.playArray(),ply-k,ply), Arrays.copyOf(future, k));

      if (ply <= 3) {
        assertEquals(score, bsearch.minMaxSearch(ply, col));
        assertArrayEquals(Arrays.copyOfRange(bsearch.playArray(), ply - k, ply), Arrays.copyOf(future, k));

        int p = bsearch.adjustedPly(ply);
        Board.Node iterator = bib.node(col).setEval(new Evaluator(),p);
        Searcher.PosBoard pb = Searcher.PosBoard.preeval(p-1, iterator);
        assertEquals(bsearch.minMaxSearch(ply, col), pb.m_value);
        Board.Pos[] par = pb.wrapUp(false);
        Board.Pos[] bs  = bsearch.playArray();
        int l = Math.min(par.length,bs.length);
        assertArrayEquals(Arrays.copyOfRange(par,par.length-l,par.length),
                          Arrays.copyOfRange(bs, bs.length-l,bs.length));
      }

      assertEquals(score, bsearch.combined_search(col, ply, true));

      assertEquals(score, bsearch.negascout(ply, col, score));
      assertArrayEquals(Arrays.copyOfRange(bsearch.playArray(),ply-k,ply), Arrays.copyOf(future, k));

      col = (byte)-col;
      if (bsearch.play() != null) {
        assertTrue(bsearch.doPlay(col,null));
        maxply--;
      }
      score = -score;
    }
  }
  @Test
  public void searchDepthStable() {
    for (int j = m_randombyte.length; --j >= 0;) {
      if (Board.rand.nextInt(m_randombyte.length) < 1000)
        dosearchStable(1 + Board.rand.nextInt(6),
            m_randombyte[j], (byte)(2*Board.rand.nextInt(2)-1));
    }
  }

  static int[] s_scores = {14,19,18,20,19,20,2,2,-25,2,2,0,-18,6,-8,-2,-14,24,7,8,34,20,13,
                           28,4,7,24,30,-11,-11,-1,-6,-27,16,19,5,1,46,22,69,58,65,66,69,
                           55,-1400,39,-1400,-1400,-1400,-1400,-1400};
  @Test
  public void safeSearch() {
    Board.rand = new Random(9876);
    byte col  = (byte)(Board.rand.nextBoolean() ? -1 : 1);
    ByteBoard board = ByteBoard.New();
    for (int j = 0; j < 5; j++) {
      Board.Pos p = BoardUtil.randomMove(board, col);
      if (p != null)
        board.dolegal(p, col);
      col = (byte)-col;
    }
    int na = 0;
    while (true) {
      Searcher s = new Searcher(board);
      int ply = s.adjustedPly(4+Board.rand.nextInt(4));
      int score = new Searcher(new BitBoard(board)).negascout(ply,(byte)-col);
      assertEquals(s_scores[na++], col*score);
      assertEquals(score, s.alphaBetaSearch(ply,(byte)-col));
      assertTrue(s.play() == null || board.legal(s.play(),col));
      assertEquals(score, s.combined_search((byte)-col, ply, false));

      ply = Board.rand.nextInt(7);
      Board.Pos p;
      if (ply < 3)
        p = BoardUtil.randomMove(board, col);
      else {
        Searcher search = new Searcher(board);
        search.combined_search((byte)-col, s.adjustedPly(ply), false);
        p = search.play();
      }
      if (p == null)
        break;
      assertTrue(board.dolegal(p, col));
      col = (byte)-col;
    }
  }
  static int playPly = 8;
  @Test
  public void playBenchmark() {
    int b4 = playBoardBenchmark4(new BitBoard(ByteBoard.New()), "VSlwBit");
    int bs = playBoardBenchmark2(new BitBoard(ByteBoard.New()), "VBit");
    int bu = playBoardBenchmark2(ByteBoard.New(), "VByt");
    int b5 = playBoardBenchmarkS(new Searcher(ByteBoard.New()), "FsByt", false);
    int b6 = playBoardBenchmarkS(new Searcher(new BitBoard(ByteBoard.New())), "FsBit", false);
             playBoardBenchmarkS(new Searcher(new BitBoard(ByteBoard.New())), "FsBitr", true);

    assertEquals(bs,bu);
    assertEquals(bs,b4);

    //assertEquals(bs,b5);
    assertEquals(b5,b6);
  }

  private int playBoardBenchmark4(Board board, String ss) {
    Searcher search = new Searcher(board);
    search.negascout(playPly,Board.e_black);
    boolean more;
    System.gc();
    long time = System.nanoTime();
    long tim2 = 0;
    int ret;
    do {
      ret = search.negascout(playPly,Board.e_black);
      Board.Pos p = search.play();
      tim2 -= System.nanoTime();
      int s2 = search.negascout(playPly,Board.e_black,ret);
      tim2 += System.nanoTime();
      assertEquals(s2,ret);
      assertEquals(p,search.play());
      more = p != null && search.doPlay(Board.e_white,null);

      ret = search.negascout(playPly,Board.e_white);
      p = search.play();
      tim2 -= System.nanoTime();
      s2 = search.negascout(playPly,Board.e_white,ret);
      tim2 += System.nanoTime();
      assertEquals(s2,ret);
      assertEquals(p,search.play());
      more |= p != null && search.doPlay(Board.e_black,null);
    } while (more);
    time = (System.nanoTime() - time - tim2)/1000000;
    System.out.printf("%s: %d ms + %d ms\n", ss, time, tim2/1000000);
    return ret;
  }
  private int playBoardBenchmark2(Board board, String ss) {
    Searcher search = new Searcher(board);
    search.negascout(playPly,Board.e_black);
    boolean more;
    System.gc();
    long time = System.nanoTime();
    int ret;
    do {
      int score = search.negascout(playPly, Board.e_black);
      more = search.doPlay(Board.e_white,null);

      ret = search.negascout(playPly, Board.e_white);
      more |= search.doPlay(Board.e_black,null);
    } while (more);
    time = (System.nanoTime() - time)/1000000;
    System.out.printf("%s: %d ms\n", ss, time);
    return ret;
  }
  private int playBoardBenchmarkS(Searcher search, String ss, boolean random) {
    search.negascout(playPly,Board.e_black);
    boolean more;
    System.gc();
    long time = System.nanoTime();
    int ret;
    do {
      ret = search.combined_search(Board.e_black, playPly, random);
      more = search.doPlay(Board.e_white,null);

      ret = search.combined_search(Board.e_white, playPly, random);
      more |= search.doPlay(Board.e_black,null);
    } while (more);
    time = (System.nanoTime() - time)/1000000;
    System.out.printf("%s: %d ms\n", ss, time);
    return ret;
  }

  @Test // bitboard and board returns same result
  public void bitBoardTest() throws Exception {
    for (int b = 0; b < m_randombyte.length; b++) {
      ByteBoard by = new ByteBoard(m_randombyte[b]);
      BitBoard  bi = m_randombit[b];
      assertEquals(by.evaluate(), bi.evaluate());
      for (Board.Pos p  = new Board.Pos(); p.Next();) {
        assertEquals(by.legal(p,Board.e_white), bi.legal(p,Board.e_white));
        assertEquals(by.legal(p,Board.e_black), bi.legal(p,Board.e_black));
        assertEquals(by.get(p), bi.get(p));
      }
      Searcher sy = new Searcher(by);
      Searcher si = new Searcher(bi);
      assertEquals(sy.minMaxSearch(1,Board.e_white), si.minMaxSearch(1,Board.e_white));
      assertEquals(sy.play(), si.play());
      assertEquals(sy.minMaxSearch(1,Board.e_black), si.minMaxSearch(1,Board.e_black));
      assertEquals(sy.play(), si.play());
    }
  }

  static Board.Eval m_debug_eval = new SuperEvalFast();
  @Test
  public void deepEvalTest() {
    Board.Eval eval = new SuperEvalSlow();
    for (BitBoard b : m_randombit) {
      Board.Moves par1 = b.node(Board.e_white).setEval(m_debug_eval,0).moves();
      Board.Moves par2 = b.node(Board.e_white).setEval(eval,0).moves();
      for (Board.Node it1, it2; null != (it1 = par1.next()) | null != (it2 = par2.next());) {
        assertEquals(it1.score(),it2.score());
      }
      par1 = b.node(Board.e_black).setEval(m_debug_eval,0).moves();
      par2 = b.node(Board.e_black).setEval(eval,0).moves();
      for (Board.Node it1, it2; null != (it1 = par1.next()) | null != (it2 = par2.next());) {
        assertEquals(it1.score(),it2.score());
      }
    }
  }
  @Test
  public void checkMoves2_3() {
    MeanDev md12 = new MeanDev();
    MeanDev md23 = new MeanDev();
    MeanDev ref  = new MeanDev();
    for (BitBoard b : m_randombit) {
      Board.Moves iter = b.node(Board.e_next).moves();
      for (Board.Node nd; (nd = iter.next()) != null; )
        for (Board.Moves it = nd.moves(); (nd = it.next()) != null;) {
          int i1 = ((BitBoard.bMoveIterator)nd).movesDelta();
          int i2 = ((BitBoard.bMoveIterator)nd).movesDelta2();
          int i3 = ((BitBoard.bMoveIterator)nd).movesDelta3();
          md12.add(i1, i2);
          md23.add(i3, i2);
          ref .add(i2, i2);
        }
    }
    System.out.printf("Quality moveDelta 1-2: %.3f 2-3: %.3f\n", md12.quality(ref), md23.quality(ref));
  }
  @Test
  public void evalBench() {
    m_randombit[0].node(Board.e_next).setEval(m_debug_eval,0).score();
    int count = 2000000/m_randombyte.length;
    long time = System.nanoTime();
    int sum = 0;
    for (ByteBoard b : m_randombyte)
      for (int j = 0; j < count; j++) {
        Board.Moves it = b.node(Board.e_white).moves();
        for (Board.Node nd;  (nd = it.next()) != null;) {
          sum += nd.score();
        }
        it = b.node(Board.e_white).moves();
        for (Board.Node nd;  (nd = it.next()) != null;) {
          sum += nd.score();
        }
      }
    System.out.printf("Byte: %.2f\n", (System.nanoTime() - time)/count/m_randombyte.length/128.);
    time = System.nanoTime();
    for (BitBoard b : m_randombit) {
      //b.dump();
      for (int j = 0; j < count; j++) {
        Board.Moves it = b.node(Board.e_white).moves();
        for (Board.Node nd;  (nd = it.next()) != null;) {
          sum -= nd.score();
        }
        it = b.node(Board.e_white).moves();
        for (Board.Node nd;  (nd = it.next()) != null;) {
          sum -= nd.score();
        }
      }
    }
    System.out.printf("Bit: %.2f\n", (System.nanoTime() - time)/count/m_randombit.length/128.);
    assertEquals(0,sum);
    time = System.nanoTime();
    for (BitBoard b : m_randombit) {
      for (int j = 0; j < count; j++) {
        Board.Moves it = b.node(Board.e_white).setEval(m_debug_eval,0).moves();
        for (Board.Node nd;  (nd = it.next()) != null;) {
          sum -= nd.score();
        }
        it = b.node(Board.e_white).setEval(m_debug_eval,0).moves();
        for (Board.Node nd;  (nd = it.next()) != null;) {
          sum -= nd.score();
        }
      }
    }
    System.out.printf("DeepBit: %.2f\n", (System.nanoTime() - time)/count/m_randombit.length/128.);
  }

  void testDebugScore(int width, double radius, int dim) {
    SimGames.ScorerNd scorer = new SimGames.ScorerNd(width,radius,dim);
    for (int j = scorer.length(); --j >= 0;) {
      scorer.update(j, (int)(Math.random()*1000-100));
    }
    scorer.closeScoreArray();
    int j = scorer.length();
    for (int k = 0; k < 100 && --j >= 0; k++)
      assertEquals(scorer.slowScore(j),scorer.getScore(j));
    while ((j-=100) >= 100)
      assertEquals(scorer.slowScore(j),scorer.getScore(j));
    while (--j >= 0)
      assertEquals(scorer.slowScore(j),scorer.getScore(j));
  }
  @Test
  public void debugScore() {
    testDebugScore(7,2.45,4);
    testDebugScore(3,1.8,9);
  }

  double[] calcMean(ArrayList<Double> al) {
    double mean = 0,
           sdev = 0;
    for (ListIterator<Double> i = al.listIterator(); i.hasNext();)
      mean += i.next();
    mean /= al.size();
    for (ListIterator<Double> i = al.listIterator(); i.hasNext();) {
      double e = mean - i.next();
      sdev += e * e * e * e;
    }
    double[] ret = {mean,Math.sqrt(Math.sqrt(sdev/al.size()))};
    return ret;
  }

  @Test
  public void timePlyRelation() throws Exception {
//    ArrayList<Integer> res = new ArrayList<>();
    for (int b = 1; --b >= 0;) {
      BitBoard bb = new BitBoard(ByteBoard.New());
      byte col = (byte) 1;//(Board.rand.nextBoolean() ? -1 : 1);
      Searcher search = new Searcher(bb);
      while (search.adjustedPly(15) == 15) {
        int ply = 2 + Board.rand.nextInt(5);
//        int mTime=99999999;
        for (int q = 0; q < 1; q++) {
//          System.gc();
          //Thread.sleep(1010);
          search.combined_search(col, ply, false);
//          int nodes = Searcher.EV_NODES_ + Searcher.NS_NODES_;
//          mTime = Math.min(mTime,Searcher.NANO_TIME);
        }
        col = (byte)-col;
        search.doPlay(col,Board.rand.nextInt(3)==-1 ? BoardUtil.randomMove(bb,col) : null);
        if (search.play() == null)
          break;
//        int nodes = Searcher.EV_NODES_ + Searcher.NS_NODES_;
//        if (Searcher.PE_NODES_ > 100)
//          System.out.printf("t %d\n", (mTime / nodes));
      }
    }
//    double dar[] = calcMean(res);
//    System.out.printf("T/node sdev %.1f %.1f\n", dar[0], dar[1]);
//    System.out.printf("%d %d %d\n", Searcher.EV_NODES_,Searcher.PE_NODES_,Searcher.NS_NODES_);
  }
  @Test
  public void int64convert_test() {
    for (int x = 0; x < 256; x++)
      assertEquals(x, from8(to8(x)));
    for (int x = 0; x < 256; x++)
      assertEquals(x, from7(to7(x)));
    for (int x = 0; x < 256; x++)
      assertEquals(x, from9(to9(x)));
    for (int x = 0; x < 256; x++)
      assertEquals(to8(x), to8f(x));
  }
  static private long to8f(int x) {
//    long l = x & 1;
//    l |= (x&~1) * 0x2040810204080L;
//    return 0x0101010101010101L & l;
    long l = x * 0x8040201008040201L & 0x8080808080808080L;
    l >>>= 7;
    l = Long.reverseBytes(l);
    return l;
  }
  static private long to8(int x) {
    // System.out.printf("%x ", x);
    long r = 0;
    for (long dr = 1; x != 0; x /= 2, dr *= 256)
      r |= dr * (x & 1);
    // System.out.printf("%x\n", r);
    return r;
  }
  static private long to7(int x) {
    // System.out.printf("%x ", x);
    long r = 0;
    for (long dr = 1; x != 0; x /= 2, dr *= 128)
      r |= dr * (x & 1);
    // System.out.printf("%x\n", r);
    return r;
  }
  static private long to9(int x) {
    long r = 0;
    for (long dr = 1; x != 0; x /= 2, dr *= 512)
      r |= dr * (x & 1);
    return r;
  }
  static private int from8(long a) {
    return (int) ((a * 0x102040810204080L) >>> 56);
  }
  static private int from7(long a) {
    return (int) ((a * 0x80 * 0x4104104104000L) >>> 57 | (a * 0x80 >>> 56) * 128);
  }
  static private int from9(long a) {
    return (int) ((a * 0x101010101010101L) >>> 56);
  }

  private void LinRegression(ArrayList<Double>x,ArrayList<Double>y) {
    // first pass: read in data, compute xbar and ybar
    double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
    int n = 0;
    for (; n < x.size(); n++) {
      sumx  += x.get(n);
      sumx2 += x.get(n) * x.get(n);
      sumy  += y.get(n);
    }
    double xbar = sumx / n;
    double ybar = sumy / n;

    // second pass: compute summary statistics
    double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
    for (int i = 0; i < n; i++) {
      xxbar += (x.get(i) - xbar) * (x.get(i) - xbar);
      yybar += (y.get(i) - ybar) * (y.get(i) - ybar);
      xybar += (x.get(i) - xbar) * (y.get(i) - ybar);
    }
    double beta1 = 0.3;//xybar / xxbar;
    double beta0 = 0.82;//ybar - beta1 * xbar;

    // print results
    System.out.printf("y= %.4f * x + %.4f.", beta1, beta0);

    // analyze results
    int df = n - 2;
    double rss = 0.0;      // residual sum of squares
    double ssr = 0.0;      // regression sum of squares
    for (int i = 0; i < x.size(); i++) {
      double fit = beta1*x.get(i) + beta0;
      rss += (fit - y.get(i)) * (fit - y.get(i));
      ssr += (ybar- y.get(i)) * (ybar- y.get(i));
    }
    System.out.printf(" \t(%.4f-%.4f) sdev %.4f\n", ybar, Math.sqrt(ssr/x.size()), Math.sqrt(rss/x.size()));
//    double R2    = ssr / yybar;
//    double svar  = rss / df;
//    double svar1 = svar / xxbar;
//    double svar0 = svar/n + xbar*xbar*svar1;
//    System.out.println("R^2                 = " + R2);
//    System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
//    System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
//    svar0 = svar * sumx2 / (n * xxbar);
//    System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
//
//    System.out.println("SSTO = " + yybar);
//    System.out.println("SSE  = " + rss);
//    System.out.println("SSR  = " + ssr);
  }
  @Test
  public void log_search_test() {
    ArrayList<Double> Arr1 = new ArrayList<>();
    ArrayList<Double> Arr2 = new ArrayList<>();
    for (int moves = 64; moves < 64; moves++) {
      ArrayList<Double> arr1 = new ArrayList<>();
      ArrayList<Double> arr2 = new ArrayList<>();
      for (int nb = 0; nb < m_randombit.length; nb++) {
        Board board = m_randombit[nb];
        if (board.count() != moves)
          continue;
        here:
        for (byte col = -1; col < 3; col += 2) {
          for (int pe = 3; pe < 4; pe++) {
            Board.Node iterator = board.node(col);
            for (int ply = 6; ply <= 9; ply+=1) {
              Searcher.PE_NODES = 0;
              Searcher.PosBoard pb = Searcher.PosBoard.preeval(pe - 1, iterator);
              double per = Math.log(Searcher.PE_NODES) / pe;
              //ply = (int)(Math.log(1000000)/0.79/per);
              double ply2 = Math.log(1000000)/(per/3 + 0.7);
              pb.eval(-9999, 9999, ply - 1, iterator, 0);
              Board.Pos[] fut = pb.wrapUp(false);
              for (int j = fut.length; --j >= 0;)
                if (fut[j] == null)
                  continue here;
              int totnodes = 1;//Searcher.NS_NODES;
              double evr = Math.log(totnodes) / ply;
              //double rel = evr / per;
              arr1.add(evr);
              arr2.add(per);
            }
          }
        }
      }
      Arr1.addAll(arr1);
      Arr2.addAll(arr2);
      if (arr1.isEmpty())
        continue;
      System.out.printf("%d\t", moves);
      LinRegression(arr2,arr1);
/*      double[] r1 = calcMean(arr1);
      double[] r2 = calcMean(arr2);
      double l4rel = r2[0];
      double per = r1[0] / l4rel;
      double evr = 0.61*per;
/*      System.out.printf("%.4f\t", r1[0]);
      for (int j = 0; j < arr1.size(); j++) {
        arr1.set(j,arr1.get(j)/r1[0]);
        arr2.set(j,arr2.get(j)/r2[0]);
      }
      r1 = calcMean(arr1);
      r2 = calcMean(arr2);
      System.out.printf("%.4f\t |\t", r1[1]);
      System.out.printf("%.4f\t%.4f\n", l4rel, r2[1]);
      for (int j = 0; j < arr1.size(); j++) {
        double d1 = arr1.get(j);
        double d2 = arr2.get(j);
        if (d1 < 0.8 || d1 > 1.2 || d2 < 0.8 || d2 > 1.2) {
          d1 += 0;
        }
      }*/
    }
  }

  @Test
  public void mapTest() {
    for (int nb = 0; nb < m_randombit.length; nb+=10) {
      BitBoard board = new BitBoard(m_randombit[nb]);
      Searcher search = new Searcher(board);
      int scorew = search.negascout(1,Board.e_white);
      Board.Pos posb = search.play().clone();
      int scoreb = search.negascout(1,Board.e_black);
      Board.Pos posw = search.play().clone();
      for (int j = 0; j < 7; j++) {
        board.map(j);
        assertEquals(scoreb,search.negascout(1,Board.e_black));
        assertTrue(board.legal(posw.map(j), Board.e_white));
        assertEquals(scorew,search.negascout(1,Board.e_white));
        assertTrue(board.legal(posb.map(j), Board.e_black));
        assertEquals(posb.map(j).map(j^((j&6)==4?1:0)), posb);
      }
      board.map(7);
      assertEquals(board,m_randombit[nb]);
    }
  }
  @Test
  public void normalizeTest() {
    for (int nb = 0; nb < m_randombit.length; nb++) {
      for (byte col = -1; col <= 1; col += 2) {
        BitBoard board = new BitBoard(m_randombit[nb]);
        board.map(Board.rand.nextInt(8));
        Searcher search = new Searcher(board);
        int score = search.negascout(1,col);
        Board.Pos pos = search.play().clone();
        for (byte col2 = -1; col2 <= 1; col2++) {
          BitBoard bnow  = new BitBoard(board);
          int prot = bnow.normalize(col2);
          Searcher snow = new Searcher(bnow);
          byte c = col2==-1?(byte)-col:col;
          int score2 = snow.negascout(1,c);
          assertEquals(score2, score);
          assertTrue(bnow.legal(pos.map(prot), (byte)-c));
        }
      }
    }
  }
  @Test
  public void rotateNEquals() {
    BitBoard board = new BitBoard(ByteBoard.New());
    final long [] s_a = {2,0};
    board.fromStore(s_a);
    Board[] barr = new Board[8];
    for (int j = 0; j < 8; j++) {
      barr[j] = new BitBoard(board);
      board.map(j);
      for (int q = j/7; q <= j; q++)
        assertThat(board, not(barr[q]));
    }
    assertEquals(board,barr[0]);
  }
  @Test
  public void frontTileDeltaTest() {
    BitBoard bb = new BitBoard(new ByteBoard(Board.e_black));
    Board.Moves ev = bb.node(Board.e_black).moves();
    for (Board.Node it; (it = ev.next()) != null;) {
      assertEquals(3,((BitBoard.bMoveIterator)it).tileDelta());
      assertEquals(5-5+4+6+4,((BitBoard.bMoveIterator)it).frontTileDelta());
    }
    final long [] s_a = {0x8100000000000081L,0};
    bb.fromStore(s_a);
    BitBoard.bMoveIterator ev2 = (BitBoard.bMoveIterator)bb.node(Board.e_white);
    assertEquals(4,ev2.tileDelta());
    assertEquals(12,ev2.frontTileDelta());
  }
  @Test
  public void boardDbTest() {
    for (byte bcol = -1; bcol <= 1; bcol += 2) {
      ByteBoard bb = new ByteBoard(bcol);
      for (byte col = -1; col <= 1; col += 2) {
        Board.Moves it = bb.node((byte)-col).moves();
        while (it.next() != null) {
          Board played = bb.played(it.createPos(), col);
          Board.Pos p = BoardDb.precalc(played, (byte) -col);
          Board played2 = played.played(p, (byte) -col);
          assertThat(played2, not(played));
          p = BoardDb.precalc(played2, col);
          assertTrue(played2.legal(p, col));
        }
      }
    }
  }
  @Test
  public void androidTest() {
    for (int nb = 0; nb < 10+0*m_randombit.length; nb++) {
      Board board = m_randombit[nb];
      Searcher search = new Searcher(board);
      int count = board.count();
      here:
      for (byte col = -1; col < 3; col += 2) {
        //Searcher.NS_NODES = 0;
        search.android_search(col,3);
        Board.Pos[] fut = search.playArray();
        for (int j = fut.length; --j >= 0;)
          if (fut[j] == null)
            continue here;
        int numb = 1000000;//Searcher.NS_NODES;
        if (numb < 100000 || numb > 2000000)
          search.android_search(col,3);
      }
    }
  }
  @Test
  public void scoreTest() {
    for (byte c = -1; c <= 1; c += 2) {
      BitBoard b = new BitBoard(1-(c>>1),2+(c>>1));
      //b.dump();
      //System.out.printf("%d ", c*b.evaluate());
      assertEquals(-20,c*b.evaluate());
      assertEquals(b.evaluate(),new ByteBoard(b).evaluate());
      Board.Moves ev2 = b.node((byte)-c).moves();
      Board.Node nd = ev2.next();
      assertTrue(nd != null);
      assertTrue(b.dolegal(ev2.createPos(), c));
      assertEquals(c*b.evaluate(),nd.score());
      for (int nb = 0; nb < m_randombit.length; nb++) {
        BitBoard board = new BitBoard(m_randombit[nb]);
        Board.Moves it = board.node((byte)-c).moves();
        nd = it.next();
        assertTrue(board.dolegal(it.createPos(), c));
        assertEquals(nd.score(), c*board.evaluate());
        ByteBoard bb = new ByteBoard(m_randombyte[nb]);
        assertTrue(bb.dolegal(it.createPos(), c));
        assertEquals(nd.score(), c*bb.evaluate());
      }
    }
  }
  final long s_endboards[] = { //< 49 to 51
      0x2161f17777c1cL, 0xe3ce8e0e8888020L, -1800, 1000,
      0x10bdc1bd9dd3ad3eL, 0x3e42622c1000L, -1600, 3400,
      0xc15fb41e1611101L, 0x7068043e1e1e2c78L, 3200, -3600,
      0x70a0d1f8b4d8b07cL, 0x1c2c074a270e00L, 4200, -3600,
      0x406e76425c7c5400L, 0x3f9189bda3818000L, -4400, 4400,
      0x181a74583000343eL, 0x20a48ba7cfff0900L, 1000, 0,
      0x3f312b2f072d103eL, 0xed4d078502800L, 3000, -3200,
      0xbef6643038140000L, 0x40081bcf476b3c38L, 2000, -3200,
/*      0x387cee6ff7ffb8fcL, 0xc000101008004000L, -3400, 3400,
      0x3eb5898d83c08000L, 0x876727c3e3d04L, 400, -200,
      0xe0e262c0c143c64L, 0x313159d3f36b0100L, 600, 400,
      0xe0d00810102000L, 0xfe1c2cf6ef6f1f0fL, -2200, 1800,
      0x7c3d5b77655ffc2cL, 0x24081aa00010L, -2200, 2200,
      0x88747ef1b3d38L, 0x7e3438381064c004L, 1600, -600,
      0xc3c2622121c0800L, 0x201d9ddede3b53eL, 2200, -1800,
      0x3d36e4e4e7000L, 0x43c2c11b1b08cfcL, -2200, 2200,
      0x1f0955e3558b0100L, 0x20362a1c2a341e1dL, 3200, -2400,
      0x40e8d0daeede9fL, 0x43c172f24102060L, 1600, -1600,
      0x3f2a06ae4786041cL, 0x15f95138393800L, 2200, -1800,
      0x8667eb5b2b032000L, 0x1918142454fc1e11L, 200, -200,
      0x7e243e122f2e3e29L, 0x98c0edd0d08004L, -4200, 1600,
      0x103d3e1d0a044L, 0x19be7c2c1e2f1c08L, 2600, -800,
      0x407e7f7c2e0e1808L, 0xbc808080d0f06074L, -400, 2200,
      0x3e747cc0403400L, 0xe000b033f3ecaffL, -3800, 4000,
      0x80042f372f3f3820L, 0x7cfad0c8d0c00000L, -200, -200,
      0x10fe4030687e0200L, 0x2f011f0f17013c7cL, -3400, 4400,
      0x804033120e163a1fL, 0x703c4c6cf0e8c4e0L, -200, -1600,
      0x1d3e34ecd4f474feL, 0xa132a0a8800L, 600, -600,
      0x40a090ac94b9bc84L, 0x1c5d6f536a460200L, 2200, -1600,
      0x7c3c7864eed8403cL, 0x2071b11273f00L, -600, 600,
      0x7c3c361f9b257805L, 0x48e064da0702L, -1200, 800,
      0x3e267a60607d3000L, 0x8199859f9f80c080L, -2800, 3200,
      0x20b1ebfdcdd58301L, 0x100c1402322a3c38L, 2800, -3400,
      0x38302836663c7880L, 0xdd7c998c0007fL, 1000, -1600,
      0x4078745854604000L, 0x1e058ba7aa9ebc38L, 1000, -700,
      0x3f07eedc784000L, 0xfe40381123073d30L, -1000, 1200,
      0x4fc8ea498b0a080L, 0xf800705b664e5848L, 600, -1000,
      0x3c3467e230001cffL, 0x181cceffe000L, 600, -400,
      0x143c18142446fefaL, 0x2e6eb5b390101L, 0, 600,
      0x1e06107e2e3e1e0cL, 0x20b8ef81d1410100L, -200, 400,
      0x68cd8fc72201c08L, 0x393327030ddf2100L, -1000, 2400,
      0x807f33550f273f27L, 0x180ccaaf0580000L, 1400, -2000,
      0x7f3a7667f4002064L, 0x408180bff1e00L, 200, -600,
      0x20476f3f373cL, 0x1bfdfb890c00800L, -300, 1400,
      0xf3a0426070e3c3eL, 0x4fad9f8f1c000L, 1600, -2400,
      0x17b5569d0c07fL, 0x103e042a162e3d00L, 800, -1200,
      0x818e8e9ad0bc20L, 0x3e3c7071652f0300L, 1200, 600,
      0x13fbf3a3edeccfcL, 0x1e0000c4c0203000L, 1600, -2800,
      0xbe7c3f0326142840L, 0x8040fcd8e8141eL, -1000, 2000,
      0x1d1fc5c7c7130000L, 0x40203a38386cbc34L, 1600, -2600,
      0x40a197bdc9e10100L, 0x1e1c6842361e3e30L, 1900, -1300,
      0x708c829f1810e000L, 0x307d6066ed1cfcL, -200, -800,
      0x503050313256f7fL, 0x80cfa3c6cda1000L, 4600, -3600,
      0x80d8ac959ffd8201L, 0x7c20506860027cc4L, 1800, -3600,
      0x40c172b417700L, 0x2871e8d4be88ffL, 200, 0,
      0x787ff8d6d3e000L, 0x3c000004282c1efeL, -2000, 3000,
      0x2d66a76f83c7cL, 0x3c7d291509070200L, 0, 1800,
      0x80fda9e58a01L, 0x10387f02561a347eL, 3200, -4600,
      0x9fb6f6bd73f01L, 0x1004109428c0feL, -900, 1000,
      0x808080d4be94a07fL, 0x387f2b416b1f00L, 3800, -3400,
      0xd274f8dcf4f01910L, 0x88a06220a0f0603L, -1600, -400,
      0x20300e0e1a3f7fffL, 0x1c0f311125408000L, 6400, -6400,
      0x841b5bdffbd3cL, 0x3e363e4a42000000L, -600, 800,
      0x6466e7d2b332000L, 0xf0b89082d4cc1c08L, -1600, 1400,
      0x1e3c4858546870feL, 0x36272b170f01L, -400, 1600,
      0x103274b5b7f3e0fL, 0x4bcd8b4a4808000L, 2200, -2800,
      0x3e3c04160f1e3c1eL, 0x7b68f0e1c100L, 1800, -2000,
      0x7c44fcc6a722100cL, 0x38003858dc6ef0L, -200, 1200,
      0xc0a0918ad7be1cL, 0x3c3c5f6e74280020L, -2200, 3200,
      0xfec4fe5e2b170201L, 0x38002054283c3cL, 1800, -1600,
      0x9feb2412b91402L, 0xff6014daed440800L, -2200, 2600,
      0xf0402b163f4000L, 0xfe0cbfd4e8c03838L, -1000, 0,
      0x8fc7d615c3c1a07L, 0x2829ea3c38500L, -1000, -600,
      0x4101ad4f7f303000L, 0xbefe5230000e0c1cL, -1400, 2000,
      0x1e060253427c7838L, 0x81f9fdacbc808000L, -3400, 4200,
      0x1e3e3a162a7c0009L, 0x2181c56955031f00L, 200, -600,
      0x10307fd0b0f0c40L, 0xfe3c7802f4703010L, 1200, -400,
      0x336deead6fe7808L, 0xfc49211529000000L, -400, 800,
      0xc0c3f4222480000L, 0xb0f0c0bcdcb4fc9cL, -1400, 600,
      0x804f05864fea0L, 0x7e34fa0f261b000eL, 800, 1200,
      0x404fef5e8d09800L, 0x7838000a172e647eL, 2700, -3800,
      0x722408506054ec00L, 0x18f4ae9eaa11feL, -600, -200,
      0x7c2c9c64c8c0e000L, 0x10629a373e1cfcL, 2000, -600,
      0x804060f3fafc3a11L, 0x7e3f1e0c04030404L, 1400, -2000,
      0x1c3c7d7c0c073379L, 0x3f3f80c06L, -3000, 2800,
      0x3c0191d3f3530904L, 0xc0fc6e2c0c2c3020L, -2600, 3200,
      0x183c0c063f7fL, 0x3e3e674372784080L, 4000, -4200,
      0xe1e3e7a74eaa000L, 0x200101050b151f79L, -4400, 4400,
      0x898d8b8bcc0807eL, 0x44242647423f3e00L, 3900, -3200,
      0x80e1e2a72781800L, 0x147161d58d87853eL, 2800, -2400,
      0x80c6c292d28200L, 0xb3d393d6d2d1c38L, -2600, 1400,
      0x183e3e6e3f3c00L, 0x7e24c0c090c0807eL, 2200, 600,
      0xfe443a1f3f5ae000L, 0x3804e0c0a41cf8L, -1800, -800,
      0x48f1fbba54081L, 0x81830e0445a3f3eL, -1400, -600,
      0x2064a52260c0cL, 0x90fcf9b5add9b060L, 1200, -600,
      0xc04041438247eL, 0x3f333b7b6bc71a00L, 600, 200,
      0x7e3c7cfcfcfcbcfcL, 0x1020202004000L, 3800, -4000,
      0x103e5c7c38302000L, 0x8fc1a382c78f9c08L, 400, 0,
      0x4060d0c9d5ff00L, 0x203c1f2e362a00feL, 0, 1200,
      0x402050c8d02b1f88L, 0xb0582e362fd4e011L, 1600, -800,/**/
    };
  @Test
  public void endTest() {
/*    for (int j = 0; j < 100; j++) {
      BitBoard bb = new BitBoard(BoardUtil.genRandomBoards(49+j%3));
      int scb = new Searcher(bb).combined_search_fixedPly(Board.e_white, 4, 63-bb.count(), true);
      int scw = new Searcher(bb).combined_search_fixedPly(Board.e_black, 4, 63-bb.count(), true);
      System.out.printf("%s, %d, %d,\n", bb, scb, scw);
    }*/
    assertEquals(s_endboards[2],Searcher.negascoutq(-9999, 9999, 999, new ByteBoard(new BitBoard(s_endboards[0],s_endboards[1])).node(Board.e_white)));
    assertEquals(s_endboards[3],Searcher.negascoutq(-9999, 9999, 999, new ByteBoard(new BitBoard(s_endboards[0],s_endboards[1])).node(Board.e_black)));
    BitBoard bbn = new BitBoard(s_endboards[0],s_endboards[1]);
    for (int j = 0; j < 5; j++)
      Searcher.negascoutq(-9999, 9999, 62-bbn.count(), bbn.node(Board.e_black).setEval(new Evaluator(),63-bbn.count()));
    for (int j = 0; j < s_endboards.length; j+=4) {
      BitBoard bb = new BitBoard(s_endboards[j+0],s_endboards[j+1]);
      int r = Searcher.negascoutq(-9999, 9999, 62-bb.count(), bb.node(Board.e_white).setEval(null, 63-bb.count()));
      assertEquals(r, Searcher.negascoutq(-9999, 9999, 62-bb.count(), new ByteBoard(bb).node(Board.e_white).setEval(null, 63-bb.count())));
      assertEquals(s_endboards[j+2],r);
    }
    long time = System.nanoTime();
    for (int j = 0; j < s_endboards.length; j+=4) {
      BitBoard bb = new BitBoard(s_endboards[j+0],s_endboards[j+1]);
      assertEquals(bb.count(),49+j%3);
      Board.Eval ev = new Evaluator();
      int r = Searcher.negascoutq(-9999, 9999, 62-bb.count(), bb.node(Board.e_white).setEval(ev,63-bb.count()));
      assertEquals(s_endboards[j+2],r);
      r = Searcher.negascoutq(-9999, 9999, 62-bb.count(), bb.node(Board.e_black).setEval(ev,63-bb.count()));
      assertEquals(s_endboards[j+3],r);
    }
    System.out.printf("EvalLast-1: %.2fms\n", (System.nanoTime() - (double)time)/s_endboards.length/.5e6);
    Searcher.negascoutq(-9999, 9999, 63-bbn.count(), bbn.node(Board.e_white).setEval(new Evaluator(),64-bbn.count()));
    time = System.nanoTime();
    for (int j = 0; j < s_endboards.length; j+=4) {
      BitBoard bb = new BitBoard(s_endboards[j+0],s_endboards[j+1]);
      assertEquals(bb.count(),49+j%3);
      Board.Eval ev = new Evaluator();
      int r = Searcher.negascoutq(-9999, 9999, 63-bb.count(), bb.node(Board.e_white).setEval(ev,64-bb.count()));
      assertEquals(s_endboards[j+2],r);
      r = Searcher.negascoutq(-9999, 9999, 63-bb.count(), bb.node(Board.e_black).setEval(ev,64-bb.count()));
      assertEquals(s_endboards[j+3],r);
    }
    System.out.printf("EvalLast: %.2fms\n", (System.nanoTime() - (double)time)/s_endboards.length/.5e6);
  }
  final long s_iterBoards[] = {
      0x40e0e173810L, 0x8383070680406L, 2, -2,
      0x603e241c38L, 0x10387c1c001a0000L, 0, -2,
      0x103e3f30202020L, 0xe1f1c14L, -8, 8,
      0x2030280c0e0e0c08L, 0x1008163270301000L, -2, 0,
      0x824160706060000L, 0x18283878383800L, 2, -2,
      0x207c04160e1c10L, 0x1000fa68700000L, -8, 8,
      0x10380d0f0b042400L, 0x3070341a1100L, -10, 6,
      0x603038341c1cL, 0x4181e0e07080000L, -2, 6,
      0x1c3e34f0502010L, 0xa0c2e1808L, -10, 10,
      0x840c0e0783800L, 0x303c3c1e800010L, -2, 2
  };
  @Test
  public void iterateBench() {
    Board.Eval eval = new Board.Eval() {
      public int score(Board.Node it) {
        BitBoard.bMoveIterator bim = (BitBoard.bMoveIterator)it;
        return -bim.tileDelta();
      }
    };
/*    for (int j = 0; j < 10; j++) {
      BitBoard bb = new BitBoard(BoardUtil.genRandomBoards(30));
      Searcher search = new Searcher(bb);
      search.setEval(eval);
      int scb = search.combined_search(Board.e_white, 10, true);
      int scw = search.combined_search(Board.e_black, 10, true);
      System.out.printf("%s, %d, %d,\n", bb, scb, scw);
    }*/
    Searcher.negascoutq(-9999, 9999, 9, new BitBoard(s_iterBoards[0],s_iterBoards[1]).node(Board.e_white).setEval(eval, -1));
//    BitBoard.s_con_time = 0;
    long time = System.nanoTime();
    for (int j = 0; j < s_iterBoards.length; j+=4) {
      BitBoard bb = new BitBoard(s_iterBoards[j+0],s_iterBoards[j+1]);
      assertEquals(bb.count(),30);
      int r = Searcher.negascoutq(-9999, 9999, 9, bb.node(Board.e_white).setEval(eval, -1));
      assertEquals(s_iterBoards[j+2],r);
      r = Searcher.negascoutq(-9999, 9999, 9, bb.node(Board.e_black).setEval(eval, -1));
      assertEquals(s_iterBoards[j+3],r);
    }
    System.out.printf("iterateBench: %.2fms\n", (System.nanoTime() - (double)time)/s_iterBoards.length/.5e6);
//    System.out.printf("subtime: %.2fms\n", (double)BitBoard.s_con_time/s_iterBoards.length/.5e6);
  }
  @Test
  public void twoStepTest() {
    for (int j = 0; j < s_endboards.length; j+=4) {
      BitBoard bb = new BitBoard(s_endboards[j+0],s_endboards[j+1]);
      assertEquals(bb.count(),49+j%3);
      int ply = 64 - bb.count();
      Searcher search = new Searcher(bb);
      search.setEval(new BitBoard.QEval2step());
      int r = search.combined_search_fixedPly(Board.e_white,2,1+Board.rand.nextInt(ply-2),false);
      assertEquals(s_endboards[j+2],r);
      r = search.combined_search_fixedPly(Board.e_black,2,1+Board.rand.nextInt(ply-2),false);
      assertEquals(s_endboards[j+3],r);
    }
  }

  @Test
  public void scoreIndexTest() {
    EdgeIndexer     edge   = new EdgeIndexer();
    CornerIndexer     corn = new CornerIndexer(4);
    for (BitBoard b0 : m_randombit) {
      BitBoard board = new BitBoard(b0);
      for (int j = 0; j < 8; j++) {
        board.map(j);
        BitBoard.bMoveIterator move = (BitBoard.bMoveIterator) board.node(Board.e_next);
        FastIndexer fi = new FastIndexer(board.node(Board.e_next)); //< due to map()
        assertEquals(edge.index(move, 1), edge.indexfast(fi, 0));
        assertEquals(edge.index(move, 8), edge.indexfast(fi, 3));
        assertEquals(corn.index4fast(fi,0), corn.index(move));
        TrainEval.map(move, 0);
        assertEquals(corn.index4fast(fi,3), corn.index(move));
        TrainEval.map(move, 1);
        assertEquals(corn.index4fast(fi,1), corn.index(move));
        TrainEval.map(move, 2);
        assertEquals(corn.index4fast(fi,2), corn.index(move));
        assertEquals(edge.index(move, 1), edge.indexfast(fi, 2));
        assertEquals(edge.index(move, 8), edge.indexfast(fi, 1));
        TrainEval.map(move, 1);
      }
      assertEquals(b0,board);
    }
  }
  @Test
  public void Edge7IndexTest() {
    byte[] board = new byte[8];
    board[0] = 1;
    int count = 0;
    for (int j = 0; j < 9*9*9*3; j++) {
      for (int k = 1, u = j; k < 8; k++) {
        board[k] = (byte)(u % 3);
        u /= 3;
      }
      int nj = 0;
      for (int k = 1, nk = 1; k < 8; k++) {
        if (board[k] != 0 && board[k] == board[k-1])
          continue;
        if (board[k] == 0 && k > 1 && board[k-1] == 0 && board[k-2] == 0)
          continue;
        nj += nk*board[k];
        nk *= 3;
      }
      if (nj != j)
        continue;
      count++;
      for (int k = 0; k < 8; k++) {
        //System.out.print(board[k] == 0 ? " " : board[k] == 1 ? "W" : "o");
      }
      //System.out.println("");
    }
    System.out.println("7-count:" + count);
  }
  @Test
  public void Edge8IndexTest() {
    BoardDb.Reader db = new MyDb(""+48).reader();
    int mycount[] = new int[9*9*9*9];
    EdgeIndexer8 index = new EdgeIndexer8();
    for (BoardDb.Reader.Data data; (data = db.read()) != null;) {
      Board.Node move = MyDb.fromCodedBoard(data.a,data.b);
      mycount[index.index((BitBoard.bMoveIterator)move, 1).m_i]++;
      mycount[index.index((BitBoard.bMoveIterator)move, 8).m_i]++;
      TrainEval.map((BitBoard.bMoveIterator)move,1);
      mycount[index.index((BitBoard.bMoveIterator)move, 1).m_i]++;
      mycount[index.index((BitBoard.bMoveIterator)move, 8).m_i]++;
    }

    byte[] board = new byte[8];
    int count = 0;
    for (int j = 0; j < 9*9*9*9; j++) {
      for (int k = 0, u = j; k < 8; k++) {
        board[k] = (byte)(u % 3);
        u /= 3;
      }
      int nj = 0;
      for (int k = 0, nk = 1; k < 8; k++) {
        if (k > 0 && board[k] != 0 && board[k] == board[k-1])
          continue;
        if (board[k] == 0 && k > 1 && board[k-1] == 0 && board[k-2] == 0)
          continue;
        nj += nk*board[k];
        nk *= 3;
      }
      if (nj != j)
        continue;
      count++;
      for (int k = 0; k < 8; k++) {
        //System.out.print(board[k] == 0 ? " " : board[k] == 1 ? "W" : "o");
      }
      //System.out.println("");
    }
    System.out.println("8-count:" + count);
  }
}
