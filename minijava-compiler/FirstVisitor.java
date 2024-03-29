import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.List;
import java.util.LinkedList;

/* FisrtVisitor fills the symbol table (catches double decalaration errors, type mismatch for overriding) */

public class FirstVisitor extends GJDepthFirst<String,Void> {
    SymbolTable symbolTable = new SymbolTable();
    String currClass = null;
    String currMethod = null;
    List<String> helperListArgs = new LinkedList<String>();
    List<String> helperListTypes = new LinkedList<String>();
    
    public Offset offset = new Offset();
   
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
    public String visit(MainClass n, Void argu) throws Exception {

        String classname = n.f1.accept(this, argu);
        currClass = classname;
        symbolTable.addClassDeclaration(classname);
        offset.add_main(classname);
        String methodname = n.f6.toString();
        currMethod = methodname;
        
        List<String> args = new LinkedList<String>();
        args.add(n.f11.accept(this, argu));
        
        List<String> types = new LinkedList<String>();
        types.add("String[]");
        
        symbolTable.addClassMethod(methodname, classname, args, types, "static void"); /* return type doesn't really matter, 
                                                        static method cannot be overriden so set "static void" so it doen't match with 
                                                        the subclasses and always return error when a program overrides it */
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);

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
    public String visit(ClassDeclaration n, Void argu) throws Exception {
  
        n.f0.accept(this, argu);
        String className = n.f1.accept(this, argu);
        currClass = className;
        symbolTable.addClassDeclaration(className);
        offset.add_class(className, null);

        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
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
   public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {

        String className = n.f1.accept(this, argu);
        currClass = className;
        String superName = n.f3.accept(this, argu);
        symbolTable.addClassDeclaration(className, superName);
        offset.add_class(className, superName);

        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
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
    public String visit(MethodDeclaration n, Void argu) throws Exception {

        String retType = n.f1.accept(this, argu);
        String methodName = n.f2.accept(this, argu);
        currMethod = methodName;
        offset.add_method(methodName);

        n.f4.accept(this, argu);
        
        symbolTable.addClassMethod(methodName, currClass, helperListArgs, helperListTypes, retType);
        helperListArgs.clear();
        helperListTypes.clear();

        n.f7.accept(this, argu);
        n.f8.accept(this, argu);

        n.f10.accept(this, argu);
        currMethod = null;
        return null;
    }
   
    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, Void argu) throws Exception {
        helperListTypes.add(n.f0.accept(this, argu));
        helperListArgs.add(n.f1.accept(this, argu));
        return null;
     }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, Void argu) throws Exception {

        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        
        if (currMethod==null){
            symbolTable.addClassField(name, currClass, type);
            offset.add_field(name, type);
        }
        else {
            symbolTable.addMethodVariable(name, currMethod, currClass, type);
        }

        return null;
    }
 
    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, Void argu) throws Exception {
        return n.f0.toString();
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
    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, Void argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> "boolean"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(BooleanArrayType n, Void argu) throws Exception {
        return "boolean[]";
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(IntegerArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n, Void argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n, Void argu) throws Exception {
        return n.f0.toString();
    }

}
