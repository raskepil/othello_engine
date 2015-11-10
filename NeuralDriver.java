public class NeuralDriver {
  
  final static int THREAD = 1;

  static class mThread implements Runnable {
    mThread(int d0) {
      DEPTH0 = d0;
      MyDb db0 = new MyDb(String.valueOf(DEPTH0)).read();
      System.out.printf("Boards %d\n", db0.size());

      m_boards = new Board.Node[Integer.min(1000,db0.size())];
      m_values = new int       [m_boards.length];
      int s = 0, t = 0;
      for (BoardDb.Iterator entry = db0.new Iterator(); entry.next();) {
        int rem = m_values.length-t;
        if (db0.size()-s == rem || rem > 0 && Board.rand.nextInt(db0.size()) < m_values.length) {
          m_boards[t] = MyDb.fromCodedBoard(entry.raw());
          m_values[t] = entry.value();
          t++;
        }
        s++;
      }
    }
    double quality(Board.Eval e, int count) {
      MeanDev test = new MeanDev();
      MeanDev real = new MeanDev();
      int step = m_values.length / count;
      for (int n = Board.rand.nextInt(step); n<m_values.length;n+=step) {
        Board.Node move = m_boards[n];
        int result      = m_values[n];
        int test_score  = e.score(move);
//        if (test_score != ((NeuralEval)e).slow_score(move))
//          throw new RuntimeException();
        test.add(test_score, result);
        real.add(result, result);
      }
      return test.quality(real);
    }
    double quality(Board.Eval e) {
      MeanDev test = new MeanDev();
      MeanDev real = new MeanDev();
      for (int n = m_values.length; --n>= 0;) {
        Board.Node move = m_boards[n];
        int result      = m_values[n];
        int test_score  = e.score(move);
        if (test_score != ((NeuralEval)e).slow_score(move))
          throw new RuntimeException();
        test.add(test_score, result);
        real.add(result, result);
      }
      return test.quality(real);
    }
    void Game() {
/*        Board.Node node = BoardUtil.genRandomBoards(DEPTH0);
        BitBoard board0 = new BitBoard(node);
        long time = System.nanoTime();
        int value = endCalc(board0);
        time = System.nanoTime() - time;
        if (value != value)
          throw new RuntimeException("abra");
        db0.Store(node, value);
        int score = m_eval.score(node);
        synchronized (_OLD) {
          _OLD.add(score,value);
          _REF.add(value,value);
        }*/
    }

    public void run() {
      while (running) {
        Game();
      }
    }
    boolean running = true;
    private int   DEPTH0;
    Board.Node[]  m_boards;
    int       []  m_values;
  }
  static int indexOf(double[] a, double v) {
    for (int mx = a.length, mn = 0; true;) {
      int t = (mx+mn)/2;
      if (t == mn)
        return mn;
      if (v < a[t])
        mx = t;
      else
        mn = t;
      
    }
  }
  public static void main(String[] args) {
    //if (args.length != 1)
    //  System.out.println("Depth");
    int d0 = 48;
    if (d0 < 30 || 60 < d0)
      System.out.println("Range: 30-d0-60");
    else {
      final mThread ptread = new mThread(d0);
      NeuralEval[] m_eval = new NeuralEval[10000];
      for (int j = 0; j < m_eval.length; j++)
        m_eval[j] = new NeuralEval();
      Thread thread[] = new Thread[THREAD];
      for (int k = 1; k < THREAD; k++)
        (thread[k] = new Thread(ptread)).start();

      //System.out.printf("Q: %f\n", ptread.quality(new SuperEvalFast(),500));

      double[] qualities = new double[m_eval.length];
      while (true) {
        double quality = 0;
        double best = 1e9;
        NeuralEval[] eval2 = new NeuralEval[m_eval.length];
        for (int t = 0; t < m_eval.length; t++) {
          double q = ptread.quality(m_eval[t]);
          if (q < best) {
            best = q;
            //eval2[0] = m_eval[t];
          }
          quality += 1/q/q;
          qualities[t] = quality;
        }
        System.out.printf("Q: %f\n", best);
        for (int t = 0; t < eval2.length; t++) {
          NeuralEval a = m_eval[indexOf(qualities,quality*Board.rand.nextDouble())];
          NeuralEval b = m_eval[indexOf(qualities,quality*Board.rand.nextDouble())];
          eval2[t] = new NeuralEval(a,b);
        }
        
        m_eval = eval2;
      }
    }
  }
}
