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
package com.reandroid.arsc.value;

public enum ValueType {

    NULL((byte) 0x00, ""),
    REFERENCE((byte) 0x01, "reference"),
    ATTRIBUTE((byte) 0x02, "reference"),
    FLOAT((byte) 0x04, "float"),
    DIMENSION((byte) 0x05, "dimension"),
    FRACTION((byte) 0x06, "fraction"),
    DEC((byte) 0x10, "integer"),
    HEX((byte) 0x11, "integer"),
    BOOLEAN((byte) 0x12, "bool"),
    COLOR_ARGB8((byte) 0x1c, "color"),
    COLOR_RGB8((byte) 0x1d, "color"),
    COLOR_ARGB4((byte) 0x1e, "color"),
    COLOR_RGB4((byte) 0x1f, "color"),
    STRING((byte) 0x03, "string"),
    DYNAMIC_REFERENCE((byte) 0x07, "reference"),
    DYNAMIC_ATTRIBUTE((byte) 0x08, "reference");

    private final byte mByte;
    private final String typeName;
    ValueType(byte b, String typeName) {
        this.mByte = b;
        this.typeName = typeName;
    }
    public byte getByte(){
        return mByte;
    }
    public String getTypeName() {
        return typeName;
    }
    public boolean isColor(){
        return this == COLOR_ARGB8
                || this == COLOR_RGB8
                || this == COLOR_ARGB4
                || this == COLOR_RGB4;
    }
    public boolean isInteger(){
        return this == DEC
                || this == HEX;
    }
    public boolean isReference(){
        return this == REFERENCE
                || this == ATTRIBUTE
                || this == DYNAMIC_REFERENCE
                || this == DYNAMIC_ATTRIBUTE;
    }

    public static ValueType valueOf(byte b){
        ValueType[] all=values();
        for(ValueType vt:all){
            if(vt.mByte==b){
                return vt;
            }
        }
        return null;
    }
    public static ValueType fromName(String name){
        if(name==null){
            return null;
        }
        name=name.toUpperCase();
        ValueType[] all=values();
        for(ValueType vt:all){
            if(name.equals(vt.name())){
                return vt;
            }
        }
        return null;
    }
}
