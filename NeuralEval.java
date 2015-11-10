import java.util.Random;

public class NeuralEval implements Board.Eval {
  final static int n_num  = 64;
  final static int t_bits = 4;
  final static int c_span = (1 << t_bits)-1;
  static Random rand = new Random();

  NeuralEval() {
    m_weight = new byte[n_num*64];
    for (int j = 0; j < m_weight.length; j++)
      m_weight[j] = (byte)(rand.nextInt(c_span)-c_span/2);
    toW();
  }
  void toW() {
    m_w = new long[n_num*t_bits];
    for (int neu = 0; neu < n_num; neu++) {
      for (int b = 0; b < t_bits; b++) {
        for (int x = 0; x < 64; x++) {
          long w = (m_weight[x+neu*64] >> b) & 1;
          m_w[b+t_bits*neu] |= w << x; 
        }
      }
    }
  }
  NeuralEval(NeuralEval a, NeuralEval b) {
    m_weight = new byte[n_num*64];
    int splice = rand.nextInt(m_weight.length);
    for (int j = 0; j < splice; j++)
      m_weight[j] = a.m_weight[j];
    for (int j = splice; j < m_weight.length; j++)
      m_weight[j] = b.m_weight[j];
    for (int t = rand.nextInt(3); --t >= 0;)
      m_weight[rand.nextInt(m_weight.length)] = (byte)(rand.nextInt(c_span)-c_span/2);
    toW();
  }
  public int score(Board.Node bard) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)bard;
    int ret = 0;
    for (int neu = 0; neu < n_num; neu++) {
      int neu1 = 0;
      byte fact = 1 << (byte) (Byte.SIZE - t_bits);
      for (int b = 0; b < t_bits; b++) {
        long m = m_w[b+t_bits*neu];
        int bc = -Long.bitCount(m&move.m_cur); 
        bc    += Long.bitCount(m&move.m_emy);
        neu1 += bc*(byte)(fact<<b);
      }
      neu1 /= fact;
      ret += neu1 > 0 ? 100 : neu1 == 0 ? 0 : -100;
    }
    return ret;
  }
  public int slow_score(Board.Node b) {
    BitBoard.bMoveIterator move = (BitBoard.bMoveIterator)b;
    int ret = 0;
    for (int neu = 0; neu < n_num; neu++) {
      int neu1 = 0;
      for (int x = 0; x < 64; x++) {
        int t  = -((int)(move.m_cur >> x) & 1);
            t +=   (int)(move.m_emy >> x) & 1;
        neu1 += t*m_weight[x+neu*64];
      }
      ret += neu1 > 0 ? 100 : neu1 == 0 ? 0 : -100;
    }
    return ret;
  }
  byte[] m_weight;
  long[] m_w;
}
