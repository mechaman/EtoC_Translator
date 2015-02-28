import java.util.*;

public class Vars {
    
    public String ID;
    public int line_declared;
    public int nesting_depth;
    public Vector<Integer> assigned;
    public Vector<Integer> used;
 
    Vars() 
    {
        line_declared = 0;
        nesting_depth = 0;
        assigned = new Vector<Integer>();
        used = new Vector<Integer>();
    }//constructor
}
