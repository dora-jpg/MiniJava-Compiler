import visitor.GJDepthFirst;
import syntaxtree.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SecondVisitor extends GJDepthFirst<String,String>{
    SymbolTable symbolTable;
    String currClass = null;
    String currMethod = null;
    Boolean method_statements = false;
    ArrayList<List<String>> expression_list = new ArrayList<List<String>>(); /* expression_list works as a stack. It keeps the lists of expression lists for ExpressionList to allow nested ExpressionLists */

    SecondVisitor(SymbolTable st){
        this.symbolTable = st;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String visit(MainClass n, String argu) throws Exception {

        currClass = n.f1.accept(this, null);
        currMethod = n.f6.toString();
       
        n.f14.accept(this, null);
        method_statements = true;
        n.f15.accept(this, null);
        method_statements = false;

        currMethod = null;
        currClass = null;
        return null;
    }

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n, String argu) throws Exception {
  
        currClass = n.f1.accept(this, null);

        n.f3.accept(this, null);
        n.f4.accept(this, null);
        currClass = null;

        return null;
    }

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {

        currClass = n.f1.accept(this, null);

        n.f5.accept(this, null);
        n.f6.accept(this, null);

        currClass = null;
        return null;
    }

    /**
     * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String ReturnType = n.f1.accept(this, null);
        
        currMethod = n.f2.accept(this, null);
        
        if (!ReturnType.equals("int") && !ReturnType.equals("int[]") && !ReturnType.equals("boolean") && !ReturnType.equals("boolean[]") && !symbolTable.class_exists(ReturnType))
            throw new Exception("Undefined Return Type in method <"+currMethod + "> in class <"+currClass+">.");

        n.f4.accept(this, null);

        n.f7.accept(this, null);
        method_statements = true;
        n.f8.accept(this, null);

        String ReturnExpr = n.f10.accept(this, null);
        if (!ReturnExpr.equals(ReturnType) && !symbolTable.is_superclass(ReturnType, ReturnExpr)) {
            throw new Exception("Return expression of type <"+ ReturnExpr +"> doesn't match actual return type <"+ReturnType+">.");
        }

        method_statements = false;

        currMethod = null;
        return null;
    }

    
    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n, String argu) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, "PrimaryExpression"); /* pass this to Identifier so it knows that it should find the type */
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, String argu) throws Exception {
        /* check calling method. If we are inside a funtion below declarations we need to return the type */
        if (method_statements == true) {
            String type = symbolTable.find_type_in_scope(n.f0.toString(), currMethod, currClass);

            if (type != null && argu!="name") return type; /* argument "name" means that we need to return the name and not the type */
            else if (argu=="AssignmentStatement" || argu=="PrimaryExpression") /* these arguments mean that if we didn't find the type we throw error */
                throw new Exception("Undefined variable <" + n.f0.toString() + "> in <"+argu+"> in method <" + currMethod + "> in class <" + currClass + ">.");
            else if (argu=="Type") /* these arguments mean that if we didn't find the type we throw error */
                throw new Exception("Undefined type <" + n.f0.toString() + "> in method <" + currMethod + "> in class <" + currClass + ">.");
        }
        return n.f0.toString();
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, String argu) throws Exception {
        /* this refers to current object. It is used inside a function, inside a class so current class is its type */
        return currClass;
    }

    /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {

        String index_type = n.f3.accept(this, null);
        if (!index_type.equals("int"))
            throw new Exception("Array size not int.");

        return "boolean[]";
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        
        String index_type = n.f3.accept(this, null);
        if (!index_type.equals("int"))
            throw new Exception("Array size not int.");

        return "int[]";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String argu) throws Exception {
        /* Identifier here is a class */
        String id_name = n.f1.accept(this, null);
        if (!symbolTable.class_exists(id_name)) throw new Exception("Identifier <" + id_name + "> in new Identifier() expression doesn't exist");
        return id_name;
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, null); /* type is f1's type */
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, String argu) throws Exception {
        
        String ex1 = n.f0.accept(this, null);
        String ex2 = n.f2.accept(this, null);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in < operator");

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, String argu) throws Exception {
        String ex1 = n.f0.accept(this, null);
        String ex2 = n.f2.accept(this, null);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in + operator");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, String argu) throws Exception {
        String ex1 = n.f0.accept(this, null);
        String ex2 = n.f2.accept(this, null);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in - operator");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, String argu) throws Exception {
        String ex1 = n.f0.accept(this, null);
        String ex2 = n.f2.accept(this, null);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in * operator");

        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String argu) throws Exception {
        String ReturnType = n.f0.accept(this, null);

        String array_index = n.f2.accept(this, null);
        if (!array_index.equals("int"))
            throw new Exception("Array index not int.");

        if (ReturnType.equals("int[]"))
            return "int";
        else if (ReturnType.equals("boolean[]"))
            return "boolean";
        else 
            throw new Exception("Array Expression not array type.");
    }

    
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String argu) throws Exception {
        
        String type = n.f0.accept(this, null);
        if (!type.equals("int[]") && !type.equals("boolean[]"))
            throw new Exception("Non array objects don't have .length member.");

        return "int";
    }

    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, String argu) throws Exception {
        String type1 = n.f0.accept(this, null);
        String type2 = n.f2.accept(this, null);
        if (!type1.equals("boolean") || !type2.equals("boolean"))
            throw new Exception("Clauses in && operand not boolean.");
        return "boolean";
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, String argu) throws Exception {
        String type = n.f1.accept(this, null);
        if (!type.equals("boolean"))
            throw new Exception("Clause in ! operand not boolean.");
        return "boolean";
    }

    
   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String id_type = n.f0.accept(this, "AssignmentStatement"); /* pass AssignmentStatement to Identifier so it knows that we need to find the type, and if not throw error */
        String expr_type = n.f2.accept(this, null);

        if (id_type.equals(expr_type))
            return null; /* case class_type = class_type and int=int, boolean=boolean, int[]=int[],  boolean[]=boolean[] */
        else if (symbolTable.is_superclass(id_type, expr_type))
            return null; 
        else
            throw new Exception("Invalid types in assignment operator: <"+id_type+"> = <"+expr_type+">.");
        
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String id_type = n.f0.accept(this, null);
        String index_type = n.f2.accept(this, null);
        String expr_type = n.f5.accept(this, null);
        if (!index_type.equals("int"))
            throw new Exception("Index type in array assignment statement is not int.");

        if ((id_type.equals("int[]") && expr_type.equals("int")) 
            || (id_type.equals("boolean[]") && expr_type.equals("boolean")))
            return null;
        else throw new Exception("Incompatible type variables in array assignmet expression.");

    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String argu) throws Exception {

        String expr_type = n.f2.accept(this, null);
        if (!expr_type.equals("boolean"))
            throw new Exception("If condition is not boolean.");

        n.f4.accept(this, null);

        n.f6.accept(this, null);
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String argu) throws Exception {

        String expr_type = n.f2.accept(this, null);
        if (!expr_type.equals("boolean"))
            throw new Exception("While condition is not boolean.");

        n.f4.accept(this, null);
        return null;
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String argu) throws Exception {

        String type = n.f2.accept(this, null);
        if (!type.equals("int"))
            throw new Exception("Print expression only accepts int.");
        return null;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String argu) throws Exception {
        String type = n.f0.accept(this, null);

        String method_name = n.f2.accept(this, "name"); /* pass this to Identifier so it knows we need the name and not the type */

        n.f4.accept(this, null);

        /* expression_list works as a stack */
        List<String> curr_parameters = null;
        if (expression_list.size()-1 <0){ /* if expression list is empty pass an empty list to find_method_type */
            curr_parameters = new LinkedList<String>();
        }
        else { /* else pop the last expression list */
            curr_parameters = expression_list.remove(expression_list.size()-1);
        }

        String return_type = symbolTable.find_method_type(method_name, type, curr_parameters);

        return return_type;
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, String argu) throws Exception {
        List<String> new_list = new ArrayList<>();
        
        String res =n.f0.accept(this, null);

        /* initialize new level of expression list */
        new_list.add(res);
        expression_list.add(new_list);
        
        n.f1.accept(this, null);
        return null;
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, String argu) throws Exception {

        /* add to last level */
        List<String> insert_list = expression_list.get(expression_list.size()-1);
        
        String res =n.f1.accept(this, null);

        insert_list.add(res);
        return null;
    }


    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "boolean";
    }

        /**
    * f0 -> "boolean"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(BooleanArrayType n, String argu) throws Exception {
        return "boolean[]";
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(IntegerArrayType n, String argu) throws Exception {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, String argu) throws Exception { /* in case of type Identifier */
        return n.f0.accept(this, "Type"); /* pass Type to Identifier so it knows that it needs to evaluate the type or else throw error */
    }
}
