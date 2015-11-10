/*import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.*;

@SuppressWarnings("serial")
public class ScoreView extends JPanel {

    public ScoreView() {
      super(true);
    }
    public void addView(int ref, Scorer.Action...action) {
      m_stores.add(new Store(ref,action));
      setPreferredSize(new Dimension((WDTH+LEFT)*(m_stores.size()>1?2:1),(HGTH+TOP)*(m_stores.size()+1)/2));
    }

    protected void processMouseMotionEvent(MouseEvent e) {
      super.processMouseMotionEvent(e);
    }

    protected void processMouseEvent(MouseEvent e) {
      super.processMouseEvent(e);
    }
    static final int LEFT = 10;
    static final int WDTH = 900;
    static final int TOP  = 10;
    static final int HGTH = 350;
    static private int toX(int x) {
      return WDTH*x/Scorer.Counter.s_W;
    }
    static private int toY(Scorer.Counter c, int y, int sc) {
      return HGTH - HGTH*c.m_hits[y]/sc;
    }
    public void paint(Graphics g) {
      super.paint(g);
      int j = 0;
      g.translate(LEFT/2, TOP/2);
      for (Store st : m_stores) {
        st.paint(g);
        g.translate((LEFT+WDTH)*(1-j%2*2), (j%2)*(TOP+HGTH));
        j++;
      }
    }

    static class Store {
      int               m_ref;
      Scorer.Action[]   m_eva;
      Store(int ref, Scorer.Action[] ev) {
        m_eva = ev;
        ref = (ref+6400)/100;
        if ((ref&1) != 0)
          ref = ref + (Board.rand.nextBoolean() ? 1 : -1);
        m_ref = ref >> 1;
      }

      static final Color[] s_colors = {new Color(255,0,0),new Color(172,0,0),new Color(128,0,0),new Color(0,255,0),new Color(0,192,0),new Color(0,128,0),new Color(0,0,255),new Color(0,0,192),new Color(0,0,128)};
      void paint(Graphics g) {
        g.setColor(new Color(0,0,0));
        g.drawRect(0, 0, WDTH, HGTH);
        g.drawLine(toX(m_ref), 0, toX(m_ref), HGTH);
        int j = 0;
        for (Scorer.Action ac : m_eva)
          j = doPaint(g, j, ac);
      }
      private int doPaint(Graphics g, int color, Scorer.Action ac) {
        do {
          g.setColor(s_colors[color++%s_colors.length]);
          Scorer.Counter ct = ac.ct();
          int sc = 1;
          for (int j = 0; j <= Scorer.Counter.s_W; j++)
            sc = Math.max(sc,ct.m_hits[j]);
          int x = toX(0);
          int y = toY(ct,0,sc);
          for (int j = 1; j <= Scorer.Counter.s_W; j++) {
            int x2 = toX(j);
            int y2 = (2*toY(ct, Math.max(j-2, 0), sc)+3*toY(ct, j-1, sc) + 4*toY(ct, j, sc) + 3*toY(ct, Math.min(j+1, Scorer.Counter.s_W), sc)+2*toY(ct, Math.min(j+2, Scorer.Counter.s_W), sc))/14;
            g.drawLine(x, y, x2, y2);
            x = x2; y = y2;
          }
          ac = ac.m_another;
        } while (ac != null);
        return color;
      }
    }
    ArrayList<Store> m_stores = new ArrayList<Store>();
};
/**/