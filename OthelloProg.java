import javax.swing.*;
import java.awt.*;

public class OthelloProg {
  /**
   * @param args
   */
  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JFrame frame = new JFrame("Play othello with me");
        OthelloView view = new OthelloView();
        frame.setContentPane(view);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(new Dimension(500, 500));
        frame.setVisible(true);
        view.doTest();
      }
    });
  }
}
