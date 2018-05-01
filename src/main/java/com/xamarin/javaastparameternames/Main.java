package com.xamarin.javaastparameternames;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
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
        final String helpText = "JavaASTParameterNames.jar [--verbose] [--xml|--text] java1-sources.jar java2-sources.jar ... output.txt|output.xml";
        
        List<String> files = new ArrayList<>();
        
        boolean xml = false;
        boolean verbose = false;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if ("--xml".equals(arg) || "-x".equals(arg)) {
                xml = true;
            } else if ("--text".equals(arg) || "-t".equals(arg)) {
                xml = false;  
            } else if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else {
                files.add(arg);
            }
        }
        
//        files.add("/Users/redth/Desktop/android-support-new/externals/com.android.support/recyclerview-v7-sources.jar");
//        files.add("/Users/redth/Desktop/out.xml");
//        xml = true;
        
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
        
        if (verbose)
            PrintConsole (data);

        if (xml)
            PrintTransformMetadataXmlFile(data, outputFile);
        else
            PrintParameterNameTextFile(data, outputFile);
            
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
    
    static void PrintParameterNameTextFile(Map<String, Map<String, TypeInfo>> info, String outputFile) throws IOException
    {
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
    
    static void PrintTransformMetadataXmlFile (Map<String, Map<String, TypeInfo>> info, String outputFile) throws IOException
    {
        java.io.File outFile = new java.io.File(outputFile);
        if (outFile.exists())
            outFile.delete();
        
        BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
 
        out.write("<metadata>");
        out.newLine();
        
        for (String pkgName : info.keySet()) {
            
            Map<String, TypeInfo> types = info.get(pkgName);
            
            for (String typeName : types.keySet()) {
                
                TypeInfo typeInfo = types.get(typeName);
                
                for (MemberInfo memberInfo : typeInfo.Members) {
                    
                    int paramCount = memberInfo.Parameters.size();
                    
                    for (int i = 0; i < memberInfo.Parameters.size (); i++) {
                        ParamInfo paramInfo = memberInfo.Parameters.get(i);
                    
                        out.write("<attr path=\"/api/package[@name='");
                        out.write(pkgName);
                        out.write("']/");

                        if (typeInfo.IsInterface)
                            out.write("interface");
                        else
                            out.write("class");

                        out.write("[@name='");
                        out.write(typeName);
                        out.write("']/method[@name='");
                        out.write(memberInfo.Name);
                        out.write("'");

                        out.write(" and count(parameter)=");
                        out.write(paramCount);

                        // constructors need special name
                        
                         for (int n = 0; n < memberInfo.Parameters.size (); n++) {
                            ParamInfo innerParamInfo = memberInfo.Parameters.get(n);
                            out.write(" and parameter[");
                            out.write(Integer.toString(n + 1));
                            out.write("][contains(@type, '");
                            
                            String ipName = innerParamInfo.TypeName.replace("<", "&lt;").replace(">", "&gt;");
                            out.write(ipName);
                            out.write("')]");
                         }
                         
                         out.write("]/parameter[");
                         out.write(Integer.toString(i + 1));
                         out.write("]\" name=\"managedName\">");
                         out.write(paramInfo.Name);
                         out.write("</attr>");
                         out.newLine();
                    }
                }
                
                out.flush();
            }
            
            out.flush();
        }
        
        out.write("</metadata>");
        out.newLine();
        out.flush();
        
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
            
            ClassOrInterfaceDeclaration classOrIface = (ClassOrInterfaceDeclaration)type;
            
            CheckType(null, classOrIface, pkgInfo);
        }
        
        info.put(pkgName, pkgInfo);
    } 
    
    static void CheckType(ClassOrInterfaceDeclaration parentType, ClassOrInterfaceDeclaration type, Map<String, TypeInfo> pkgInfo)
    {
        Boolean isInterface = ((ClassOrInterfaceDeclaration) type).isInterface();

        // Get our type's name
        String typeName = type.getNameAsString();
        
        if (parentType != null)
            typeName = parentType.getNameAsString() + "." + typeName;

        TypeInfo typeInfo = pkgInfo.getOrDefault(typeName, new TypeInfo (typeName, isInterface));

        // Go through all fields, methods, etc. in this type
        NodeList<BodyDeclaration<?>> members = type.getMembers();
        for (BodyDeclaration<?> member : members) {

            // Storage for parameters and member name
            NodeList<Parameter> parameters = new NodeList<>();

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
            }
            else if (member instanceof ClassOrInterfaceDeclaration)
            {
                CheckType(type, (ClassOrInterfaceDeclaration)member, pkgInfo);
                continue;
            }
            else
            {
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
}
