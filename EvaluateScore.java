import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class EvaluateScore {
  static String qualityAbsoluteDev(MyDb db_base, Board.Eval eval, int max) {
    MeanDev REF = new MeanDev();
    MeanDev VAL = new MeanDev();
    long rand   = 1;
    int db_size = db_base.size();

    for (BoardDb.Iterator entry = db_base.new Iterator(); entry.next();) {
      rand = (rand * 1009 + 257) % 0x7fffffff;
      if (max > 0 && rand % db_size > max)
        continue;
      Board.Node move = MyDb.fromCodedBoard(entry.raw());
      int result      = entry.value();
      int test_score  = eval.score(move);
      REF.add(result, result);
      VAL.add(test_score, result);
    }
    return String.format("%.6f", VAL.quality(REF));
  }
  static class Pair {
    Pair(int e, int r) { eval = e; result = r; }
    int eval;
    int result;
  }
  static String qualityOrderDev(MyDb db_base, Board.Eval eval, int max) {
    ArrayList<Pair> array = new ArrayList<Pair>();
    MeanDev REF = new MeanDev();
    MeanDev VAL = new MeanDev();
    long rand   = 1;
    int db_size = db_base.size();

    for (BoardDb.Iterator entry = db_base.new Iterator(); entry.next();) {
      rand = (rand * 1009 + 257) % 0x7fffffff;
      if (max > 0 && rand % db_size > max)
        continue;
      Board.Node move = MyDb.fromCodedBoard(entry.raw());
      int result      = entry.value();
      int test_score  = eval.score(move);
      array.add(new Pair(test_score,result));
    }
    array.sort(new Comparator<Pair>() {
      public int compare(Pair a, Pair b) { return a.eval - b.eval; }
    });
    for (int j = 0; j < array.size(); ++j)
      array.get(j).eval = j;
    array.sort(new Comparator<Pair>() {
      public int compare(Pair a, Pair b) { return a.result - b.result; }
    });
    for (Pair p : array) {
      int order = p.eval;
      int faker = array.get(order).result;
      REF.add(p.result, p.result);
      VAL.add(faker, p.result);
    }
    return String.format("%.6f", VAL.quality(REF));
  }
  public static void main(String[] args) {
    System.out.printf("Algorit: %d\n", Ert.calcLength());
    final String[] bases = {/*"43","44","45","46","48a", "48", "47", "48", "48a",*/
        "48x",
        "48base",
//        "46",
//        "43",
//        "42",
//        "40",
//        "34a"
//        "53",
//        "58"
    };
    MyDb[] db_bases = new MyDb[bases.length];
    for (int j = 0; j < bases.length; j++) {
      db_bases[j] = new MyDb(bases[j]).read();
      System.out.printf("Db %s size: %d\n", bases[j], db_bases[j].size());
    }

    for (MyDb db_base : db_bases) {
      TreeMap<String,Integer> result_set = new TreeMap<String,Integer>();
      int no_var = 0;
      for (int score_id = Ert.calcLength(); --score_id >= 0;) {
        Board.Eval eval = new EvalFlex(score_id);
        String quality = qualityOrderDev
                         //qualityAbsoluteDev
            (db_base,eval,50000);
  //      if (quality < 0.4)
  //        quality = quality(db_base,eval,0);
        Integer r = result_set.put(quality, score_id);
        if (r != null)
          no_var++;
     //     System.out.printf("No-variance %s %s\n", eval, new BoardDebug.DebugEval(r));
     //   System.out.printf("%7d\t%.3f\n", score_id, quality);
      }
      if (no_var != 0)
        System.out.printf("No-variance %s w: %d\n", db_base, no_var);
      int count = 0;
      for (Map.Entry<String,Integer> entry : result_set.entrySet()) {
        Board.Eval eval = new EvalFlex(entry.getValue());
        System.out.printf("%d\t%s\t%s\n",  entry.getValue(), entry.getKey(), eval.toString());
        count++;
        if (count == 20)
          break;
  /*      if (count % 200 == 0) {
          try {
            System.in.read();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }*/
      }
      System.out.printf("\n");
    }
    for (int j = 1; j < 8; j++) {
      System.out.printf("%.1f ", Math.sqrt((j < 4 ? 3:1)*EvalFlex.s_rms[j]/EvalFlex.s_rms[0]));
    }
    System.out.printf("\nhits: %d %d\n", Board.n0, Board.n1);
    System.out.printf("hits: %d %d\n", Board.n2, Board.n3);
    System.out.printf("hits: %d\n", Board.ndiff);
  }
}
