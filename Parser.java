/** Authors: Julien Hoachuck, Carolyn Chan, Tiffany Chan, Nelson Johansen
* Title: E to C Translator
**/
import java.util.*;

public class Parser {

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }

    private Scan scanner;
    Parser(Scan scanner) {
        this.scanner = scanner;
        System.out.println("#include <stdio.h>");
        System.out.println("#include <math.h>");
        System.out.println("main() {");
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
    }
    //required for translator
    Token prev;
    //variables used to handle special cases in C syntax
    int inVar = 0;
    int inDo = 0;
    int inST = 0;
    int inFA = 0;
    int inPrint = 0;
    int inRoot = 0;
    int saveFa1 = 0;
    int saveFa2 = 0;
    int sqrCount = 0;
    int rootCount = 0;
    //variables used to hold epxresions for later use in translation
    Token itr;
    String exprRoot = new String();
    String expr1 = new String();
    String expr2 = new String();
    
    //translates E code to C code based on Token.kind
    public void translate() 
    {
        if((inRoot == 1) && (tok.kind != TK.ROOT))
        {
            if(is(TK.ID))
            {
                exprRoot = exprRoot + "x_" + tok.string;
            }
            else
            {
                exprRoot = exprRoot + tok.string;
            }
            return;
        }
        
        if(saveFa1 == 1)
        {
            if(is(TK.ID))
            {
                expr1 = expr1 + " x_" + tok.string;
            }
            else
            {
                expr1 = expr1 + tok.string;
            }
        }
        
        if(saveFa2 == 1)
        {
            if(is(TK.ID))
            {
                expr2 = expr2 + " x_" + tok.string;
            }
            else
            {
                expr2 = expr2 + tok.string;
            }
        }
        
        if(inRoot == 1)
        {
            if(is(TK.ID))
            {
                exprRoot = exprRoot + "x_" + tok.string;
            }
            else
            {
                exprRoot = exprRoot + tok.string;
            }
        }
        
        if(tok.kind == TK.VAR)
        {
            inVar = 1;
        }
        else if(tok.kind ==  TK.ID)
        {
            if(inVar == 1)
            {
                System.out.println("int x_" + tok.string + " = -12345;");
            }
            else
            {
                System.out.print("x_" + tok.string + " ");
            }
        }
        else if(tok.kind == TK.RAV)
        {
            inVar = 0;
        }
        else if(is(TK.ASSIGN))
        {
            System.out.print(" = ");
        }
        else if(is(TK.NE))
        {
            System.out.print(" != ");
        }
        else if(is(TK.EQ))
        {
            System.out.print(" == ");
        }
        else if(is(TK.LE) || is(TK.GE) || is(TK.GT) || is(TK.LT)
                || is(TK.DIVIDE) || is(TK.TIMES) ||
                is(TK.MINUS) || is(TK.PLUS) || is(TK.RPAREN) || is(TK.LPAREN))
        {
            System.out.print(" " + tok.string + " ");
        }
        else if(tok.kind == TK.PRINT)
        {
            System.out.print("printf(" + "\"%d\\n\", ");
        }
        else if(tok.kind == TK.NUM)
        {
            System.out.print(tok.string);
        }
        else if(tok.kind == TK.IF)
        {
            System.out.print("if( ");
        }
        else if(tok.kind == TK.FI)
        {
            if(inDo == 1)
            {
                System.out.println("continue;");
            }
            
            System.out.println("}//end of if block");
        }
        else if(tok.kind == TK.DO)
        {
            inDo = 1;
            System.out.println("while(1) {");
            System.out.println("if(" );
        }
        else if(tok.kind == TK.OD)
        {
            inDo = 0;
            System.out.println("continue;");
            System.out.println("}//do with last gaurd for do block");
            System.out.println("break;");
            System.out.println("}//end of do while loop");
        }
        else if(tok.kind == TK.FA)
        {
            inFA = 1;
            System.out.println("for(");
        }
        else if(tok.kind == TK.TO)
        {
            System.out.println("; x_" + itr.string + " <= " );
        }
        else if(tok.kind == TK.ST)
        {
            System.out.println("if(");
        }
        else if(tok.kind == TK.AF)
        {
            if(inST == 1)
            {
                System.out.println("} //end of conditional");
            }
            
            inFA = 0;
            inST = 0;
            System.out.println("}//end for loop");
        }
        else if(tok.kind == TK.ARROW)
        {
            if(prev.kind == TK.ELSE)
            {
                System.out.println("{");   
            }
            else if(inFA == 0)
            {
                System.out.println(") {");
            }
            else
            {
                return;
            }
        }
        else if(tok.kind == TK.BOX)
        {
            if(inDo == 1)
            {
                System.out.println("continue;");
            }
            System.out.println("}//end of if statement");
            System.out.print("else if( ");
        }
        else if(tok.kind == TK.ELSE)
        {
            System.out.print("}");//close block above else
            System.out.print(" else");
        }
        else
        {
        }
        
      prev = tok;
    }
    
    
    //Symbol Table data structure
    HashMap<String,Stack<Vars>> symTable = new HashMap<String,Stack<Vars>>();
    //Holds data about which variables were declared at each depth.
    HashMap<Integer,ArrayList<String>> currentDepth 
            = new HashMap<Integer,ArrayList<String>>();
    //Data structure to preserve values for printing
    ArrayList<PrintingQueue> symTablePreserved = new ArrayList<PrintingQueue>();
    //Global variable to allow printing iff program was valid
    boolean invalid; 
    //depth variable
    int depth;
    //keep track of previous variable for certian statements
    Token currVar;

    public class mySort implements Comparator<PrintingQueue> {
        @Override
        public int compare(PrintingQueue pQ1, PrintingQueue pQ2)
        {
            if(pQ1.depth >= pQ2.depth)
                return 1;
            else
                return -1;
        }
    }
    
    private void program() {
        block();

        System.out.print("}//end of program");

        if(invalid == false)
            printPreserved();
    }

    private void block() {
        if(is(TK.VAR))
        {
            declarations();
        }
        invalid = true;
        statement_list();           
        removeSymbols();
    }

    //updates or adds symbol to symbol table data structure    
    private void updateSymbols() {
        //create a new class for the current declaration of the variable
        Vars var = new Vars();
        //stack to hold variables
        Stack<Vars> stack = new Stack<Vars>();
        
        if(!(symTable.containsKey(tok.string)))
        {     
            var.ID = tok.string;
            var.line_declared = tok.lineNumber;
            var.nesting_depth = depth; 
            
            stack.push(var);
            symTable.put(tok.string, stack);               
        }
        else //symTable contains the key and its stack already!!!
        {
            stack = symTable.get(tok.string);
            
            var.ID = tok.string;   
            var.line_declared = tok.lineNumber;
            var.nesting_depth = depth;
                
            stack.push(var);
            symTable.put(tok.string, stack);
        }
    }
    
    //when variables go out of scope remove them from symbol table
    private void removeSymbols() {
        //stack to hold variables
        Stack<Vars> stack = new Stack<Vars>();
        //holds the key values to be poped for current depth
        ArrayList<String> temp = new ArrayList<String>();
        //holds class being preserved currently
        PrintingQueue pQ = new PrintingQueue();
        //holds class being worked with
        Vars var = new Vars();
        
        temp = currentDepth.get(depth);
        
        if(temp == null)
            return; 
        
        for(String key: temp)
        {           
            stack = symTable.get(key);           
            var = stack.pop();
            
            pQ.depth = var.line_declared;
            pQ.pQueue.add(var);

            if(stack.isEmpty())
                symTable.remove(key);
            else
                symTable.put(key, stack);
        }    

        symTablePreserved.add(pQ);    
    }
    
    //prints out the variables on symbol table in declared order
    private void printPreserved() {
        //used to hold Objects coming off stack
        Vars var = new Vars();
        //Variable to hold queue being printed currently
        Queue<Vars> pQ = new LinkedList<Vars>();

        //Variables for printing out correct values in ()
        int prev = -1;
        int counter = 1;
        int size = 0;
        String current = null;

        Collections.sort(symTablePreserved, new mySort());

        for(PrintingQueue p : symTablePreserved)
        {
            pQ = p.pQueue;
        
            while(!(pQ.isEmpty()))
            {
                size = 0;
                var = pQ.remove();
                
                System.err.println(var.ID);
                System.err.println("  declared on line " + var.line_declared + 
                    " at nesting depth " + var.nesting_depth);
                if(var.assigned.isEmpty())
                {
                    System.err.println("  never assigned");
                }
                else
                {
                    System.err.print("  assigned to on:");
                    for(Integer x : var.assigned)
                    {
                        if(var.ID != current)
                        {
                            prev = -1;
                            counter = 1;
                            current = var.ID;
                        }
                    
                        if(x == prev && size < (var.assigned.size()-1))
                        {
                            counter++;
                        }
                        else if(x != prev && counter == 1)
                        {
                            System.err.print(" " + x);  
                        }
                        else if(x != prev && counter > 1)
                        {
                            System.err.print("(" + counter + ")");
                            System.err.print(" " + x);
                            counter = 1;
                        }
                        else
                        {
                            counter++;
                            System.err.print("(" + counter + ")");
                        }
               
                        prev = x;
                        size++;
                     }
                    System.err.println();
                }
            
                if(var.used.isEmpty())
                {
                    System.err.println("  never used");
                }
                else
                {
                    System.err.print("  used on:");
            
                    prev = -1;
                    counter = 1;
                    size = 0;
                    current = null;
            
                    for(Integer y : var.used)
                    {
                        if(var.ID != current)
                        {
                            prev = -1;
                            counter = 1;
                            current = var.ID;
                        }
               
                        if(y == prev && size < (var.used.size()-1))
                        {
                             counter++;
                        }
                        else if(y != prev && counter == 1)
                        {
                             System.err.print(" " + y);  
                        }
                        else if(y != prev && counter > 1)
                        {
                             System.err.print("(" + counter + ")");
                             System.err.print(" " + y);
                             counter = 1;
                        }
                        else
                        {
                            counter++;
                            System.err.print("(" + counter + ")");
                        }
               
                         prev = y;
                         size++;
                     }
                     System.err.println();
                }//end of print block
            } //end of while
        }//end of for  
    }
    
    //when a variable is assigned update its information on symbol table
    private void variableAssigned() {
        //create a new class for the current declaration of the variable
        Vars var = new Vars();
        //stack to hold variables
        Stack<Vars> stack = new Stack<Vars>();

        if(symTable.containsKey(tok.string))
        {
            stack = symTable.get(tok.string);
            var = stack.pop();
            
            var.assigned.add(tok.lineNumber);
            
            stack.push(var);
            symTable.put(tok.string, stack);
        }
        else
        {
            System.err.println("undeclared variable " + tok.string +" on line " 
                    + tok.lineNumber);
            System.exit(1);
        }
    }
    
    //when a variable is used update information on symbol table
    private void variableUsed(Token t) {
        //create a new class for the current declaration of the variable
        Vars var = new Vars();
        //stack to hold symTable stack
        Stack<Vars> stack = new Stack<Vars>();
        
        if(symTable.containsKey(t.string))
        {
            stack = symTable.get(t.string);
            var = stack.pop();
            
            var.used.add(t.lineNumber);
            
            stack.push(var);
            symTable.put(t.string, stack);
        }
        else
        {
            System.err.println("undeclared variable " + t.string +" on line " 
                    + t.lineNumber);
            System.exit(1);
        }
        
    }
   
    private void declarations() {
        translate(); //gen code for start of variable declartion
        mustbe(TK.VAR);
        
        //redeclartion checking structure and current block variables to pop 
        //before leaving scope
        ArrayList<String> sentinel = new ArrayList<String>();
        
        while( is(TK.ID) ) {
            //sentinel holds all variable IDs for current var rav statement.
            if(!(sentinel.contains(tok.string)))
            {
                sentinel.add(tok.string);
                updateSymbols();
                translate(); //gen code for each ID
            }
            else
            {
                System.err.println("variable " + tok.string + 
                        " is redeclared on line " + tok.lineNumber);
            }
            scan();
            
        }
        currentDepth.put(depth, sentinel);
        translate(); //gen code for end of variable declartion
        mustbe(TK.RAV);
    }
    
    private void statement_list() {
        
        while(is(TK.ID) || is(TK.PRINT) || is(TK.IF) || is(TK.DO) || is(TK.FA))
        {
            invalid = false;
            statement();
        }
        
    }

    private void statement() {
        if(is(TK.ID))
        {
            variableAssigned();
            assign();
        }
        else if(is(TK.PRINT))
        {
            print();
        }
        else if(is(TK.IF))
        {
            depth++;
            currentDepth.put(depth, null);
            x_if();
            depth--;
        }
        else if(is(TK.DO))
        {
            depth++;
            currentDepth.put(depth, null);
            x_do();
            depth--;
        }
        else
        {
            depth++;
            currentDepth.put(depth, null);
            x_fa();
            depth--;
        }
    }
    
    private void assign() {
        translate(); //gen code for ID
        mustbe(TK.ID);
        translate(); //gen code for ASSIGN
        mustbe(TK.ASSIGN);
        expr();
        System.out.println(";"); //create code for end of assignment statement
    }
    
    private void print() {
        inPrint = 1;
        translate(); //gen code is printf("%d\n", 
        mustbe(TK.PRINT);
        expr();
        System.out.println(");"); //creat code for end of print statement
        inPrint = 0;
    }
    
    private void x_if() {
        translate(); //gen code for if
        mustbe(TK.IF);
        guarded_commands();
        translate(); //gen code for fi
        mustbe(TK.FI);
    }
    
    private void x_do() {
        translate(); //gen code for do
        mustbe(TK.DO);
        guarded_commands();
        translate(); //gen code for od
        mustbe(TK.OD);
    }
    
    private void x_fa() {
        expr1 = "";
        expr2 = "";
        
        translate(); //gen code for fa
        mustbe(TK.FA);
        variableAssigned();
        translate(); //gen code for ID of fa
        itr = tok; // hold onto the iterator variable to generator for loop
        mustbe(TK.ID);
        translate(); //gen ASSIGN
        mustbe(TK.ASSIGN);
        
        saveFa1 = 1;
        expr();
        saveFa1 = 0;
        
        translate(); //gen TO
        mustbe(TK.TO);
        
        saveFa2 = 1;
        expr();
        saveFa2 = 0;
        
        System.out.println("; " + "x_" +itr.string + "++" +") {");
        System.out.println("if(" + expr1 + " > " + expr2 + ") {");
        System.out.println("  break;");
        System.out.println("}");
        
        if(is(TK.ST))
        {
            inST = 1;
            translate();// gen ST
            mustbe(TK.ST);
            expr();
            System.out.println(") {");
        }
        
        commands();
        translate(); // gen AF
        mustbe(TK.AF);
    }
    
    private void expr() {
        simple();
        
        if(is(TK.EQ) || is(TK.LT) || is(TK.GT) || is(TK.NE) 
                || is(TK.LE) || is(TK.GE)) 
        {
            translate(); //gen code for operators
            scan();
            simple();
        }
    }
    
    private void simple() {
        term();

        while(is(TK.PLUS) || is(TK.MINUS))
        {
            translate(); //gen code for operators
            scan();
            term();
        }
    }
    
    private void term() {
        factor();

        while(is(TK.TIMES) || is(TK.DIVIDE))
        {
            translate(); //gen code for operators
            scan();
            factor();
        }
    }
    
    private void factor() {
        if(is(TK.LPAREN))
        {
            translate(); //gen code for (
            mustbe(TK.LPAREN);
            expr();
            translate(); //gen code for )
            mustbe(TK.RPAREN);
        }
        else if(is(TK.ID))
        {
            //hold and check current tok to see if declared
            currVar = tok;
            if(!(symTable.containsKey(tok.string)))
            {
                System.err.println("undeclared variable " + tok.string +
                        " on line " + tok.lineNumber);
                System.exit(1);
            }
            variableUsed(tok);
            
            translate(); //gen code for ID
            mustbe(TK.ID);
        }
        else if(is(TK.NUM))
        {
            translate(); //gen code for NUM
            mustbe(TK.NUM);
        }
        else if(is(TK.SQR))
        {
            if(inRoot == 1)
            {
                exprRoot = exprRoot + "(int) pow(";
            }
            else
            {
                System.out.print("(int)");
                System.out.print("pow( ");
            }
            mustbe(TK.SQR);
            sqrCount++;
            expr();
            sqrCount--;
            if(inRoot == 1)
            {
                exprRoot = exprRoot + ",2)";
            }else{
            System.out.print(",2)");
            
            }
        }
        else if(is(TK.ROOT))
        {
            exprRoot = "";
            inRoot = 1;
            System.out.print("(int)");
            System.out.print("sqrt( ");
            mustbe(TK.ROOT);
                   
            expr();
            
            if((inRoot == 1) && (prev.kind != TK.ROOT))
            {
                System.out.print(exprRoot + " < 0 ? 0 : " + exprRoot);
                inRoot = 0;
            }
            System.out.print(" )");
        }
        else
        {
            parse_error("factor");
        }
    }
    
    private void guarded_commands() {
        guarded_command();
        
        while(is(TK.BOX))
        {
            translate(); //gen else if code for BOX
            mustbe(TK.BOX);
            guarded_command();
        }
        
        if(is(TK.ELSE))
        {
            translate(); //gen code for else
            mustbe(TK.ELSE);
            commands();
        }             
        
    }
    
    private void guarded_command() {
        expr();
        commands();
    }
    
    private void commands() {
        translate(); //gen code for ARROW
        mustbe(TK.ARROW);
        block();
    }

    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( ! is(tk) ) {
            System.err.println( "mustbe: want " + tk + ", got " +
                                    tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }

    private void parse_error(String msg) {
        System.err.println( "can't parse: line "
                            + tok.lineNumber + " " + msg );
        System.exit(1);
    }
}
