/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xamarin.javaastparameternames;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author redth
 */
public class TypeInfo {
    public String Name;
    public Boolean IsInterface;
    public List<MemberInfo> Members = new ArrayList<>();

    TypeInfo(String typeName, Boolean isInterface) {
        Name = typeName;
        IsInterface = isInterface;
    }
}
