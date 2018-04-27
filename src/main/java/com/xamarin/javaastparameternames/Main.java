/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xamarin.javaastparameternames;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author redth
 */
public class Main {
    public static void main(String[] args) throws Exception {
        final String helpText = "JavaASTParameterNames.jar java1-sources.jar java2-sources.jar ... output.txt";
        
        List<String> files = new ArrayList<>();
        files.addAll(Arrays.asList(args));
        
        // Make sure there's at least one input, and one output file
        if (files.size() < 2) {
            System.out.println (helpText);
            return;
        }
        
        int lastIndex = files.size() - 1;
        String outputFile = files.get(lastIndex);
        files.remove(lastIndex);
        
        // Check the last file is not a .jar so we aren't going to overwrite a jar with output
        if (outputFile.endsWith("jar")) {
            System.out.println (helpText);
            return;
        }
        
        // Check that all the input files exist
        for (String filename : files)  {
            java.io.File file = new java.io.File(filename);
            
            if (!file.exists()) {
                System.out.println (helpText);
                return;
            }
        }

        Map<String, Map<String, TypeInfo>> data = new HashMap<>();
        
        for (String filename : files) {
            ZipFile zip = new ZipFile(filename);

            // Inspect the .jar file for 
            for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (!entry.isDirectory()) {
                    // Find .java files
                    if (entry.getName().endsWith("java")) {
                        InputStream fileStream = zip.getInputStream(entry);
                        ParseJavaStream (fileStream, data);
                    }
                }
            }   
        }
        
        //PrintConsole (data);
        PrintParameterNameTextFile (data, outputFile);
    }
    
    static void PrintConsole (Map<String, Map<String, TypeInfo>> info)
    {
        for (String pkgName : info.keySet()) {
            
            System.out.println(pkgName);
            
            Map<String, TypeInfo> types = info.get(pkgName);
            
            for (String typeName : types.keySet()) {
                
                TypeInfo typeInfo = types.get(typeName);
            
                System.out.println ("  " + typeName);
                
                for (MemberInfo memberInfo : typeInfo.Members) {
                    System.out.print("    " + memberInfo.Name + " (");
                    
                    for (int i = 0; i < memberInfo.Parameters.size (); i++) {
                        ParamInfo paramInfo = memberInfo.Parameters.get(i);
                        
                        System.out.print (paramInfo.TypeName + " " + paramInfo.Name);
                        
                        if (i < (memberInfo.Parameters.size() - 1))
                            System.out.print (", ");
                    }
                    
                    System.out.println(")");
                }
            }
        }
    }
    
    static void PrintParameterNameTextFile (Map<String, Map<String, TypeInfo>> info, String outputFile) throws IOException
    {
        /*
        * The Text Format is:
        * 
        * package {packagename}
        * ;---------------------------------------
        *   interface {interfacename}{optional_type_parameters} -or-
        *   class {classname}{optional_type_parameters}
        *     {optional_type_parameters}{methodname}({parameters})
        * 
        * Anything after ; is treated as comment.
        * 
        * optional_type_parameters: "" -or- "<A,B,C>" (no constraints allowed)
        * parameters: type1 p0, type2 p1 (pairs of {type} {name}, joined by ", ")
        * 
        * It is with strict indentations. two spaces for types, four spaces for methods.
        * 
        * Constructors are named as "#ctor".
        * 
        * Commas are used by both parameter types and parameter separators,
        * but only parameter separators can be followed by a whitespace.
        * It is useful when writing text parsers for this format.
        * 
        * Type names may contain whitespaces in case it is with generic constraints (e.g. "? extends FooBar"),
        * so when parsing a parameter type-name pair, the only trustworthy whitespace for tokenizing name is the *last* one.
        * 
        */
        
        java.io.File outFile = new java.io.File(outputFile);
        if (outFile.exists())
            outFile.delete();
        
        BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
 
        for (String pkgName : info.keySet()) {
            
            out.write("package " + pkgName);
            out.newLine();
            out.write(";-------------------------------");
            out.newLine();
            
            Map<String, TypeInfo> types = info.get(pkgName);
            
            for (String typeName : types.keySet()) {
                
                //   interface|class {typename}
                TypeInfo typeInfo = types.get(typeName);
                if (typeInfo.IsInterface) {
                    out.write("  interface ");
                } else {
                    out.write("  class ");
                }
                out.write(typeName);
                out.newLine();
                
                for (MemberInfo memberInfo : typeInfo.Members) {
                    // constructors need special name
                    if (memberInfo.Name.equals(typeName)) {
                        out.write("    " + "#ctor(");
                    } else {
                        out.write("    " + memberInfo.Name + "(");
                    }   

                    for (int i = 0; i < memberInfo.Parameters.size (); i++) {
                        ParamInfo paramInfo = memberInfo.Parameters.get(i);
                        
                        out.write (paramInfo.TypeName + " " + paramInfo.Name);
                        
                        if (i < (memberInfo.Parameters.size() - 1))
                            out.write (", ");
                    }
                    
                    out.write (")");
                    out.newLine();
                }
            }
            
            out.flush();
        }
        
        out.close();
    }
    
    static void ParseJavaStream (InputStream stream,  Map<String, Map<String, TypeInfo>> info)
    {
        CompilationUnit cu = JavaParser.parse(stream);

        // Get the package name
        String pkgName = cu.getPackageDeclaration().get().getNameAsString();
        
        Map<String, TypeInfo> pkgInfo = info.getOrDefault(pkgName, new HashMap<>());

        // Go through all the types in the file
        NodeList<TypeDeclaration<?>> types = cu.getTypes();
        for (TypeDeclaration<?> type : types) {

            if (!(type instanceof ClassOrInterfaceDeclaration))
                continue;
            
            Boolean isInterface = ((ClassOrInterfaceDeclaration) type).isInterface();
                    
            // Get our type's name
            String typeName = type.getNameAsString();
            TypeInfo typeInfo = pkgInfo.getOrDefault(typeName, new TypeInfo (typeName, isInterface));
            
            // Go through all fields, methods, etc. in this type
            NodeList<BodyDeclaration<?>> members = type.getMembers();
            for (BodyDeclaration<?> member : members) {
                
                // Storage for parameters and member name
                NodeList<Parameter> parameters;
                
                MemberInfo memberInfo = new MemberInfo ();
                            
                // Check and see if we found a method declaration or ctor declaration
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    memberInfo.Name = method.getName().asString();
                    parameters = method.getParameters();
                } else if (member instanceof ConstructorDeclaration) {
                    ConstructorDeclaration ctor = (ConstructorDeclaration) member;
                    memberInfo.Name = ctor.getName().asString();
                    parameters = ctor.getParameters();
                } else {
                    continue; // Otherwise skip this member
                }
                
                for (Parameter param : parameters) {
                    // Get the parameter type's name
                    ParamInfo paramInfo = new ParamInfo ();
                    paramInfo.Name = param.getName().asString();
                    paramInfo.TypeName = param.getType().asString();
                    
                    memberInfo.Parameters.add(paramInfo);  
                }
                
                typeInfo.Members.add(memberInfo);
            }
            
            pkgInfo.put(typeName, typeInfo);
        }
        
        info.put(pkgName, pkgInfo);
    } 
}
