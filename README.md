# Java.Sources.AST.Parser
A java based -sources.jar AST parser to output parameter name information

The purpose for this tool is to take java *-sources.jar files, and parse their .java code files into an abstract syntax tree so we can extrac the parameter names for methods.

These parameter names are useful when binding Xamarin.Android projects.

## Usage:

```
java -jar JavaASTParameterNames.jar "input-sources.jar" "output.txt"
```

## Xamarin.Android bindings

To use the output file in your Xamarin.Android binding project, include it in your project:

```
<ItemGroup>
  <_AndroidDocumentationPath Include="output.txt" />
</ItemGroup>
```