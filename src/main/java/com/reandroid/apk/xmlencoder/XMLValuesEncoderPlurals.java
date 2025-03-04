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
package com.reandroid.apk.xmlencoder;

import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.coder.EncodeResult;
import com.reandroid.arsc.coder.ValueDecoder;
import com.reandroid.arsc.value.AttributeType;
import com.reandroid.arsc.value.ResTableMapEntry;
import com.reandroid.arsc.value.ResValueMap;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.xml.XMLElement;

class XMLValuesEncoderPlurals extends XMLValuesEncoderBag{
    XMLValuesEncoderPlurals(EncodeMaterials materials) {
        super(materials);
    }
    @Override
    protected void encodeChildes(XMLElement parentElement, ResTableMapEntry resValueBag){
        int count = parentElement.getChildesCount();
        ResValueMapArray itemArray = resValueBag.getValue();
        EncodeMaterials materials = getMaterials();
        for(int i=0;i<count;i++){
            XMLElement child = parentElement.getChildAt(i);
            ResValueMap bagItem = itemArray.get(i);
            AttributeType quantity = AttributeType
                    .fromName(child.getAttributeValue("quantity"));
            if(quantity == null){
                throw new EncodeException("Unknown plurals quantity: "
                        + child.toText());
            }

            bagItem.setName(quantity.getId());

            String valueText = child.getTextContent();
            EncodeResult encodeResult = materials.encodeReference(valueText);
            if(encodeResult != null){
                bagItem.setTypeAndData(encodeResult.valueType, encodeResult.value);
                continue;
            }
            bagItem.setValueAsString(ValueDecoder
                    .unEscapeUnQuote(valueText));
        }
    }
}
