import java.util.Arrays;
import java.util.Comparator;

/*
 */

//Games/value: 1.98.  Time/game.    43.9 ms.
public class SimGames {
  public static void main(String[] args) {
    optimize();
  }

  /*
*/
  /*
Games/value: 54.00.  Time/game.    3628.28 ms.
best  id: 9   value: 34.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 70, -70]
best  id: 3   value: 20.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 90, -50]
best  id: 17   value: 19.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 110, -70]
best  id: 15   value: 19.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 110, -70]
best  id: 16   value: 18.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 110, -70]
best  id: 18   value: 16.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 70, -90]
best  id: 20   value: 16.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 70, -90]
best  id: 13   value: 13.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 90, -70]
best  id: 12   value: 13.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 90, -70]
best  id: 23   value: 6.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 90, -90]
best  id: 10   value: 6.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 70, -70]
best  id: 14   value: 1.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 90, -70]
best  id: 0   value: 1. [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 70, -50]
best  id: 5   value: -2.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 90, -50]
best  id: 19   value: -2.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 70, -90]
best  id: 11   value: -2.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 70, -70]
best  id: 22   value: -3.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 90, -90]
best  id: 6   value: -6.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 110, -50]
best  id: 1   value: -7.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 70, -50]
best  id: 8   value: -8.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 110, -50]
best  id: 2   value: -8.        [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 70, -50]
best  id: 21   value: -11.      [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 90, -90]
best  id: 26   value: -14.      [9, 2, 1, -10, 60, -10, 40, 80, -60, -90, 110, -90]
best  id: 25   value: -14.      [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 110, -90]
best  id: 7   value: -15.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 110, -50]
best  id: 24   value: -17.      [9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 110, -90]
best  id: 4   value: -20.       [9, 2, 1, -10, 60, -10, 40, 80, -60, -70, 90, -50]
best  id: 27   value: -53.      [4, 2, 1, -10, 60, -10, 40, 80, -60, -60, 80, -60]
worst id: 0 value: -9999999
[9, 2, 1, -10, 60, -10, 40, 80, -60, -50, 70, -50]
   */
  static final int THREADS = 4;

  static class BoardThread implements Runnable {
    static void Game() {
      int[] boardId = SimGames.GenerateId();
      int line = aGame(boardId);
      SimGames.RegisterWin(line, boardId[0], boardId[1]);
    }

    static int aGame(int[] boardId) {
      int ply0 = Ert.weights(boardId[0])[0];
      int ply1 = Ert.weights(boardId[1])[0];
      Board board = new BitBoard(ByteBoard.New());
      int line;
      boolean more;
      Searcher search = new Searcher(board);
      do {
        search.setEval(new EvalFlex(boardId[0]));
        search.combined_search(Board.e_white, ply0, true);
        Board.Pos p = search.play();
        more = search.doPlay(Board.e_black, null);
        search.setEval(new EvalFlex(boardId[1]));
        line = search.combined_search(Board.e_black, ply1, true);
        p = search.play();
        more |= search.doPlay(Board.e_white, null);
      } while (more);
      return line;
    }

    public void run() {
      while (true)
        Game();
    }
  }

  static void optimize() {
    System.out.printf(" ok...%d cells %d smooth\n", scorer.length(), 0/*
                                                                       * scorer.
                                                                       * div_diff
                                                                       * .length
                                                                       */);
    Thread[] aTread = new Thread[THREADS];
    for (int j = 1; j < THREADS; j++)
      (aTread[j] = new Thread(new BoardThread())).start();
    startTime = System.currentTimeMillis();
    while (true) {
      boolean wait = false;
      try {
        BoardThread.Game();
        wait = true;
      } catch (RuntimeException e) {
        try {
          for (int j = 1; j < THREADS; j++)
            aTread[j].join();
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
        break;
      } finally {
        SimGames.Print(wait);
      }
    }
  }

  static long s_id_ct = 0;
  static long startTime;

  static final class pair {
    pair(int a, int b) {
      key = a;
      val = b;
    }
    public boolean equals(Object O) {
      pair o = (pair) O;
      return o.key == key && o.val == val;
    }
    int key, val;
  }
  static int na=-1, nb=0;
  synchronized static int[] GenerateId() {
    int a, b;
    do {
      if (++na == scorer.length()){
        na = 0;
        nb++;
      }
      //if (++nb >= scorer.length())
      //  nb -= scorer.length();
      a = na;
      b = nb;
      //a = Board.rand.nextInt(scorer.length());
      //b = Board.rand.nextInt(scorer.length());
    } while (a == b && scorer.length() > 1);
    return new int[] { b, a };
  }

  static synchronized void RegisterWin(int lastscore, int idP, int idN) {
    ++s_id_ct;
    int basewin = lastscore == 0 ? 0 : lastscore > 0 ? 1 : -1;
    scorer.update(idP, basewin);
    scorer.update(idN, -basewin);
  }

  static long s_print_check = 10;

  static void Print(boolean wait) {
    if (wait && s_id_ct < s_print_check)
      return;
    long deltaTime = System.currentTimeMillis() - startTime;
    if (wait && deltaTime < s_sec_update * 128)
      s_print_check *= 3;
    else {
      System.out
          .printf("Games/value: %.2f.  ", 2.0 * s_id_ct / scorer.length());
      System.out.printf("Time/game.    %.2f ms.\n", deltaTime
          / (double) s_id_ct);
      s_print_check += s_print_check / (deltaTime * 1e-3 / s_sec_update + .2);
      if (wait)
        return;

      pair[] id_b = new pair[500];
      pair id_w = new pair(0, -9999999);
      scorer.closeScoreArray();
      for (int j = scorer.length(); --j >= 0;) {
        pair mx = scorer.getScore(j);
        mx = new pair(j, (int) (mx.val / mx.key));
        if (mx.val < id_w.val)
          id_w = mx;
        if (wait && mx.val <= 0)
          continue;
        for (int k = 0; k < id_b.length; k++)
          if (id_b[k] == null || mx.val > id_b[k].val) {
            pair t = mx;
            mx = id_b[k];
            id_b[k] = t;
          }
      }
      for (int q = id_b.length;;)
        if (id_b[--q] != null) {
          id_b = Arrays.copyOf(id_b, q + 1);
          break;
        }
      int last_score = Math.max(-999999999, id_b[id_b.length - 1].val);

      while (id_b[0].val >= last_score) {
        System.out.printf("best  id: %d   value: %d.\t", id_b[0].key,
            id_b[0].val);
        System.out.print(new EvalFlex(id_b[0].key) + "\n");
        pair sc2 = scorer.getScore(id_b[0].key);
        scorer.addScoredEntry(id_b[0].key, -sc2.val / 100);
        id_b[0].val = last_score-1;
        for (int k = 0; k < id_b.length; k++) {
          if (id_b[k].val > last_score) {
            pair mx = scorer.getScore(id_b[k].key);
            id_b[k].val = (int) (mx.val / mx.key);
          }
        }
        Arrays.sort(id_b, new Comparator<pair>() {
          public int compare(pair a, pair b) {
            return b.val - a.val;
          }
        });
        // Arrays.sort(id_b, (a,b) -> b.val - a.val);
      }

      System.out.printf("worst id: %d value: %d\n", id_w.key, id_w.val);
      System.out.print(new EvalFlex(id_w.key) + "\n\n");
      for (int j = 1; j < 8; j++) {
        System.out.printf("%.1f ",
            Math.sqrt((j < 4 ? 3 : 1) * EvalFlex.s_rms[j] / EvalFlex.s_rms[0]));
      }
    }
  }

  static class RawScore {
    RawScore(int length) {
      m_update = new int[length];
    }

    pair getScore(int id0) {
      return new pair(1, m_calc[id0]);
    }

    int length() {
      return m_update.length;
    }

    void update(int id, int add) {
      m_update[id] += add;
    }

    void closeScoreArray() {
      m_calc = m_update.clone();
    }

    void addScoredEntry(int id, int add) {
      m_calc[id] += add;
    }

    final int[] m_update;
    int[] m_calc;
  }

  static final class ScorerNd extends RawScore {
    public ScorerNd(int width, double radius, int dim) {
      super((int) Math.pow(width, dim));
      m_dim = dim;
      m_radius = radius;
      m_width = width;
      div_diff = gen_div_diff();
    }

    pair getScore(int id0) {
//      if (id0 % 1000000 == 0)
//        System.out.printf("o");// %.2f \r", (float)id0/div_diff.length);
      long norm = id2norm(id0);
      int score = 0;
      int wtot = 0;
      long radd = (020 - m_width) * 01010101010101010101L;
      for (int id = div_diff.length; --id >= 0;) {
        long r = div_diff[id].norm + norm;
        // System.out.printf("%o %o\n", norm, r);
        boolean accept1 = ((r | r + radd) & 020202020202020202020L) == 0;
        // byte[] ev0 = genEvalu(id0);
        // byte[] ev1 = genEvalu(ido);
        // boolean accept2 = ido >= 0;
        // //System.out.print(Arrays.toString(ev0) + "\n");
        // //System.out.print(Arrays.toString(ev1) + "\n");
        // int code = div_diff[id].code7;
        // int mid2 = (code>=0?1:-1)*s_ct/2;
        // for (int q = 1, qq = 0; qq < m_dim; qq++) {
        // int div = (code+mid2) / s_ct;
        // int mod = code - s_ct*div;
        // code = div;
        // accept2 &= ev0[q] + mod*s_delta_templ[q] == ev1[q];
        // q += qq==0?2:1;
        // }
        // if (accept1 != accept2)
        // throw new RuntimeException();
        if (accept1) {
          int ido = id0 + div_diff[id].code7;
          wtot += div_diff[id].dst;
          score += div_diff[id].dst * m_calc[ido];
        }
      }
      // pair p = slowScore(id0);
      // if (p.key != wtot || p.val != score)
      // p.key++;
      return new pair(wtot, score);
    }

    pair slowScore(int id0) {
      // sanity test
      int score = 0;
      int wtot = 0;
      byte[] ev0 = convertToArray(id0);
      for (int id = 0; id < m_calc.length; id++) {
        byte[] ev1 = convertToArray(id);
        int len = 0;
        for (int j = 0; j < ev0.length; j++) {
          int l = ev0[j] - ev1[j];
          len += l * l;
        }
        if (len > m_radius * m_radius)
          continue;
        int w = 100 - (int) (100.0 / (m_radius * m_radius) * len);
        wtot += w;
        score += w * m_calc[id];
      }
      return new pair(wtot, score);
    }

    byte[] convertToArray(int id0) {
      byte[] ret = new byte[m_dim];
      for (int j = 0; j < m_dim; j++) {
        int i3 = id0 % m_width;
        ret[j] = (byte) (i3 - m_width / 2);
        id0 /= m_width;
      }
      return ret;
    }

    long id2norm(int id) {
      long r = 0;
      for (long r0 = 1; id != 0; r0 *= 0100) {
        r += id % m_width * r0;
        id /= m_width;
      }
      return r;
    }

    static final class DivDiff {
      DivDiff(long n, int d, int c) {
        norm = n;
        dst = d;
        code7 = c;
      }

      long norm;
      int dst;
      int code7;
    }

    private DivDiff[] gen_div_diff() {
      DivDiff[] ret2 = new DivDiff[(int) (10 * Math.pow(m_radius, m_dim))];
      int divdiffl = 0;
      final int mid = m_width / 2;
      for (int id = (int) Math.pow(m_width, m_dim); --id >= 0;) {
        long r = 0;
        int dst = 0;
        int i7 = 0;
        int pow = 1;
        for (int j = 0, id0 = id; j < m_dim; j++) {
          int div = id0 / m_width;
          int m = id0 - m_width * div - mid;
          id0 = div;
          r += (long) m << 6 * j;
          i7 += m * pow;
          pow *= m_width;
          dst += m * m;
        }
        if (dst <= m_radius * m_radius) {
          ret2[divdiffl++] = new DivDiff(r,
              100 - (int) (100.0 / (m_radius * m_radius) * dst), i7);
        }
      }
      return Arrays.<DivDiff> copyOf(ret2, divdiffl);
    }

    final DivDiff[] div_diff;
    final double m_radius;
    final int m_width;
    final int m_dim;
  }

  // static ScorerNd scorer = new ScorerNd(9, 3.01, calcDim());
  static RawScore scorer = new RawScore(Ert.calcLength()+1);
  static int s_sec_update =  220;
  // static ScorerNd scorer = new ScorerNd(7, 3.01, calcDim());
  // static int s_sec_update = 420;
}
