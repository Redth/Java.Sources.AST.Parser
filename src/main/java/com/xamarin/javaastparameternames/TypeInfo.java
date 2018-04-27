package com.xamarin.javaastparameternames;

import java.util.ArrayList;
import java.util.List;

public class TypeInfo {
    public String Name;
    public Boolean IsInterface;
    public List<MemberInfo> Members = new ArrayList<>();

    TypeInfo(String typeName, Boolean isInterface) {
        Name = typeName;
        IsInterface = isInterface;
    }
}
