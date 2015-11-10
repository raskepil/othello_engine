import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class EvaluateScoreComb {
  static class mThread {
    mThread() {}
    void Game() {
      MyDb db_base = new MyDb("48").read();
      BoardDb db   = new MyDb("48a").read();
      if (db.size() == 0)
        db   = new BoardDb();
      int size = db_base.size();
      double meanScore = 0;
      System.out.printf("Calculated: %d\n", size);
      //AllEdgeIndexer aei = new AllEdgeIndexer(size);
      int tt = 0;
/*      Board.rand.setSeed(1); ///////////////////////////////////**/
      for (BoardDb.Iterator entry = db_base.new Iterator(); entry.next();) {
        BitBoard board = entry.raw();
        int result = entry.value();
        Board.Node move = MyDb.fromCodedBoard(board);
        //aei.train(move, result);
        meanScore += result;
        if (!(db instanceof MyDb) && Board.rand.nextInt(1000) < 50) {
          db.insert(board,result);
          continue;
        }
        //eval48.train(move,result);
        eval48.trainLongSlow(move,entry.raw_value(), false);
        int sc = eval48.scoreSlow(move);
        if (++tt%2000000 == 0) System.out.println(tt);
      }
/*      aei.optimize();
      MeanDev md = new MeanDev();
      MeanDev tr = new MeanDev();
      for (BoardDb.Iterator entry = db_base.new Iterator(); entry.next();) {
        BitBoard board = entry.raw();
        int result = entry.value();
        Board.Node move = MyDb.fromCodedBoard(board);
        int y = aei.calc(move);
        md.add(y, result);
        tr.add(result, result);
      }
      System.out.printf("\nMDscore: %f\n\n", md.quality(tr));
*/      meanScore /= size;

      System.out.println("edge " + eval48.edge.count());
      System.out.println("cor4 " + eval48.score4.count());
      System.out.println("cor5 " + eval48.score5.count());
      System.out.println("pie  " + eval48.pie8.count());
//      System.out.println("AllEdge " + aei.count());
      /*JFrame frame = new JFrame("Data");
      ScoreView scoreView = new ScoreView();
      JScrollPane scrollPane = new JScrollPane();
      scrollPane.getViewport().add(scoreView);
      frame.setContentPane(scrollPane);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setSize(new Dimension(1100, 500));
      frame.setVisible(true);
      //final int[] s_s = {0,1,2,4,7,10,11,12,13};
      //for (int j : s_s)
      //  scoreView.addView(0, eval48.scores[j].index(-4), eval48.scores[j].index(-3), eval48.scores[j].index(-2), eval48.scores[j].index(-1), eval48.scores[j].index(0), eval48.scores[j].index(1), eval48.scores[j].index(2), eval48.scores[j].index(3), eval48.scores[j].index(4));
     /* for (Scorer.Counter.S_S=0;Scorer.Counter.S_S <= 3;Scorer.Counter.S_S+=0.2)*/ {
      int fail = 0;
      int ofail = 0;
      int total = 0;
      int nfail = 0;
      final MeanDev OLDfix = new MeanDev();
      final MeanDev NEWfix = new MeanDev();
      final MeanDev NEWwgt = new MeanDev();
      final MeanDev REF = new MeanDev();
      final MeanDev EDGE = new MeanDev();
      final MeanDev ED14 = new MeanDev();
      final MeanDev NEW4 = new MeanDev();
      final MeanDev NEW5 = new MeanDev();
            MeanDev SUPR = new MeanDev();
            MeanDev PIE  = new MeanDev();
      final MeanAccum accumulator = new MeanAccum();
      for (BoardDb.Iterator entry = db.new Iterator(); entry.next();) {
        if (Board.rand.nextInt(db.size()) > 100000)
          continue;
        Board.Node move = MyDb.fromCodedBoard(entry.raw());
        int result = entry.value();
        TrainEval.Evalhelp ev = eval48.new Evalhelp(move);

        int nres4 =  ev.calc4.calc();
        int nres5 =  ev.calc5.calc();

        OLDfix.add(ev.ores, result);
        REF.add(result, result);
        NEWwgt.add(ev.pres0.calc(), result);
        NEWfix.add(ev.psedres, result);
        NEW4.add(nres4, result);
        PIE.add(ev.pie.calc(), result);
        int e4 = ev.edge4.calc();
        EDGE.add(e4, result);
        ED14.add(ev.edge1.calc(),result);
        NEW5.add(nres5, result);
        accumulator.addArr(result, ev.pres10, ev.pres11, ev.pie, ev.edge4,ev.calc4,ev.calc5);
        int supr = Scorer.guessAt(ev.pres10, ev.pres11, ev.edge1,ev.calc5);
        SUPR.add(supr, result);

        if (supr*result < 0 && Math.abs(result) > 1000 && Board.rand.nextInt(100) == 0) {
          //scoreView.addView(result, ev.calc4, ev.edge4, ev.pres10, ev.pres11);
        }

        total++;
        if (ev.psedres * result <= 0)
          fail++;
        if (ev.ores * result <= 0)
          ofail++;
        if (supr * result <= 0)
          nfail++;
      }
      System.out.printf("\nRef: %.2f / %.0f\n", meanScore, REF.stddev());
      System.out.printf("%d Fail: %d, old: %d new: %d\n", total,fail,ofail, nfail);
      System.out.printf("OLDfix: %.3f. NEWfix: %.3f. NEWwgt: %.3f. NEW4: %.3f. NEW5: %.3f. EDGE: %.3f. ED14: %.3f. SUPR %.3f. PIE %.3f.\n", OLDfix.quality(REF), NEWfix.quality(REF), NEWwgt.quality(REF), NEW4.quality(REF), NEW5.quality(REF), EDGE.quality(REF), ED14.quality(REF), SUPR.quality(REF), PIE.quality(REF));
      accumulator.print(REF);
      }    }
    TrainEval   eval48  = new TrainEval();
  }
  public static void main(String[] args) {
    final mThread ptread = new mThread();
    ptread.Game();

    for (int j = 0; j < ptread.eval48.scores.length; j++) {
      int consec = 0;
      int ct = 0;
      int rel = 0;
      for (int k = 1; consec < 10; k++) {
        int ind = (k ^ ((k & 1) == 1 ? -1 : 0)) / 2;
        Scorer.Action ac = ptread.eval48.scores[j].index(ind);
        int calc = Scorer.Counter.calc(ac);
        if (calc == 0)
          consec++;
        else if (ind != 0) {
          consec = 0;
          ct++;
          rel += calc / ind;
        }
      }
      System.out.printf("%d,", rel/ct);
    }
    System.out.printf("\nPres10avg: %.2f", ptread.eval48.scores[10].average());
    System.out.printf("\nPres11avg: %.2f", ptread.eval48.scores[11].average());
    System.out.printf("\nPres12avg: %.2f", ptread.eval48.scores[12].average());
    System.out.printf("\nPres13avg: %.2f", ptread.eval48.scores[13].average());
  }
}
/*
 990 1463485 (17) 15485 (0) 26793 (0) 159310 (0)*
41645 Fail: 9086, old: 14653 new: 7419
OLD: 1.125. CUr: 0.462. NEW4: 0.385. NEW5: 0.331. EDGE: 0.354. BLOB: 0.431.
 1000000 0100000 1100000 0010000 1010000 0110000 1110000 0001000 1001000 0101000 1101000 0011000 1011000 0111000 1111000 0000100 1000100 0100100 1100100 0010100 1010100 0110100 1110100 0001100 1001100 0101100 1101100 0011100 1011100 0111100 1111100 0000010 1000010 0100010 1100010 0010010 1010010 0110010 1110010 0001010 1001010 0101010 1101010 0011010 1011010 0111010 1111010 0000110 1000110 0100110 1100110 0010110 1010110 0110110 1110110 0001110 1001110 0101110 1101110 0011110 1011110 0111110 1111110 0000001 1000001 0100001 1100001 0010001 1010001 0110001 1110001 0001001 1001001 0101001 1101001 0011001 1011001 0111001 1111001 0000101 1000101 0100101 1100101 0010101 1010101 0110101 1110101 0001101 1001101 0101101 1101101 0011101 1011101 0111101 1111101 0000011 1000011 0100011 1100011 0010011 1010011 0110011 1110011 0001011 1001011 0101011 1101011 0011011 1011011 0111011 1111011 0000111 1000111 0100111 1100111 0010111 1010111 0110111 1110111 0001111 1001111 0101111 1101111 0011111 1011111 0111111 1111111 0000000 1000000 0100000 1100000 0010000 1010000 0110000 1110000 0001000 1001000 0101000 1101000 0011000 1011000 0111000 1111000 0000100 1000100 0100100 1100100 0010100 1010100 0110100 1110100 0001100 1001100 0101100 1101100 0011100 1011100 0111100 1111100 0000010 1000010 0100010 1100010 0010010 1010010 0110010 1110010 0001010 1001010 0101010 1101010 0011010 1011010 0111010 1111010 0000110 1000110 0100110 1100110 0010110 1010110 0110110 1110110 0001110 1001110 0101110 1101110 0011110 1011110 0111110 1111110 0000001 1000001 0100001 1100001 0010001 1010001 0110001 1110001 0001001 1001001 0101001 1101001 0011001 1011001 0111001 1111001 0000101 1000101 0100101 1100101 0010101 1010101 0110101 1110101 0001101 1001101 0101101 1101101 0011101 1011101 0111101 1111101 0000011 1000011 0100011 1100011 0010011 1010011 0110011 1110011 0001011 1001011 0101011 1101011 0011011 1011011 0111011 1111011 0000111 1000111 0100111 1100111 0010111 1010111 0110111 1110111 0001111 1001111 0101111 1101111 0011111 1011111 0111111 1111111
 0.87037 1.05598 0.85718 0.82889 0.73464 0.86480 0.77771 3.50362 0.89424 1.09356 0.86143 0.87540 0.75024 0.87936 0.78396 0.43138 0.34420 0.35233 0.32848 0.35012 0.32676 0.34273 0.34373 0.46117 0.36641 0.37734 0.34574 0.37516 0.34502 0.36240 0.35782 0.35422 0.30841 0.31267 0.31329 0.29948 0.30093 0.31441 0.33026 0.37789 0.32527 0.33096 0.32573 0.31981 0.31542 0.32965 0.34108 0.37126 0.32269 0.33135 0.30312 0.32106 0.29614 0.30517 0.29310 0.38440 0.33401 0.34325 0.31311 0.33338 0.30661 0.31608 0.30224 0.38466 0.36983 0.36306 0.39601 0.37989 0.39403 0.41477 0.43482 0.44891 0.40564 0.40745 0.42039 0.41937 0.41846 0.44197 0.45214 0.35589 0.31198 0.31562 0.29781 0.31763 0.30013 0.30749 0.30413 0.37814 0.33008 0.33535 0.31339 0.33687 0.31568 0.32410 0.31738 0.30991 0.28337 0.28457 0.27950 0.28255 0.27745 0.28313 0.28879 0.32924 0.29885 0.30125 0.29257 0.29940 0.29094 0.29739 0.30013 0.33403 0.30230 0.30720 0.28734 0.30267 0.28441 0.28992 0.27991 0.34584 0.31274 0.31814 0.29686 0.31369 0.29408 0.29998 0.28866 0.33130 0.34984 0.34114 0.38302 0.36137 0.38429 0.40034 0.42458 0.37392 0.37477 0.37011 0.40014 0.38949 0.40247 0.41996 0.43762 0.31450 0.28764 0.28748 0.28128 0.29425 0.28644 0.29117 0.29433 0.33377 0.30321 0.30429 0.29458 0.31089 0.29990 0.30544 0.30578 0.27338 0.26161 0.25943 0.26425 0.26231 0.26536 0.26865 0.27976 0.29015 0.27498 0.27372 0.27547 0.27694 0.27709 0.28097 0.28960 0.30613 0.28236 0.28509 0.27167 0.28346 0.27086 0.27472 0.26922 0.31699 0.29191 0.29506 0.28033 0.29356 0.27971 0.28390 0.27720 0.32863 0.31926 0.31711 0.32588 0.32736 0.33108 0.33737 0.34864 0.35476 0.33885 0.33878 0.34193 0.34815 0.34702 0.35446 0.36175 0.31242 0.29098 0.29206 0.28257 0.29607 0.28625 0.28969 0.28748 0.32760 0.30395 0.30584 0.29419 0.30962 0.29782 0.30185 0.29780 0.28212 0.26917 0.26887 0.26621 0.27097 0.26780 0.27019 0.27332 0.29587 0.28084 0.28120 0.27658 0.28330 0.27828 0.28115 0.28259 0.30080 0.28211 0.28428 0.27250 0.28362 0.27240 0.27533 0.26918 0.31042 0.29076 0.29327 0.28051 0.29263 0.28047 0.28368 0.27663
907,333,-76,163,-306,37,151,-191,-198,110,354,76,105,-19, */
