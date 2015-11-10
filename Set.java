
public class Set<T extends Set<T>> {
  int compare(T o){return 0;}
  T add(T item) {
    if (item.m_high != null || item.m_low != null)
      throw new RuntimeException("duplicate add");
    if (m_low == null) {
      m_low = item;
      return null;
    }
    for (T c = m_low; true;) {
      int r = c.compare(item);
      if (r == 0)
        return c;
      if (r < 0) {
        if (c.m_low == null) {
          c.m_low = item;
          return null;
        }
        c = c.m_low;
      }
      else {
        if (c.m_high == null) {
          c.m_high = item;
          return null;
        }
        c = c.m_high;
      }
    }
  }
  T add2(T item) {
    if (item.m_high != null || item.m_low != null)
      throw new RuntimeException("duplicate add");
    for (T c = (T)this; true;) {
      if (c.m_low == null) {
        c.m_low = item;
        return null;
      }
      c = c.m_low;
      while (true) {
        int r = c.compare(item);
        if (r == 0)
          return c;
        if (r < 0)
          break;
        if (c.m_high == null) {
          c.m_high = item;
          return null;
        }
        c = c.m_high;
      }
    }
  }
  T add_3bal(T item) {
    if (item.m_high != null || item.m_low != null)
      throw new RuntimeException("duplicate add");
    for (T c = (T)this; true;) {
      if (c.m_low == null) {
        c.m_low = item;
        return null;
      }
      c = c.m_low;
      while (true) {
        int r = c.compare(item);
        if (r == 0)
          return c;
        if (r < 0)
          break;
        if (c.m_high == null) {
          c.m_high = item;
          return null;
        }
        c = c.m_high;
      }
    }
  }
  T m_high;
  T m_low;
}
/*
    15
  8     7   
 4 4   3 4
 
     1
  1     0
*/
