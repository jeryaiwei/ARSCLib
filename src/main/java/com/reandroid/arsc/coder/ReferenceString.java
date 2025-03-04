/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.arsc.coder;

import com.reandroid.arsc.value.ValueType;

public class ReferenceString {
    public final String prefix;
    public final String packageName;
    public final String type;
    public final String name;

    public ReferenceString(String prefix, String packageName, String type, String name) {
        this.prefix = prefix;
        this.packageName = packageName;
        this.type = type;
        this.name = name;
    }
    public ValueType getValueType(){
        if("?".equals(prefix)){
            return ValueType.ATTRIBUTE;
        }
        return ValueType.REFERENCE;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (prefix != null) {
            builder.append(prefix);
        }
        if (packageName != null) {
            builder.append(packageName);
            builder.append(':');
        }
        if (type != null) {
            builder.append(type);
            builder.append('/');
        }
        builder.append(name);
        return builder.toString();
    }


    public static ReferenceString parseReference(String ref){
        if(ref == null || ref.length() < 2 || ref.indexOf('/') < 0 || ref.indexOf(' ') > 0){
            return null;
        }
        char first = ref.charAt(0);
        if(first != '@' && first != '?'){
            return null;
        }
        String prefix;
        int i = 1;
        if(ref.charAt(1) == '+'){
            i = 2;
        }
        prefix = ref.substring(0, i);
        ref = ref.substring(i);
        String packageName = null;
        i = ref.indexOf(':');
        if(i > 0){
            packageName = ref.substring(0, i);
            i++;
            ref = ref.substring(i);
        }
        i = ref.indexOf('/');
        if(i < 0){
            return null;
        }
        String type = ref.substring(0, i);
        i++;
        String name = ref.substring(i);
        if(!isValidResourceName(name)){
            return null;
        }
        return new ReferenceString(prefix, packageName, type, name);
    }
    private static boolean isValidResourceName(String text){
        char[] chars = text.toCharArray();
        int length = chars.length;
        if(length == 0){
            return false;
        }
        for(int i = 0; i < length; i++){
            if(!isValidResourceName(chars[i])){
                return false;
            }
        }
        return true;
    }
    private static boolean isValidResourceName(char ch){
        switch (ch){
            case ':':
            case '/':
            case '@':
            case '?':
            case '"':
            case '<':
            case '>':
            case '+':
            case '*':
                return false;
            default:
                return true;
        }
    }
}
