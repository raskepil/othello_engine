import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class OthelloView extends JPanel {

    public OthelloView() throws HeadlessException {
        super(true);
        m_board = new BitBoard(new ByteBoard(Board.e_white));
        //m_board = new ByteBoard(Board.e_black);
        if (m_score_debug != 0)
          for (int t = 0; t < 0; t++) {
            Board.Pos x = computerMove(m_board,m_score_debug).m_pos;
            m_board = m_board.played(x,m_score_debug);
            m_score_debug = (byte)-m_score_debug;
          }
        enableEvents(MouseEvent.MOUSE_MOVED);

        m_forfeit = new JButton("Forfeit") {
            protected void fireActionPerformed(ActionEvent event) {
                doTest();
                Board.Pos ps = computerMove(Board.e_black);
                performMove(Board.e_black,ps);
            }
        };
        m_forfeit.setEnabled(true);
        add(m_forfeit);
        add(new JButton("Debug") {
            protected void fireActionPerformed(ActionEvent event) {
                if (demoMode) {
                    doTest();
                    return;
                }
                if (m_debug_ct < 0)
                    m_debug_ct = m_nMaxply;
                m_nMaxply -= m_forfeit.isEnabled() ? 0 : 1;
                byte piece = (m_debug_ct-m_nMaxply)%2==0 ? Board.e_black : Board.e_white;
                Board.Pos ps = computerMove(piece);
                performMove(piece,ps);
            }
        });
        if (m_score_debug != 0) {
          add(new JButton("+") {
            protected void fireActionPerformed(ActionEvent event) {
              m_debug_depth++;
              paintNow();
            }
          });
          add(new JButton("-") {
            protected void fireActionPerformed(ActionEvent event) {
              m_debug_depth--;
              paintNow();
            }
          });
        }
    }
    static byte first = Board.e_black;
    static boolean demo_render = true;
    public void doTest() {
        if (demoMode) {
            first = (byte)-first;
            while (true) {
                m_nMaxply = 10;
                Board.Pos x = computerMove(first);
                boolean done = x == null;
                performMove(first,x);
                m_nMaxply = 10;
                x = computerMove((byte)-first);
                performMove((byte)-first,x);
                if (done && x == null)
                    break;
            }
        }
    }

    private void setTitle(String cc) {
      java.awt.Container cp = getParent().getParent().getParent();
      if (!demoMode || demo_render)
        ((java.awt.Frame)cp).setTitle(cc);
    }

    private Board.Pos computerMove(byte c) {
      StringPos sp = computerMove(m_board,c);
      setTitle(sp.m_s);
      return sp.m_pos;
    }
    class StringPos {
      StringPos(String s, Board.Pos p) { m_pos = p; m_s = s; }
      String    m_s;
      Board.Pos m_pos;
    }
    private StringPos computerMove(Board board, byte c) {
        long now = demoMode ? startTime : System.currentTimeMillis();
        Searcher search = new Searcher(board);
        int line = search.combined_search(c, m_nMaxply, randomMove);
        now = System.currentTimeMillis() - now;
        String cc = "Othello  " + line + ":  (" + now + "ms) ";
        for (int n = search.playArray().length-1; --n >= 0;)
          cc += search.playArray()[n] != null ?
              (1 + search.playArray()[n].x()) + "," + (1 + search.playArray()[n].y()) + "   " :
              "forfeit  ";
        return new StringPos(cc,search.play());
    }

    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        Rectangle2D.Float rect = firstTile();

        int x = (int) ((e.getX() - rect.getX()) / rect.getWidth());
        int y = (int) ((e.getY() - rect.getY()) / rect.getHeight());
        boolean leg =  0 <= x && x < 8 && 0 <= y && y < 8 && m_board.legal(new Board.Pos(x,y), player());
        setCursor(new Cursor(leg ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private byte player() {
      return m_score_debug == 0 ? Board.e_white : m_score_debug;
    }
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        Rectangle2D.Float rect = firstTile();

        int x = (int) ((e.getX() - rect.getX()) / rect.getWidth());
        int y = (int) ((e.getY() - rect.getY()) / rect.getHeight());
        if (e.getID() == MouseEvent.MOUSE_RELEASED && e.getButton() == MouseEvent.BUTTON1) {
            Board.Pos pos = new Board.Pos(x,y);
            BitBoard bb2 = new BitBoard(m_board);
            int mapid = bb2.normalize(player());
            System.out.printf(" %s, %d, // %d\n", bb2.toString(), pos.map(mapid).bitpos(), m_debug_depth);
            final Board bb = m_board.played(pos,player());
            if (bb != m_board) {
                final StringPos[] nx = new StringPos[1];
                Thread thread = new Thread(new Runnable() {
                  public void run() {
                    Thread.yield();
                    nx[0] = computerMove(bb,Board.e_black);
                  }
                });
                thread.start();

                performMove(player(), pos);
                if (m_score_debug == 0) {
                  setCursor(new Cursor(Cursor.WAIT_CURSOR));
                  try {
                    thread.join();
                  } catch (InterruptedException ee) {
                  }
                  setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                  setTitle(nx[0].m_s);
                  performMove(Board.e_black, nx[0].m_pos);
                }
            }
        }
    }
    public void paint(final Graphics g) {
      super.paint(g);
      Rectangle2D.Float rect = firstTile();

      g.setColor(new Color(0, 0, 0));
      for (int z = 0; z <= 8; z++) {
        g.drawLine((int)rect.getX(), (int)(rect.getY()+z*rect.getHeight()), (int)(rect.getX()+8*rect.getWidth()),
            (int)(rect.getY()+z*rect.getHeight()));
        g.drawLine((int)(rect.getX()+z*rect.getWidth()), (int)rect.getY(), (int)(rect.getX()+z*rect.getWidth()),
            (int)(rect.getY()+8*rect.getHeight()));
      }
      final byte scd = (byte)(m_score_debug / 2);
      ArrayList<Thread> threads = new ArrayList<Thread>();
      for (final Board.Pos z = new Board.Pos(); z.Next();) {
        if (m_board.get(z) != 0)
          circle(z, m_board.get(z), g);
        if (scd != 0 && m_board.legal(z,scd)) {
          final Board bb = m_board.played(z,scd);
          final Board.Pos zz = z.clone();
          Thread th = new Thread(new Runnable() {
            public void run() {
              Searcher search = new Searcher(bb);
              search.setEval(m_eval);
              int line = search.combined_search(scd, m_debug_depth, randomMove);
              Rectangle r = tileRect(zz);
              g.drawString(String.valueOf(line),10+(int)r.getX(),20+(int)r.getY());
              g.drawString(String.valueOf(m_debug_depth),0,20);
            }
          });
          threads.add(th);
          th.start();
          //Searcher search = new Searcher(bb);
          //search.setEval(m_eval);
          //int line = search.combined_search(scd, m_debug_depth, randomMove);
          /*Board.Pos[] par = search.deepPlayArray(m_debug_depth, scd, line);
          Board.Node node = bb.node(scd, 0, null);
          Board.Node pnod = node;
          for (int j = par.length; --j>=0; ) {
            pnod = node;
            node = node.moves().doLegal(par[j]);
          }
          int tlin = m_eval.score(node);
          int tpar = m_eval.score(pnod);*/
          //Rectangle r = tileRect(z);
          //g.drawString(String.valueOf(line),10+(int)r.getX(),20+(int)r.getY());
          //g.drawString(String.valueOf(m_debug_depth),0,20);
        }
      }
      for (Thread th : threads)
        try {
          th.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
    }
    Board.Eval m_eval = new SuperEvalFast();
    public final void circle(Board.Pos pos, byte c, Graphics g) {
        Rectangle rect = tileRect(pos);
        if (c == Board.e_white)
            g.setColor(new Color(0, 200, 0));
        else if (c == Board.e_black)
            g.setColor(new Color(150, 0, 0));
        else
            g.setColor(new Color(200, 200, 200));

        g.fillOval(rect.x+2,rect.y+2,rect.width-4,rect.height-4);
        g.setColor(new Color(50, 50, 50));
        g.drawOval(rect.x+2,rect.y+2,rect.width-4,rect.height-4);
    }
    private final Rectangle2D.Float firstTile() {
        final int TOP = 30;
        int w = getWidth();
        int h = getHeight()-TOP;
        int m = w < h ? w : h;
        float dm = m / 18f;
        if (w < h)
            return new Rectangle2D.Float(dm,TOP+(h-w)/2f+dm,2*dm,2*dm);
        else
            return new Rectangle2D.Float((w-h)/2f+dm,TOP+dm,2*dm,2*dm);
    }
    private final Rectangle tileRect(Board.Pos pos) {
        int x = pos.x();
        int y = pos.y();
        Rectangle2D.Float rect = firstTile();
        return new Rectangle(((int)(rect.getX()+x*rect.getWidth())), (int)(rect.getY()+y*rect.getHeight()),
                (int)(rect.getWidth()), (int)(rect.getHeight()));
    }

    public boolean performMove(byte c, Board.Pos pos) {
        Board newb = m_board.played(pos,c);
        if (newb == null)
            return false;
        Board oldb = m_board;
        m_board = newb;
        if (!demoMode && m_score_debug == 0) {
            m_forfeit.setEnabled(m_board.node(Board.e_white).moves().next() == null);
            Rectangle rect = tileRect(pos);
            for (int j = 0; j < 3; j++) {
              paintImmediately(rect);
              try {
                Thread.sleep(150);
                m_board = oldb;
                paintImmediately(rect);
                Thread.sleep(150);
              } catch (InterruptedException e) {}
              m_board = newb;
            }
        }
        if (!demoMode || demo_render) {
          m_score_debug *= -1;
          paintNow();
        }
        return true;
    }
    void paintNow() {
      m_score_debug *= 2;
      paintImmediately(0, 0, getWidth(), getHeight());
      m_score_debug /= 2;
    }

    static       boolean demoMode  = false;
    static final boolean randomMove= false;
    static final boolean m_test    = false;
    final long           startTime = System.currentTimeMillis();

    private byte    m_score_debug = -1; //< 0 | 1/-1
    private int     m_debug_depth = 10;

    private JButton m_forfeit;
    private int     m_debug_ct = -1;
    private int     m_nMaxply = m_test ? 10 : 8;
    private Board   m_board;
};
