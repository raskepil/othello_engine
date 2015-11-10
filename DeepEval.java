class TrainEval {
  TrainEval() {
    for (int j = 0; j < scores.length; j++)
      scores[j] = new IntIndexer();
  }
  TrainEval(BoardDb.Reader db) {
    this();
    long rest = 0;
    long restn = 0;
    System.out.printf("Results <= 1200: ");
    for (BoardDb.Reader.Data data; (data = db.read()) != null;) {
      Board.Node move = MyDb.fromCodedBoard(data.a,data.b);
      trainFast(move,data.value());
      rest ++;
      restn += Math.abs(data.value()) <= 1200 ? 1 : 0;
    }
    System.out.printf("%d/%d\n", restn, rest);
    for (int j = edge.m_a.length; --j >= 0;) {
      Scorer.Counter ac = edge.new Action(j).ct();
      double f = ac.confidence();
      m_fastedge[2*j]   = (int)f;
      m_fastedge[2*j+1] = (int)(f*ac.mean());
    }
    for (int j = score4.m_a.length; --j >= 0;) {
      Scorer.Counter ac = score4.new Action(j).ct();
      double f = ac.confidence();
      m_fastcorn[2*j]   = (int)f;
      m_fastcorn[2*j+1] = (int)(f*ac.mean());
    }
  }
  static int forfeitIndex(BitBoard.ParNode p) {
    int r = 0;
    for (int z = 0; p.m_top != null; p = p.m_top, z++)
      if (p.m_res == 0)
        r = 2*(z&1)-1;
    return r;
  }
  static void map(BitBoard.bMoveIterator b, int n) {
    if ((n & 3) == 1) {
      b.m_cur = Long.reverse(b.m_cur);
      b.m_emy = Long.reverse(b.m_emy);
    }
    if ((n & 1) == 0) {
      b.m_cur = Long.reverseBytes(b.m_cur);
      b.m_emy = Long.reverseBytes(b.m_emy);
    }
  }
  int scoreSlow(Board.Node b) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)b;
    Scorer.Action edge4 = edge.index(move, 1);
    edge4.append(edge.index(move, 8));
    Scorer.Action calc4 = score4.index(move);
    map(move,0);
    calc4.append(score4.index(move));
    map(move,1);
    calc4.append(score4.index(move));
    map(move,2);
    calc4.append(score4.index(move));
    edge4.append(edge.index(move, 1));
    edge4.append(edge.index(move, 8));
    map(move,1);
    return Scorer.guessAt(/*ev.pres11, */edge4,calc4);
  }
  int scoreFast(Board.Node b) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)b;
    int lasts = FastIndexer.vgetH(move, 8);
    int corne = 27 * FastIndexer.corn0(move);
    int firsl = corne + lasts;

    lasts    += 27 * Scorer.ge2(move, 17);
    int i     = 27 * Scorer.ge2(move, 10);
    int upper = FastIndexer.hgetH(move, 1);
    i        += upper;
    int m     = Math.min(i, lasts);
    i        += lasts - m + 81 * (m + 3 * corne);
    int conf  = m_fastcorn[2*i];
    int res   = m_fastcorn[2*i+1];
    upper += corne;
    corne = 27 * FastIndexer.corn1(move);
    lasts = FastIndexer.hgetL(move, 4);
    i     = corne + lasts;
    m     = Math.min(i, upper);
    i    += upper - m + 243 * m;
    conf += m_fastedge[2*i];
    res  += m_fastedge[2*i+1];
    lasts+= 27 * Scorer.ge2(move, 13);
    i     = 27 * Scorer.ge2(move, 22);
    upper = FastIndexer.vgetH(move, 15);
    i    += upper;
    m     = Math.min(i, lasts);
    i    += lasts - m + 81 * (m + 3 * corne);
    conf += m_fastcorn[2*i];
    res  += m_fastcorn[2*i+1];
    upper+= corne;
    corne = 27 * FastIndexer.corn2(move);
    lasts = FastIndexer.vgetL(move, 39);
    i     = corne + lasts;
    m     = Math.min(i, upper);
    i    += upper - m + 243 * m;
    conf += m_fastedge[2*i];
    res  += m_fastedge[2*i+1];
    lasts+= 27 * Scorer.ge2(move, 46);
    i     = 27 * Scorer.ge2(move, 53);
    upper = FastIndexer.hgetL(move, 60);
    i    += upper;
    m     = Math.min(i, lasts);
    i    += lasts - m + 81 * (m + 3 * corne);
    conf += m_fastcorn[2*i];
    res  += m_fastcorn[2*i+1];
    upper+= corne;
    corne = 27 * FastIndexer.corn3(move);
    lasts = FastIndexer.hgetH(move, 57);
    i     = corne + lasts;
    m     = Math.min(i, upper);
    i    += upper - m + 243 * m;
    conf += m_fastedge[2*i];
    res  += m_fastedge[2*i+1];
    lasts+= 27 * Scorer.ge2(move, 50);
    i     = 27 * Scorer.ge2(move, 41);
    upper = FastIndexer.vgetL(move, 32);
    i    += upper;
    m     = Math.min(i, lasts);
    i    += lasts - m + 81 * (m + 3 * corne);
    conf += m_fastcorn[2*i];
    res  += m_fastcorn[2*i+1];
    upper+= corne;
    m     = Math.min(firsl, upper);
    i     = upper + firsl - m + 243 * m;
    conf += m_fastedge[2*i];
    res  += m_fastedge[2*i+1];
    return res/(conf == 0 ? 1 : conf);
  }
  void trainFast(Board.Node node, int result) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)node;
    Scorer.Action addere = edge  .new Action();
    Scorer.Action adder4 = score4.new Action();
    addere.append(edge.index(move,1)).add(result);
    addere.append(edge.index(move,8)).add(result);
    adder4.append(score4.index(move)).add(result);
    map(move,0);
    adder4.append(score4.index(move)).add(result);
    map(move,1);
    adder4.append(score4.index(move)).add(result);
    map(move,2);
    adder4.append(score4.index(move)).add(result);
    addere.append(edge.index(move,1)).add(result);
    addere.append(edge.index(move,8)).add(result);
    map(move,1);
  }
  static int getBestIndex(long s, int a, int b) {
    s >>= Short.SIZE;
    return Math.min((int)(s>>4*a)&0xf, (int)(s>>4*b)&0xf);
  }
  static int getBestIndex(long s, int a, int b, int c) {
    s >>= Short.SIZE;
    return Math.min((int)(s>>4*c)&0xf, Math.min((int)(s>>4*a)&0xf, (int)(s>>4*b)&0xf));
  }
  void test(BitBoard.bMoveIterator move, int res, int res_i, java.util.function.Supplier<Integer> supp) {
    if (GenEndDatabase.m_eval == null)
      GenEndDatabase.m_eval = new SuperEvalFast();
    int ac = supp.get();
    Board.Moves moves = move.moves();
    Board.Node mynod;
    int min = 10000;
    int count = new BitBoard(move).count();
    int ply0  = Math.min(6, (63-count)/2-2); //GC mem limit
    while ((mynod = moves.next())!=null) {
      move.m_cur ^= 1L << moves.createPos().bitpos();
      boolean hit = ac != supp.get();
      move.m_cur ^= 1L << moves.createPos().bitpos();
      if (hit) {
        Searcher.PosBoard pb = Searcher.PosBoard.preeval(ply0-1, mynod.setEval(GenEndDatabase.m_eval, ply0));
        int num = -pb.eval(-9999, 9999, 61-count, mynod.setEval(null,62-count), 0);
        min = Math.min(min, num);
      }
    }
    if (res_i != GenEndDatabase.mThread.scoreToShort(min-10000,res))
      if (min!=10000)
        throw new RuntimeException("compare");
  }
  static int SLOPE = 0;
  void myAdd(Scorer.Action sc, int res, int res_i) {
    res -= SLOPE*100;
    if (SLOPE == 0 || res_i == 15)
      sc.add(res);
    else {
      sc.add(res+200*res_i);
      if (res_i == 0)
        for (int j = 0; j < SLOPE; j++)
          sc.add(res-400);
    }
  }
  void trainLongSlow(Board.Node node, long raw_res, boolean test) {
    short result = (short)raw_res;
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)node;
    Scorer.Action addere = edge  .new Action();
    Scorer.Action adder4 = score4.new Action();
    
    int res_i = getBestIndex(raw_res,0,1);
    if (test)
      test(move, result, res_i, () -> edge.index(move,1).m_i);
    myAdd(addere.append(edge.index(move,1)), result, res_i);

    res_i = getBestIndex(raw_res,6,7);
    if (test)
      test(move, result, res_i, () -> edge.index(move,8).m_i);
    myAdd(addere.append(edge.index(move,8)), result, res_i);
    
    res_i = getBestIndex(raw_res,0,7,8);
    if (test)
      test(move, result, res_i, () -> score4.index(move).m_i);
    myAdd(adder4.append(score4.index(move)), result, res_i);
    map(move,0);

    res_i = getBestIndex(raw_res,5,6,11);
    if (test)
      test(move, result, res_i, () -> score4.index(move).m_i);
    myAdd(adder4.append(score4.index(move)), result, res_i);
    map(move,1);

    res_i = getBestIndex(raw_res,1,2,9);
    if (test)
      test(move, result, res_i, () -> score4.index(move).m_i);
    myAdd(adder4.append(score4.index(move)), result, res_i);
    map(move,2);

    res_i = getBestIndex(raw_res,3,4,10);
    if (test)
      test(move, result, res_i, () -> score4.index(move).m_i);
    myAdd(adder4.append(score4.index(move)), result, res_i);

    res_i = getBestIndex(raw_res,4,5);
    if (test)
      test(move, result, res_i, () -> edge.index(move,1).m_i);
    myAdd(addere.append(edge.index(move,1)), result, res_i);

    res_i = getBestIndex(raw_res,2,3);
    if (test)
      test(move, result, res_i, () -> edge.index(move,8).m_i);
    myAdd(addere.append(edge.index(move,8)), result, res_i);
    map(move,1);
  }
  void trainExtras(Board.Node node, int result) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)node;
    //Scorer.Action corners4 = corn4.index(adder4);
    Scorer.Action adder14 = edge14.new Action();
    Scorer.Action adder5 = score5.new Action();
    Scorer.Action pie    = pie8.new Action();
    //if (Board.rand.nextInt(200000) == 0 && result != -new Searcher(board).combined_search_fixedPly(Board.e_white, 64-1-board.count(), false))
    //  throw new RuntimeException();

    adder14.append(edge14.index(move,1)).add(result);
    adder14.append(edge14.index(move,8)).add(result);
    adder5.append(score5.index(move)).add(result);
    pie.append(pie8.index(move, 1)).add(result);
    pie.append(pie8.index(move, 8)).add(result);
    map(move,0);
    adder5.append(score5.index(move)).add(result);
    pie.append(pie8.index(move, 1)).add(result);
    pie.append(pie8.index(move, 8)).add(result);
    map(move,1);
    adder5.append(score5.index(move)).add(result);
    pie.append(pie8.index(move, 1)).add(result);
    pie.append(pie8.index(move, 8)).add(result);
    map(move,2);
    adder5.append(score5.index(move)).add(result);
    adder14.append(edge14.index(move,1)).add(result);
    adder14.append(edge14.index(move,8)).add(result);
    pie.append(pie8.index(move, 1)).add(result);
    pie.append(pie8.index(move, 8)).add(result);
    map(move,1);

    if (move.m_top != null)
      scores[10].index(move.movesDelta()).add(result);

    IndEv evaluator = new IndEv();//;
    while (evaluator.m_index < 10) {
      int sco = evaluator.score(move);
      scores[evaluator.m_index++].index(sco).add(result);
    }
    int r = move.frontTileDelta();
    scores[11].index(r).add(result);
    r = move.frontTileDeltaRel();
    scores[12].index(r).add(result);
    r = move.tileDelta();
    scores[13].index(r).add(result);
    r = forfeitIndex(move);
    scores[14].index(r).add(result);
  }
  void train(Board.Node node, int result) {
    trainFast(node,result);
    trainExtras(node,result);
  }
  EdgeIndexer     edge = new EdgeIndexer();
  EdgeIndexer14 edge14 = new EdgeIndexer14();
  CornerIndexer score4 = new CornerIndexer(4);
  CornerIndexer score5 = new CornerIndexer(5);
  PieIndexer    pie8   = new PieIndexer();
  void train2____(Board.Node node, int result) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)node;
    Scorer.Action adder4 = score4.new Action();
    adder4.append(score4.index(move));
    map(move,0);
    adder4.append(score4.index(move));
    map(move,1);
    adder4.append(score4.index(move));
    map(move,2);
    adder4.append(score4.index(move));
    map(move,1);
  }
  IntIndexer[] scores  = new IntIndexer[15];
  int[] m_fastedge = new int[2*edge.m_a.length];
  int[] m_fastcorn = new int[2*score4.m_a.length];
  static final int s_fa[] = {1089,158,-237,-4,-397,-121,49,-238,-180,82};
  final class Evalhelp {
    int psedres = 0;
    int ores    = 0;
    Scorer.Action pres10;
    Scorer.Action pres0 = null;
    Scorer.Action pres11;
    Scorer.Action pres12;
    Scorer.Action pres13;
    Scorer.Action pres14;
    Scorer.Action edge4;
    Scorer.Action edge1;
    Scorer.Action calc4;
    Scorer.Action calc5;
    Scorer.Action pie;
    Evalhelp(Board.Node node) {
      BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)node;
      int remaining = 64 - new BitBoard(move).count();

      pres10 = scores[10].index(move.movesDelta());
      IndEv evaluator = new IndEv();//;
      while (evaluator.m_index < 10) {
        int sco = evaluator.score(move);
        psedres += sco*s_fa[evaluator.m_index];
        ores    += sco*Evaluator.scale(remaining,6,Evaluator.s_templboard[evaluator.m_index]-Evaluator.s_weights[1]);
        Scorer.Action a = scores[evaluator.m_index++].index(sco);
        if (pres0 == null) pres0 = a; else pres0.append(a);
      }
      int r = move.frontTileDelta();
      pres11 = scores[11].index(r);
      r = move.frontTileDeltaRel();
      pres12 = scores[12].index(r);
      r = move.tileDelta();
      pres13 = scores[13].index(r);
      r = forfeitIndex(move);
      pres14 = scores[14].index(r);
      edge4 = edge.index(move, 1);
      edge4.append(edge.index(move, 8));
      edge1 = edge14.index(move, 1);
      edge1.append(edge14.index(move, 8));
      calc4 = score4.index(move);
      calc5 = score5.index(move);
      pie  = pie8.index(move, 1);
      pie.append(pie8.index(move, 8));
      map(move,0);
      calc4.append(score4.index(move));
      calc5.append(score5.index(move));
      pie.append(pie8.index(move, 1));
      pie.append(pie8.index(move, 8));
      map(move,1);
      calc4.append(score4.index(move));
      calc5.append(score5.index(move));
      pie.append(pie8.index(move, 1));
      pie.append(pie8.index(move, 8));
      map(move,2);
      calc4.append(score4.index(move));
      calc5.append(score5.index(move));
      edge4.append(edge.index(move, 1));
      edge4.append(edge.index(move, 8));
      edge1.append(edge14.index(move, 1));
      edge1.append(edge14.index(move, 8));
      pie.append(pie8.index(move, 1));
      pie.append(pie8.index(move, 8));
      map(move,1);
    }
  }
}
abstract class DeepEval implements Board.Eval {
  static TrainEval m_train;
  DeepEval() {}
  DeepEval(String level) {
    if (m_train != null)
      throw new RuntimeException();
    m_train = new TrainEval(new MyDb(level).reader());
  }
}
class DeepEvalslow extends DeepEval {
  DeepEvalslow() {}
  DeepEvalslow(String string) {
    super(string);
  }
  public int score(Board.Node b) {
    return m_train.scoreSlow(b);
  }
}
class DeepEvalfast extends DeepEval {
  DeepEvalfast() {}
  public int score(Board.Node b) {
    return m_train.scoreFast(b);
  }
}
