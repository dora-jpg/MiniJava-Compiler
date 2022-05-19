import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

public class SymbolTable {
    
    /* class returns ClassInfo */
    Map<String, ClassInfo> class_dec = new HashMap<String, ClassInfo>();
    
    /* field->class returns VariableInfo  */
    Map<String, Map<String, VariableInfo>> field_in_class = new HashMap<String, Map<String, VariableInfo>>();
    
    /* method->class returns MethodInfo */
    Map<String, Map<String, MethodInfo>> method_in_class = new HashMap<String, Map<String, MethodInfo>>();
    
    /* variable->method->class returns VariableInfo*/ 
    Map<String, Map<String, Map<String, VariableInfo>>> var_in_method_in_class = new HashMap<String,Map<String,Map<String, VariableInfo>>>();

    
    public void addClassDeclaration(String ClassName) throws Exception {
        if (class_dec.containsKey(ClassName)){
            throw new Exception("Class <"+ ClassName + "> already defined");
        }
        else {
            ClassInfo new_class = new ClassInfo(ClassName, null);
            class_dec.put(ClassName, new_class);
        }
    }

    public void addClassDeclaration(String ClassName, String SuperName) throws Exception { 
        /* SuperName can be null if class does not inherit from another class
         * If SuperName doesn't exist in class declarations at the time we call this function, it is invalid because the superclass must be declared first
         */
        if (SuperName == null){
            addClassDeclaration(ClassName);
        }
        else if (class_dec.containsKey(ClassName)){
            throw new Exception("Class <"+ ClassName + "> already defined");
        }
        else if (!class_dec.containsKey(SuperName)){
            throw new Exception("The superclass <"+ SuperName + "> that class <" + ClassName + "> inherits from is not defined");
        } 
        else {
            ClassInfo new_class = new ClassInfo(ClassName, class_dec.get(SuperName));
            class_dec.put(ClassName, new_class);
        }
    }

    public void addClassField(String FieldName, String ClassName, String Type) throws Exception { /* */
        Map<String, VariableInfo> classes_map = field_in_class.get(FieldName);
        if (classes_map == null){
            classes_map = new HashMap<String, VariableInfo>();
        }
        else if (classes_map.containsKey(ClassName)){
            throw new Exception("Field <" + FieldName + "> already declared in class <" + ClassName + ">");
        }
        VariableInfo new_var = new VariableInfo(FieldName, Type, null, ClassName);
        classes_map.put(ClassName, new_var);
        field_in_class.put(FieldName, classes_map);
    }

    public void addClassMethod(String MethodName, String ClassName, List<String> arg_names, List<String> arg_types, String ReturnType) throws Exception {
        
        /* check for superclass override */
        /* repeat until you find a superclass with the same method name or null */
        MethodInfo curr_method_info = new MethodInfo(MethodName, ClassName, arg_names, arg_types, ReturnType);
        ClassInfo curr_class = class_dec.get(ClassName).getSuper();
        while (curr_class != null){

            if (method_in_class.get(MethodName) == null ) break;
            MethodInfo temp =  method_in_class.get(MethodName).get(curr_class.name());
            if (temp != null){
                if (!curr_method_info.equals(temp)){
                    throw new Exception("Invalid method override in method <" + MethodName + "> in class <" + ClassName + ">. Previous definition was in class <" + curr_class.name() +">.");
                }
                break;
            }
            curr_class = curr_class.getSuper();
        }
        
        Map<String, MethodInfo> get_class_map = method_in_class.get(MethodName);
        if (get_class_map != null){
            MethodInfo method_info = get_class_map.get(ClassName);
            if (method_info != null) {
                throw new Exception("Method <" + MethodName + "> already declared in class <" + ClassName + ">");
            }
            else {
                get_class_map.put(ClassName, curr_method_info);
            }
        }
        else {
            get_class_map = new HashMap<String, MethodInfo>();
            get_class_map.put(ClassName, curr_method_info);
            method_in_class.put(MethodName, get_class_map);
        }

        /* now add all arguments in the variable_in_method_in_class */
        Iterator<String> names = arg_names.iterator();
        Iterator<String> types = arg_types.iterator();
        while (names.hasNext() && types.hasNext()) {
            this.addMethodVariable(names.next(), MethodName, ClassName, types.next());
        }

    }

    public void addMethodVariable(String VariableName, String MethodName, String ClassName, String Type) throws Exception { /* method should already exist in method_in_class map */
    
        VariableInfo to_insert = new VariableInfo(VariableName, Type, MethodName, ClassName);

        Map<String, MethodInfo> declared_methods_map_class = method_in_class.get(MethodName);
        MethodInfo method = declared_methods_map_class.get(ClassName);

        /* check if method doesn't exist in methods' map: THIS SHOULD NOT HAPPEN */
        if (method == null) {
            throw new Exception("Method <" + MethodName + "> at <" + ClassName + "> not declared when trying to insert <" + VariableName + "> into Symbol Table");
        }
        
        /* first search by VariableName in map */
        Map<String, Map<String, VariableInfo>> first= var_in_method_in_class.get(VariableName);
        if (first != null) {
            /* then by method */
            Map<String, VariableInfo> second = first.get(MethodName);
            if (second !=  null){
                /* at last by class */
                VariableInfo third = second.get(ClassName);
                if (third != null){
                    throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> in class <" + ClassName + ">");
                }
                else {
                    second.put(ClassName, to_insert);
                    first.put(MethodName, second);
                    var_in_method_in_class.put(VariableName, first);
                }
            }
            else { 
                /* create second and put */
                second = new HashMap<String, VariableInfo>();
                second.put(ClassName, to_insert);
                first.put(MethodName, second);
                var_in_method_in_class.put(VariableName, first);
            }
        }
        else {
            /* create first and second and put */
            first = new HashMap<String, Map<String, VariableInfo>>();
            Map<String, VariableInfo> second = new HashMap<String, VariableInfo>();
            second.put(ClassName, to_insert);
            first.put(MethodName, second);
            var_in_method_in_class.put(VariableName, first);
        }
    
    }

    /* find methods */

    /* returns type of an identifier */
    String find_type_in_scope(String VariableName, String MethodName, String ClassName) throws Exception{
        
        /* variable in a scope (MethodName, ClassName) can be declared inside the Method, as argument 
        to the Method, as Field in Class or a Field in superclass */

        /* first search the nearest scope: variable or argument in a method */
        VariableInfo variable = null;
        Map<String, Map<String, VariableInfo>> by_var = var_in_method_in_class.get(VariableName);
        if (by_var != null) {
            Map<String, VariableInfo> by_method = by_var.get(MethodName);
            if (by_method!=null) {
                variable = by_method.get(ClassName);
                // System.out.println("found variable in scope searching for "+VariableName+" "+MethodName+" "+ClassName);
                if (variable != null) return variable.getType();
            }
        }

        /* search at class or superclasses */
        /* get the map of classes that belongs to our variable name. In that map we will search for the closest superclass each time */
        Map<String, VariableInfo> map_of_classes = field_in_class.get(VariableName);
        if (map_of_classes == null) return null; /* case variable was not found */
        
        VariableInfo temp;
        ClassInfo curr_class = class_dec.get(ClassName);
        while (curr_class != null){
            temp = map_of_classes.get(curr_class.name());
            
            if (temp!=null) {
                return temp.getType();
            }
            
            curr_class = curr_class.getSuper();
        }

        return null; /* case variable was not found */
    }

    public String find_method_type(String MethodName, String ClassName, List<String> parameters) throws Exception{
        Map<String, MethodInfo> by_method = method_in_class.get(MethodName);
        if (by_method==null) 
            throw new Exception("Reference to undefined method <" + MethodName + ">");
                
        MethodInfo temp;
        ClassInfo curr_class = class_dec.get(ClassName);
        while (curr_class != null){
            temp = by_method.get(curr_class.name());
            
            if (temp!=null) {
                /* check if method parameter expressions are the correct type */
                if (temp.check_types(parameters, this) == false) 
                    throw new Exception("Parameter types not compatible with argument types in method "+ClassName+"."+MethodName+". Method is inherited from class <" + curr_class.name() + ">.");
                return temp.getReturnType();
            }
            
            curr_class = curr_class.getSuper();
        }

        /* if not found */
        throw new Exception("Method <"+MethodName+"> doesn't exist for type <"+ ClassName + ">");
    }

    public List<String> return_method_info(String MethodName, String ClassName){
        Map<String, MethodInfo> by_method = method_in_class.get(MethodName);
                
        MethodInfo temp;
        ClassInfo curr_class = class_dec.get(ClassName);
        while (curr_class != null){
            temp = by_method.get(curr_class.name());
            
            if (temp!=null) {
                /* check if method parameter expressions are the correct type */
                List<String> ret = temp.getArgTypes();
                ret.add(0, temp.getReturnType());
                return ret;
            }
            
            curr_class = curr_class.getSuper();
        }
        return null;
    }

    public boolean class_exists(String ClassName){
        return class_dec.containsKey(ClassName);
    }

    public boolean is_superclass(String id_type, String expr_type){

        ClassInfo curr_class = class_dec.get(expr_type);
        while (curr_class != null){
            
            if (curr_class.name().equals(id_type)) {
                return true;
            }            
            curr_class = curr_class.getSuper();
        }
        return false;
    }

}

class MethodInfo { /* holds all information for the method */
    String ClassName;
    String MethodName;
    String ReturnType;
    List<String> arg_names;
    List<String> arg_types;

    MethodInfo(String MethodName, String ClassName, List<String> arg_names, List<String> arg_types, String ReturnType){ /* proper initialization */
        this.ClassName = ClassName;
        this.MethodName = MethodName;
        this.arg_names = new LinkedList<>(arg_names); /* copy */
        this.arg_types = new LinkedList<>(arg_types); /* copy */
        this.ReturnType = ReturnType;
    }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;

        return MethodName.equals(that.MethodName) &&
        ReturnType.equals(that.ReturnType) &&
        arg_types.equals(that.arg_types); /* 2 methods are considered equals based on types */
    }

    public boolean hasArgName(String name) {
        return arg_names.contains(name);
    }

    public String getReturnType(){
        return ReturnType;
    }

    public List<String> getArgTypes(){
        return arg_types;
    }

    public boolean check_types(List<String> parameters_types, SymbolTable sTable) {
        if (parameters_types.size()!=arg_types.size()) return false;

        Iterator<String> parameters = parameters_types.iterator();
        Iterator<String> args = arg_types.iterator();

        while (parameters.hasNext() && args.hasNext()) {
            String arg = args.next();
            String param = parameters.next();
            if (!arg.equals(param)) /* for basic types */
                if (!sTable.is_superclass(arg, param)) return false;
        }

        return true;
    }
}

class ClassInfo {
    String name;
    ClassInfo superclass;
    Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();

    ClassInfo(String name, ClassInfo superclass){
        this.name = name;
        this.superclass = superclass;
    }

    public void addMethod(String name,MethodInfo method){
        methods.put(name, method);
    }

    public MethodInfo getMethod(String name){
        return methods.get(name);
    }
    
    public ClassInfo getSuper(){
        return superclass;
    }

    public String name(){
        return name;
    }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassInfo that = (ClassInfo) o;
        
        return name.equals(that.name);
    }
}

class VariableInfo {
    String name;
    String type;
    String method;
    String classname;

    VariableInfo(String name, String type, String method, String classname){
        this.name = name;
        this.type = type;
        this.method = method;
        this.classname = classname;
    }

    public String getType(){
        return type;
    }

    public String getMethod(){
        return method;
    }

    public String getClassname(){
        return method;
    }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableInfo that = (VariableInfo) o;
        
        return name.equals(that.name) && classname.equals(that.classname);
    }
}