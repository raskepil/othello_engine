import java.util.Arrays;

class Ert {

  static final byte[] s_aSupFx = { 0, 0, 0,   0,  0,   0,  1,  1,   1,  1, 1, 1 };

  static final int calcLength() {
    int r = 1;
    for (int j = 0; j < s_weights2.length; j++)
      r *= 1 + s_weights2[j][2] * (s_weights2[j][1] == 0 || s_aSupFx[j] != 0 ? 0 : 1);
    /*
     * r *= s_templboards.length; for (int j = 0; j < s_delta_templ.length; j++)
     * r *= 1 + 2*s_delta_count*(s_delta_templ[j]<=0?0:1);
     */return r;
  }
  static int[] s_a_a = {4, 2, 1, -10, 60, -10, 40, 80, -60, -60, 80, -60};
  static int[] weights(int id) {
    if (id == calcLength())
      return s_a_a;
    int[] ret = new int[s_weights2.length];
    for (int j = 0; j < ret.length; j++) {
      int l = 1 + s_weights2[j][2] * (s_weights2[j][1] == 0 || s_aSupFx[j] != 0 ? 0 : 1);
      ret[j] = s_aSupFx[j] * SuperEvalSlow.s_aSuper[j] + (1-s_aSupFx[j])*s_weights2[j][0] + (id % l) * s_weights2[j][1];
      id /= l;
    }
    if (id != 0)
      throw new RuntimeException("Too long");
    return ret;
  }

  // start step count(-1)
  private static final byte[][] s_weights2 =
    { {  4, 1, 0  }, // depth
      {  3, 1, 0 }, // main eval
      {  1, 0, 0 }, // moveDelta eval-func
      { -2, -2, 6 }, // piece bonus @EVAL
      { 10, 10, 7 }, // moves bonus @EVAL
      { -4, -4, 6 }, // fronttile & rel penalty @EVAL
      {-10, 10, 5 }, // piece bonus @EVAL+10
      { 60, 10, 7 }, // moves bonus @EVAL+10
      { 10,-10, 6 }, // fronttile & rel penalty @EVAL+10
      {-40,-20, 8 }, // piece bonus @EVAL-30
      {  0, 20, 8 }, // moves bonus @EVAL-30
      { 60,-20, 3 }, // fronttile & rel penalty @EVAL-30
  };
}

// 1M scores for search count 38->48.
class EvalFlex implements Board.Eval {
  EvalFlex(int id) {
    this(Ert.weights(id));
  }
  EvalFlex(int... id) {
    m_wgts = id;
  }
  static int EVAL_CT = 48;
  static final byte s_fa[] = { 109, 16, -24, -1, -40, -12, 5, -24, -18, 8 };
  static final int s_wg[] = { 0, 0 };
  static Board.Eval[] s_evals = { new Evaluator(), new Evaluator(s_fa, s_wg), new DeepEvalslow(String.valueOf(EVAL_CT)+"base"), new DeepEvalfast() };
  static double[] s_rms = new double[8];
  static int xx = 0;
  public int score(Board.Node it) {
    BitBoard.bMoveIterator bim = (BitBoard.bMoveIterator) it;
    int count = bim.count();
    int score = (int) (s_evals[m_wgts[1]].score(bim) * (m_wgts[1] == 0 ? 1.5 : m_wgts[1] == 1 ? 0.9 : 1.0));
    s_rms[0]++;
    s_rms[1 + m_wgts[1]] += score * score;
    int n = bim.tileDelta();
    int t = xx;
    score += t;
    xx = t;
    s_rms[4] += n * n;
    score += interp2(count - EVAL_CT, m_wgts[3], m_wgts[9], m_wgts[6], n);
    int fact = 1;
    switch (m_wgts[2]) {
    case 2: n = bim.movesDelta3();
            fact = 6; break;
    case 1: n = bim.movesDelta2(); break;
    case 0: n = bim.movesDelta();
    }
    s_rms[5] += n * n;
    score += (interp2(count - EVAL_CT, m_wgts[4], m_wgts[10], m_wgts[7], n)) / fact;
    n = bim.frontTileDelta();
    //n = -bim.neighborTile();
    s_rms[6] += n * n;
    score += interp2(count - EVAL_CT, m_wgts[5], m_wgts[11], m_wgts[8], n);
    s_rms[1] += (double) score * score;
    return score / 10;
  }
  
  static int interp2(int pm50, int l48, int down, int up, int value) {
    //down = 1*l48 + 3 * down;
    return pm50 > 0 ? value*(l48*(10-pm50) + pm50 * up) / 10 : value*((l48*(30+pm50) - down*pm50)) / 30;
  }
  public String toString() {
    return Arrays.toString(m_wgts);
  }
  int[] m_wgts;
}

class SuperEvalSlow extends EvalFlex {
  static final int[]  s_aSuper = { 4, 2, 1, -10, 45, -10, 20, 110, -20, -40, 40, 0 };
  SuperEvalSlow() {
    super(s_aSuper);
  }
}

final class SuperEvalFast extends SuperEvalSlow {
  SuperEvalFast() {
    m_wgts[1] = 3;
  }
}

