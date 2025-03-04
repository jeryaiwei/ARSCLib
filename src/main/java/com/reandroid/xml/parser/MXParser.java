/*
 *  This class is taken from org.xmlpull.*
 *
 *  Check license: http://xmlpull.org
 *
 */

/*This package is renamed from org.xmlpull.* to avoid conflicts*/
package com.reandroid.xml.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class MXParser implements XmlPullParser
{
    protected final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
    protected final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    protected final static String FEATURE_XML_ROUNDTRIP=
        //"http://xmlpull.org/v1/doc/features.html#xml-roundtrip";
        "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";
    protected final static String FEATURE_NAMES_INTERNED =
        "http://xmlpull.org/v1/doc/features.html#names-interned";
    protected final static String PROPERTY_XMLDECL_VERSION =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
    protected final static String PROPERTY_XMLDECL_STANDALONE =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";
    protected final static String PROPERTY_XMLDECL_CONTENT =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-content";
    protected final static String PROPERTY_LOCATION =
        "http://xmlpull.org/v1/doc/properties.html#location";

    protected boolean allStringsInterned;

    protected void resetStringCache() {
    }

    protected String newString(char[] cbuf, int off, int len) {
        return new String(cbuf, off, len);
    }

    protected String newStringIntern(char[] cbuf, int off, int len) {
        return (new String(cbuf, off, len)).intern();
    }

    private static final boolean TRACE_SIZING = false;

    protected boolean processNamespaces;
    protected boolean roundtripSupported;

    protected String location;
    protected int lineNumber;
    protected int columnNumber;
    protected boolean seenRoot;
    protected boolean reachedEnd;
    protected int eventType;
    protected boolean emptyElementTag;

    protected int depth;
    protected char[][] elRawName;
    protected int[] elRawNameEnd;
    protected int[] elRawNameLine;

    protected String[] elName;
    protected String[] elPrefix;
    protected String[] elUri;
    //protected String elValue[];
    protected int[] elNamespaceCount;

    protected void ensureElementsCapacity() {
        final int elStackSize = elName != null ? elName.length : 0;
        if( (depth + 1) >= elStackSize) {
            // we add at least one extra slot ...
            final int newSize = (depth >= 7 ? 2 * depth : 8) + 2; // = lucky 7 + 1 //25
            final boolean needsCopying = elStackSize > 0;
            String[] arr = null;
            arr = new String[newSize];
            if(needsCopying) System.arraycopy(elName, 0, arr, 0, elStackSize);
            elName = arr;
            arr = new String[newSize];
            if(needsCopying) System.arraycopy(elPrefix, 0, arr, 0, elStackSize);
            elPrefix = arr;
            arr = new String[newSize];
            if(needsCopying) System.arraycopy(elUri, 0, arr, 0, elStackSize);
            elUri = arr;

            int[] iarr = new int[newSize];
            if(needsCopying) {
                System.arraycopy(elNamespaceCount, 0, iarr, 0, elStackSize);
            } else {
                iarr[0] = 0;
            }
            elNamespaceCount = iarr;
            iarr = new int[newSize];
            if(needsCopying) {
                System.arraycopy(elRawNameEnd, 0, iarr, 0, elStackSize);
            }
            elRawNameEnd = iarr;

            iarr = new int[newSize];
            if(needsCopying) {
                System.arraycopy(elRawNameLine, 0, iarr, 0, elStackSize);
            }
            elRawNameLine = iarr;

            final char[][] carr = new char[newSize][];
            if(needsCopying) {
                System.arraycopy(elRawName, 0, carr, 0, elStackSize);
            }
            elRawName = carr;
        }
    }
    protected int attributeCount;
    protected String[] attributeName;
    protected int[] attributeNameHash;
    protected String[] attributePrefix;
    protected String[] attributeUri;
    protected String[] attributeValue;

    /**
     * Make sure that in attributes temporary array is enough space.
     */
    protected  void ensureAttributesCapacity(int size) {
        final int attrPosSize = attributeName != null ? attributeName.length : 0;
        if(size >= attrPosSize) {
            final int newSize = size > 7 ? 2 * size : 8; // = lucky 7 + 1 //25

            final boolean needsCopying = attrPosSize > 0;
            String[] arr = null;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributeName, 0, arr, 0, attrPosSize);
            attributeName = arr;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributePrefix, 0, arr, 0, attrPosSize);
            attributePrefix = arr;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributeUri, 0, arr, 0, attrPosSize);
            attributeUri = arr;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributeValue, 0, arr, 0, attrPosSize);
            attributeValue = arr;

            if( ! allStringsInterned ) {
                final int[] iarr = new int[newSize];
                if(needsCopying) System.arraycopy(attributeNameHash, 0, iarr, 0, attrPosSize);
                attributeNameHash = iarr;
            }

            arr = null;
        }
    }
    protected int namespaceEnd;
    protected String namespacePrefix[];
    protected int namespacePrefixHash[];
    protected String namespaceUri[];

    protected void ensureNamespacesCapacity(int size) {
        final int namespaceSize = namespacePrefix != null ? namespacePrefix.length : 0;
        if(size >= namespaceSize) {
            final int newSize = size > 7 ? 2 * size : 8; // = lucky 7 + 1 //25

            final String[] newNamespacePrefix = new String[newSize];
            final String[] newNamespaceUri = new String[newSize];
            if(namespacePrefix != null) {
                System.arraycopy(
                    namespacePrefix, 0, newNamespacePrefix, 0, namespaceEnd);
                System.arraycopy(
                    namespaceUri, 0, newNamespaceUri, 0, namespaceEnd);
            }
            namespacePrefix = newNamespacePrefix;
            namespaceUri = newNamespaceUri;


            if( ! allStringsInterned ) {
                final int[] newNamespacePrefixHash = new int[newSize];
                if(namespacePrefixHash != null) {
                    System.arraycopy(
                        namespacePrefixHash, 0, newNamespacePrefixHash, 0, namespaceEnd);
                }
                namespacePrefixHash = newNamespacePrefixHash;
            }
        }
    }
    protected static int fastHash(char ch[], int off, int len ) {
        if(len == 0) {
            return 0;
        }
        int hash = ch[off];
        hash = (hash << 7) + ch[ off +  len - 1 ];
        if(len > 16) {
            hash = (hash << 7) + ch[ off + (len / 4)];
        }
        if(len > 8) {
            hash = (hash << 7) + ch[ off + (len / 2)];
        }
        return  hash;
    }
    protected int entityEnd;

    protected String entityName[];
    protected char[] entityNameBuf[];
    protected String entityReplacement[];
    protected char[] entityReplacementBuf[];

    protected int entityNameHash[];

    protected void ensureEntityCapacity() {
        final int entitySize = entityReplacementBuf != null ? entityReplacementBuf.length : 0;
        if(entityEnd >= entitySize) {
            final int newSize = entityEnd > 7 ? 2 * entityEnd : 8; // = lucky 7 + 1 //25
            final String[] newEntityName = new String[newSize];
            final char[][] newEntityNameBuf = new char[newSize][];
            final String[] newEntityReplacement = new String[newSize];
            final char[][] newEntityReplacementBuf = new char[newSize][];
            if(entityName != null) {
                System.arraycopy(entityName, 0, newEntityName, 0, entityEnd);
                System.arraycopy(entityNameBuf, 0, newEntityNameBuf, 0, entityEnd);
                System.arraycopy(entityReplacement, 0, newEntityReplacement, 0, entityEnd);
                System.arraycopy(entityReplacementBuf, 0, newEntityReplacementBuf, 0, entityEnd);
            }
            entityName = newEntityName;
            entityNameBuf = newEntityNameBuf;
            entityReplacement = newEntityReplacement;
            entityReplacementBuf = newEntityReplacementBuf;

            if( ! allStringsInterned ) {
                final int[] newEntityNameHash = new int[newSize];
                if(entityNameHash != null) {
                    System.arraycopy(entityNameHash, 0, newEntityNameHash, 0, entityEnd);
                }
                entityNameHash = newEntityNameHash;
            }
        }
    }
    protected static final int READ_CHUNK_SIZE = 8*1024;
    protected Reader reader;
    protected String inputEncoding;
    protected InputStream inputStream;


    protected int bufLoadFactor = 95;

    protected char buf[] = new char[READ_CHUNK_SIZE];
    protected int bufSoftLimit = ( bufLoadFactor * buf.length ) /100;
    protected boolean preventBufferCompaction;

    protected int bufAbsoluteStart; // this is buf
    protected int bufStart;
    protected int bufEnd;
    protected int pos;
    protected int posStart;
    protected int posEnd;

    protected char[] pc = new char[
        Runtime.getRuntime().freeMemory() > 1000000L ? READ_CHUNK_SIZE : 64 ];
    protected int pcStart;
    protected int pcEnd;

    protected boolean usePC;


    protected boolean seenStartTag;
    protected boolean seenEndTag;
    protected boolean pastEndTag;
    protected boolean seenAmpersand;
    protected boolean seenMarkup;
    protected boolean seenDocdecl;

    protected boolean tokenize;
    protected String text;
    protected String entityRefName;

    protected String xmlDeclVersion;
    protected Boolean xmlDeclStandalone;
    protected String xmlDeclContent;

    protected void reset() {
        location = null;
        lineNumber = 1;
        columnNumber = 0;
        seenRoot = false;
        reachedEnd = false;
        eventType = START_DOCUMENT;
        emptyElementTag = false;

        depth = 0;

        attributeCount = 0;

        namespaceEnd = 0;

        entityEnd = 0;

        reader = null;
        inputEncoding = null;

        preventBufferCompaction = false;
        bufAbsoluteStart = 0;
        bufEnd = bufStart = 0;
        pos = posStart = posEnd = 0;

        pcEnd = pcStart = 0;

        usePC = false;

        seenStartTag = false;
        seenEndTag = false;
        pastEndTag = false;
        seenAmpersand = false;
        seenMarkup = false;
        seenDocdecl = false;

        xmlDeclVersion = null;
        xmlDeclStandalone = null;
        xmlDeclContent = null;

        resetStringCache();
    }

    public MXParser() {
    }
    public void setFeature(String name, boolean state) throws XmlPullParserException
    {
        if(name == null) throw new IllegalArgumentException("feature name should not be null");
        if(FEATURE_PROCESS_NAMESPACES.equals(name)) {
            if(eventType != START_DOCUMENT) throw new XmlPullParserException(
                    "namespace processing feature can only be changed before parsing", this, null);
            processNamespaces = state;
        } else if(FEATURE_NAMES_INTERNED.equals(name)) {
            if(state != false) {
                throw new XmlPullParserException(
                    "interning names in this implementation is not supported");
            }
        } else if(FEATURE_PROCESS_DOCDECL.equals(name)) {
            if(state != false) {
                throw new XmlPullParserException(
                    "processing DOCDECL is not supported");
            }
        } else if(FEATURE_XML_ROUNDTRIP.equals(name)) {
            roundtripSupported = state;
        } else {
            throw new XmlPullParserException("unsupported feature "+name);
        }
    }

    public boolean getFeature(String name)
    {
        if(name == null) throw new IllegalArgumentException("feature name should not be null");
        if(FEATURE_PROCESS_NAMESPACES.equals(name)) {
            return processNamespaces;
        } else if(FEATURE_NAMES_INTERNED.equals(name)) {
            return false;
        } else if(FEATURE_PROCESS_DOCDECL.equals(name)) {
            return false;
        } else if(FEATURE_XML_ROUNDTRIP.equals(name)) {
            return roundtripSupported;
        }
        return false;
    }

    public void setProperty(String name,
                            Object value)
        throws XmlPullParserException
    {
        if(PROPERTY_LOCATION.equals(name)) {
            location = (String) value;
        } else {
            throw new XmlPullParserException("unsupported property: '"+name+"'");
        }
    }


    public Object getProperty(String name)
    {
        if(name == null) throw new IllegalArgumentException("property name should not be null");
        if(PROPERTY_XMLDECL_VERSION.equals(name)) {
            return xmlDeclVersion;
        } else if(PROPERTY_XMLDECL_STANDALONE.equals(name)) {
            return xmlDeclStandalone;
        } else if(PROPERTY_XMLDECL_CONTENT.equals(name)) {
            return xmlDeclContent;
        } else if(PROPERTY_LOCATION.equals(name)) {
            return location;
        }
        return null;
    }


    public void setInput(Reader in) throws XmlPullParserException
    {
        reset();
        reader = in;
    }

    public void setInput(java.io.InputStream inputStream, String inputEncoding)
        throws XmlPullParserException
    {
        if(inputStream == null) {
            throw new IllegalArgumentException("input stream can not be null");
        }
        this.inputStream = inputStream;
        Reader reader;
        try {
            if(inputEncoding != null) {
                reader = new InputStreamReader(inputStream, inputEncoding);
            } else {
                reader = new InputStreamReader(inputStream, "UTF-8");
            }
        } catch (UnsupportedEncodingException une) {
            throw new XmlPullParserException(
                "could not create reader for encoding "+inputEncoding+" : "+une, this, une);
        }
        setInput(reader);
        this.inputEncoding = inputEncoding;
    }
    public InputStream getInputStream(){
        return inputStream;
    }
    public Reader getReader(){
        reset();
        return reader;
    }

    public String getInputEncoding() {
        return inputEncoding;
    }

    public void defineEntityReplacementText(String entityName,
                                            String replacementText)
        throws XmlPullParserException
    {
        ensureEntityCapacity();

        // this is to make sure that if interning works we will take advantage of it ...
        this.entityName[entityEnd] = newString(entityName.toCharArray(), 0, entityName.length());
        entityNameBuf[entityEnd] = entityName.toCharArray();

        entityReplacement[entityEnd] = replacementText;
        entityReplacementBuf[entityEnd] = replacementText.toCharArray();
        if(!allStringsInterned) {
            entityNameHash[ entityEnd ] =
                fastHash(entityNameBuf[entityEnd], 0, entityNameBuf[entityEnd].length);
        }
        ++entityEnd;
    }

    public int getNamespaceCount(int depth) throws XmlPullParserException
    {
        if(!processNamespaces || depth == 0) {
            return 0;
        }
        if(depth < 0 || depth > this.depth) {
            throw new IllegalArgumentException(
                    "allowed namespace depth 0.."+this.depth+" not "+depth);
        }
        return elNamespaceCount[ depth ];
    }

    public String getNamespacePrefix(int pos)
        throws XmlPullParserException
    {
        if(pos < namespaceEnd) {
            return namespacePrefix[ pos ];
        } else {
            throw new XmlPullParserException(
                "position "+pos+" exceeded number of available namespaces "+namespaceEnd);
        }
    }

    public String getNamespaceUri(int pos) throws XmlPullParserException
    {
        if(pos < namespaceEnd) {
            return namespaceUri[ pos ];
        } else {
            throw new XmlPullParserException(
                "position "+pos+" exceeded number of available namespaces "+namespaceEnd);
        }
    }

    public String getNamespace( String prefix )
    {
        if(prefix != null) {
            for( int i = namespaceEnd -1; i >= 0; i--) {
                if( prefix.equals( namespacePrefix[ i ] ) ) {
                    return namespaceUri[ i ];
                }
            }
            if("xml".equals( prefix )) {
                return XML_URI;
            } else if("xmlns".equals( prefix )) {
                return XMLNS_URI;
            }
        } else {
            for( int i = namespaceEnd -1; i >= 0; i--) {
                if( namespacePrefix[ i ]  == null) { //"") { //null ) { //TODO check FIXME Alek
                    return namespaceUri[ i ];
                }
            }

        }
        return null;
    }


    public int getDepth()
    {
        return depth;
    }


    private static int findFragment(int bufMinPos, char[] b, int start, int end) {
        if(start < bufMinPos) {
            start = bufMinPos;
            if(start > end) start = end;
            return start;
        }
        if(end - start > 65) {
            start = end - 10; // try to find good location
        }
        int i = start + 1;
        while(--i > bufMinPos) {
            if((end - i) > 65) break;
            final char c = b[i];
            if(c == '<' && (start - i) > 10) break;
        }
        return i;
    }
    @Override
    public String getPositionDescription()
    {
        return "line="+getLineNumber()+", col="+getColumnNumber();
    }
    @Override
    public int getLineNumber()
    {
        return lineNumber;
    }
    @Override
    public int getColumnNumber()
    {
        return columnNumber;
    }


    public boolean isWhitespace() throws XmlPullParserException
    {
        if(eventType == TEXT || eventType == CDSECT) {
            if(usePC) {
                for (int i = pcStart; i <pcEnd; i++)
                {
                    if(!isS(pc[ i ])) return false;
                }
                return true;
            } else {
                for (int i = posStart; i <posEnd; i++)
                {
                    if(!isS(buf[ i ])) return false;
                }
                return true;
            }
        } else if(eventType == IGNORABLE_WHITESPACE) {
            return true;
        }
        throw new XmlPullParserException("no content available to check for white spaces");
    }

    public String getText()
    {
        if(eventType == START_DOCUMENT || eventType == END_DOCUMENT) {
            return null;
        } else if(eventType == ENTITY_REF) {
            return text;
        }
        if(text == null) {
            if(!usePC || eventType == START_TAG || eventType == END_TAG) {
                text = new String(buf, posStart, posEnd - posStart);
            } else {
                text = new String(pc, pcStart, pcEnd - pcStart);
            }
        }
        return text;
    }

    public char[] getTextCharacters(int [] holderForStartAndLength)
    {
        if( eventType == TEXT ) {
            if(usePC) {
                holderForStartAndLength[0] = pcStart;
                holderForStartAndLength[1] = pcEnd - pcStart;
                return pc;
            } else {
                holderForStartAndLength[0] = posStart;
                holderForStartAndLength[1] = posEnd - posStart;
                return buf;

            }
        } else if( eventType == START_TAG
                      || eventType == END_TAG
                      || eventType == CDSECT
                      || eventType == COMMENT
                      || eventType == ENTITY_REF
                      || eventType == PROCESSING_INSTRUCTION
                      || eventType == IGNORABLE_WHITESPACE
                      || eventType == DOCDECL)
        {
            holderForStartAndLength[0] = posStart;
            holderForStartAndLength[1] = posEnd - posStart;
            return buf;
        } else if(eventType == START_DOCUMENT
                      || eventType == END_DOCUMENT) {

            holderForStartAndLength[0] = holderForStartAndLength[1] = -1;
            return null;
        } else {
            throw new IllegalArgumentException("unknown text eventType: "+eventType);
        }
    }

    public String getNamespace()
    {
        if(eventType == START_TAG) {
            //return processNamespaces ? elUri[ depth - 1 ] : NO_NAMESPACE;
            return processNamespaces ? elUri[ depth  ] : NO_NAMESPACE;
        } else if(eventType == END_TAG) {
            return processNamespaces ? elUri[ depth ] : NO_NAMESPACE;
        }
        return null;
    }

    public String getName()
    {
        if(eventType == START_TAG) {
            //return elName[ depth - 1 ] ;
            return elName[ depth ] ;
        } else if(eventType == END_TAG) {
            return elName[ depth ] ;
        } else if(eventType == ENTITY_REF) {
            if(entityRefName == null) {
                entityRefName = newString(buf, posStart, posEnd - posStart);
            }
            return entityRefName;
        } else {
            return null;
        }
    }

    public String getPrefix()
    {
        if(eventType == START_TAG) {
            return elPrefix[ depth ] ;
        } else if(eventType == END_TAG) {
            return elPrefix[ depth ] ;
        }
        return null;
    }


    public boolean isEmptyElementTag() throws XmlPullParserException
    {
        if(eventType != START_TAG) throw new XmlPullParserException(
                "parser must be on START_TAG to check for empty element", this, null);
        return emptyElementTag;
    }

    public int getAttributeCount()
    {
        if(eventType != START_TAG) return -1;
        return attributeCount;
    }

    public String getAttributeNamespace(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(processNamespaces == false) return NO_NAMESPACE;
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributeUri[ index ];
    }

    public String getAttributeName(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributeName[ index ];
    }

    public String getAttributePrefix(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(processNamespaces == false) return null;
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributePrefix[ index ];
    }

    public String getAttributeType(int index) {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return "CDATA";
    }

    public boolean isAttributeDefault(int index) {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return false;
    }

    public String getAttributeValue(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributeValue[ index ];
    }

    @Override
    public String getAttributeValue(String namespace, String name)
    {
        if(eventType != START_TAG) {
            throw new IndexOutOfBoundsException("only START_TAG can have attributes "
                    +getPositionDescription());
        }
        if(name == null) {
            throw new IllegalArgumentException("attribute name can not be null");
        }
        // TODO make check if namespace is interned!!! etc. for names!!!
        if(processNamespaces) {
            if(namespace == null) {
                namespace = "";
            }

            for(int i = 0; i < attributeCount; ++i) {
                if((namespace == attributeUri[ i ] ||
                        namespace.equals(attributeUri[ i ]) )
                       //(namespace != null && namespace.equals(attributeUri[ i ]))
                       // taking advantage of String.intern()
                       && name.equals(attributeName[ i ]) )
                {
                    return attributeValue[i];
                }
            }
        } else {
            if(namespace != null && namespace.length() == 0) {
                namespace = null;
            }
            if(namespace != null) throw new IllegalArgumentException(
                    "when namespaces processing is disabled attribute namespace must be null");
            for(int i = 0; i < attributeCount; ++i) {
                if(name.equals(attributeName[i]))
                {
                    return attributeValue[i];
                }
            }
        }
        return null;
    }


    public int getEventType()
        throws XmlPullParserException
    {
        return eventType;
    }

    public void require(int type, String namespace, String name)
        throws XmlPullParserException, IOException
    {
        if(processNamespaces == false && namespace != null) {
            throw new XmlPullParserException(
                "processing namespaces must be enabled on parser (or factory)"+
                    " to have possible namespaces declared on elements"
                    +(" (position:"+ getPositionDescription())+")");
        }
        if (type != getEventType()
                || (namespace != null && !namespace.equals (getNamespace()))
                || (name != null && !name.equals (getName ())) )
        {
            throw new XmlPullParserException (
                "expected event "+TYPES[ type ]
                    +(name != null ? " with name '"+name+"'" : "")
                    +(namespace != null && name != null ? " and" : "")
                    +(namespace != null ? " with namespace '"+namespace+"'" : "")
                    +" but got"
                    +(type != getEventType() ? " "+TYPES[ getEventType() ] : "")
                    +(name != null && getName() != null && !name.equals (getName ())
                          ? " name '"+getName()+"'" : "")
                    +(namespace != null && name != null
                          && getName() != null && !name.equals (getName ())
                          && getNamespace() != null && !namespace.equals (getNamespace())
                          ? " and" : "")
                    +(namespace != null && getNamespace() != null && !namespace.equals (getNamespace())
                          ? " namespace '"+getNamespace()+"'" : "")
                    +(" (position:"+ getPositionDescription())+")");
        }
    }

    public void skipSubTree()
        throws XmlPullParserException, IOException
    {
        require(START_TAG, null, null);
        int level = 1;
        while(level > 0) {
            int eventType = next();
            if(eventType == END_TAG) {
                --level;
            } else if(eventType == START_TAG) {
                ++level;
            }
        }
    }

    public String nextText() throws XmlPullParserException, IOException
    {
        if(getEventType() != START_TAG) {
            throw new XmlPullParserException(
                "parser must be on START_TAG to read next text", this, null);
        }
        int eventType = next();
        if(eventType == TEXT) {
            final String result = getText();
            eventType = next();
            if(eventType != END_TAG) {
                throw new XmlPullParserException(
                    "TEXT must be immediately followed by END_TAG and not "
                        +TYPES[ getEventType() ], this, null);
            }
            return result;
        } else if(eventType == END_TAG) {
            return "";
        } else {
            throw new XmlPullParserException(
                "parser must be on START_TAG or TEXT to read text", this, null);
        }
    }

    public int nextTag() throws XmlPullParserException, IOException
    {
        next();
        if(eventType == TEXT && isWhitespace()) {  // skip whitespace
            next();
        }
        if (eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("expected START_TAG or END_TAG not "
                                                 +TYPES[ getEventType() ], this, null);
        }
        return eventType;
    }

    public int next()
        throws XmlPullParserException, IOException
    {
        tokenize = false;
        return nextImpl();
    }

    public int nextToken()
        throws XmlPullParserException, IOException
    {
        tokenize = true;
        return nextImpl();
    }


    protected int nextImpl()
        throws XmlPullParserException, IOException
    {
        text = null;
        pcEnd = pcStart = 0;
        usePC = false;
        bufStart = posEnd;
        if(pastEndTag) {
            pastEndTag = false;
            --depth;
            namespaceEnd = elNamespaceCount[ depth ]; // less namespaces available
        }
        if(emptyElementTag) {
            emptyElementTag = false;
            pastEndTag = true;
            return eventType = END_TAG;
        }
        if(depth > 0) {

            if(seenStartTag) {
                seenStartTag = false;
                return eventType = parseStartTag();
            }
            if(seenEndTag) {
                seenEndTag = false;
                return eventType = parseEndTag();
            }

            char ch;
            if(seenMarkup) {  // we have read ahead ...
                seenMarkup = false;
                ch = '<';
            } else if(seenAmpersand) {
                seenAmpersand = false;
                ch = '&';
            } else {
                ch = more();
            }
            posStart = pos - 1; // VERY IMPORTANT: this is correct start of event!!!

            // when true there is some potential event TEXT to return - keep gathering
            boolean hadCharData = false;

            // when true TEXT data is not continual (like <![CDATA[text]]>) and requires PC merging
            boolean needsMerging = false;

            MAIN_LOOP:
            while(true) {
                // work on MARKUP
                if(ch == '<') {
                    if(hadCharData) {
                        //posEnd = pos - 1;
                        if(tokenize) {
                            seenMarkup = true;
                            return eventType = TEXT;
                        }
                    }
                    ch = more();
                    if(ch == '/') {
                        if(!tokenize && hadCharData) {
                            seenEndTag = true;
                            //posEnd = pos - 2;
                            return eventType = TEXT;
                        }
                        return eventType = parseEndTag();
                    } else if(ch == '!') {
                        ch = more();
                        if(ch == '-') {
                            // note: if(tokenize == false) posStart/End is NOT changed!!!!
                            parseComment();
                            if(tokenize) return eventType = COMMENT;
                            if( !usePC && hadCharData ) {
                                needsMerging = true;
                            } else {
                                posStart = pos;  //completely ignore comment
                            }
                        } else if(ch == '[') {
                            parseCDSect(hadCharData);
                            if(tokenize) return eventType = CDSECT;
                            final int cdStart = posStart;
                            final int cdEnd = posEnd;
                            final int cdLen = cdEnd - cdStart;


                            if(cdLen > 0) { // was there anything inside CDATA section?
                                hadCharData = true;
                                if(!usePC) {
                                    needsMerging = true;
                                }
                            }
                        } else {
                            throw new XmlPullParserException(
                                "unexpected character in markup "+printable(ch), this, null);
                        }
                    } else if(ch == '?') {
                        parsePI();
                        if(tokenize) return eventType = PROCESSING_INSTRUCTION;
                        if( !usePC && hadCharData ) {
                            needsMerging = true;
                        } else {
                            posStart = pos;  //completely ignore PI
                        }

                    } else if( isNameStartChar(ch) ) {
                        if(!tokenize && hadCharData) {
                            seenStartTag = true;
                            //posEnd = pos - 2;
                            return eventType = TEXT;
                        }
                        return eventType = parseStartTag();
                    } else {
                        throw new XmlPullParserException(
                            "unexpected character in markup "+printable(ch), this, null);
                    }

                } else if(ch == '&') {
                    // work on ENTITTY
                    //posEnd = pos - 1;
                    if(tokenize && hadCharData) {
                        seenAmpersand = true;
                        return eventType = TEXT;
                    }
                    final int oldStart = posStart + bufAbsoluteStart;
                    final int oldEnd = posEnd + bufAbsoluteStart;
                    final char[] resolvedEntity = parseEntityRef();
                    if(tokenize) return eventType = ENTITY_REF;
                    // check if replacement text can be resolved !!!
                    if(resolvedEntity == null) {
                        if(entityRefName == null) {
                            entityRefName = newString(buf, posStart, posEnd - posStart);
                        }
                        throw new XmlPullParserException(
                            "could not resolve entity named '"+printable(entityRefName)+"'",
                            this, null);
                    }
                    //int entStart = posStart;
                    //int entEnd = posEnd;
                    posStart = oldStart - bufAbsoluteStart;
                    posEnd = oldEnd - bufAbsoluteStart;
                    if(!usePC) {
                        if(hadCharData) {
                            joinPC(); // posEnd is already set correctly!!!
                            needsMerging = false;
                        } else {
                            usePC = true;
                            pcStart = pcEnd = 0;
                        }
                    }
                    for (int i = 0; i < resolvedEntity.length; i++)
                    {
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = resolvedEntity[ i ];

                    }
                    hadCharData = true;
                } else {

                    if(needsMerging) {
                        joinPC();
                        needsMerging = false;
                    }

                    hadCharData = true;

                    boolean normalizedCR = false;
                    final boolean normalizeInput = tokenize == false || roundtripSupported == false;

                    boolean seenBracket = false;
                    boolean seenBracketBracket = false;
                    do {
                        if(ch == ']') {
                            if(seenBracket) {
                                seenBracketBracket = true;
                            } else {
                                seenBracket = true;
                            }
                        } else if(seenBracketBracket && ch == '>') {
                            throw new XmlPullParserException(
                                "characters ]]> are not allowed in content", this, null);
                        } else {
                            if(seenBracket) {
                                seenBracketBracket = seenBracket = false;
                            }
                            // assert seenTwoBrackets == seenBracket == false;
                        }
                        if(normalizeInput) {
                            // deal with normalization issues ...
                            if(ch == '\r') {
                                normalizedCR = true;
                                posEnd = pos -1;
                                // posEnd is already is set
                                if(!usePC) {
                                    if(posEnd > posStart) {
                                        joinPC();
                                    } else {
                                        usePC = true;
                                        pcStart = pcEnd = 0;
                                    }
                                }
                                //assert usePC == true;
                                if(pcEnd >= pc.length) ensurePC(pcEnd);
                                pc[pcEnd++] = '\n';
                            } else if(ch == '\n') {
                                //   if(!usePC) {  joinPC(); } else { if(pcEnd >= pc.length) ensurePC(); }
                                if(!normalizedCR && usePC) {
                                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                                    pc[pcEnd++] = '\n';
                                }
                                normalizedCR = false;
                            } else {
                                if(usePC) {
                                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                                    pc[pcEnd++] = ch;
                                }
                                normalizedCR = false;
                            }
                        }

                        ch = more();
                    } while(ch != '<' && ch != '&');
                    posEnd = pos - 1;
                    continue MAIN_LOOP;
                }
                ch = more();
            } // endless while(true)
        } else {
            if(seenRoot) {
                return parseEpilog();
            } else {
                return parseProlog();
            }
        }
    }


    protected int parseProlog()
        throws XmlPullParserException, IOException
    {
        char ch;
        if(seenMarkup) {
            ch = buf[ pos - 1 ];
        } else {
            ch = more();
        }

        if(eventType == START_DOCUMENT) {
            if(ch == '\uFFFE') {
                throw new XmlPullParserException(
                    "first character in input was UNICODE noncharacter (0xFFFE)"+
                        "- input requires int swapping", this, null);
            }
            if(ch == '\uFEFF') {
                ch = more();
            }
        }
        seenMarkup = false;
        boolean gotS = false;
        posStart = pos - 1;
        final boolean normalizeIgnorableWS = tokenize == true && roundtripSupported == false;
        boolean normalizedCR = false;
        while(true) {
            if(ch == '<') {
                if(gotS && tokenize) {
                    posEnd = pos - 1;
                    seenMarkup = true;
                    return eventType = IGNORABLE_WHITESPACE;
                }
                ch = more();
                if(ch == '?') {
                    if(parsePI()) {  // make sure to skip XMLDecl
                        if(tokenize) {
                            return eventType = PROCESSING_INSTRUCTION;
                        }
                    } else {
                        // skip over - continue tokenizing
                        posStart = pos;
                        gotS = false;
                    }

                } else if(ch == '!') {
                    ch = more();
                    if(ch == 'D') {
                        if(seenDocdecl) {
                            throw new XmlPullParserException(
                                "only one docdecl allowed in XML document", this, null);
                        }
                        seenDocdecl = true;
                        parseDocdecl();
                        if(tokenize) return eventType = DOCDECL;
                    } else if(ch == '-') {
                        parseComment();
                        if(tokenize) return eventType = COMMENT;
                    } else {
                        throw new XmlPullParserException(
                            "unexpected markup <!"+printable(ch), this, null);
                    }
                } else if(ch == '/') {
                    throw new XmlPullParserException(
                        "expected start tag name and not "+printable(ch), this, null);
                } else if(isNameStartChar(ch)) {
                    seenRoot = true;
                    return parseStartTag();
                } else {
                    throw new XmlPullParserException(
                        "expected start tag name and not "+printable(ch), this, null);
                }
            } else if(isS(ch)) {
                gotS = true;
                if(normalizeIgnorableWS) {
                    if(ch == '\r') {
                        normalizedCR = true;
                        if(!usePC) {
                            posEnd = pos -1;
                            if(posEnd > posStart) {
                                joinPC();
                            } else {
                                usePC = true;
                                pcStart = pcEnd = 0;
                            }
                        }
                        //assert usePC == true;
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = '\n';
                    } else if(ch == '\n') {
                        if(!normalizedCR && usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = '\n';
                        }
                        normalizedCR = false;
                    } else {
                        if(usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = ch;
                        }
                        normalizedCR = false;
                    }
                }
            } else {
                throw new XmlPullParserException(
                    "only whitespace content allowed before start tag and not "+printable(ch),
                    this, null);
            }
            ch = more();
        }
    }

    protected int parseEpilog()
        throws XmlPullParserException, IOException
    {
        if(eventType == END_DOCUMENT) {
            throw new XmlPullParserException("already reached end of XML input", this, null);
        }
        if(reachedEnd) {
            return eventType = END_DOCUMENT;
        }
        boolean gotS = false;
        final boolean normalizeIgnorableWS = tokenize && !roundtripSupported;
        boolean normalizedCR = false;
        try {
            char ch;
            if(seenMarkup) {
                ch = buf[ pos - 1 ];
            } else {
                ch = more();
            }
            seenMarkup = false;
            posStart = pos - 1;
            if(!reachedEnd) {
                while(true) {
                    if(ch == '<') {
                        if(gotS && tokenize) {
                            posEnd = pos - 1;
                            seenMarkup = true;
                            return eventType = IGNORABLE_WHITESPACE;
                        }
                        ch = more();
                        if(reachedEnd) {
                            break;
                        }
                        if(ch == '?') {
                            parsePI();
                            if(tokenize) return eventType = PROCESSING_INSTRUCTION;

                        } else if(ch == '!') {
                            ch = more();
                            if(reachedEnd) {
                                break;
                            }
                            if(ch == 'D') {
                                parseDocdecl();
                                if(tokenize) {
                                    return eventType = DOCDECL;
                                }
                            } else if(ch == '-') {
                                parseComment();
                                if(tokenize) return eventType = COMMENT;
                            } else {
                                throw new XmlPullParserException(
                                    "unexpected markup <!"+printable(ch), this, null);
                            }
                        } else if(ch == '/') {
                            throw new XmlPullParserException(
                                "end tag not allowed in epilog but got "+printable(ch), this, null);
                        } else if(isNameStartChar(ch)) {
                            throw new XmlPullParserException(
                                "start tag not allowed in epilog but got "+printable(ch), this, null);
                        } else {
                            throw new XmlPullParserException(
                                "in epilog expected ignorable content and not "+printable(ch),
                                this, null);
                        }
                    } else if(isS(ch)) {
                        gotS = true;
                        if(normalizeIgnorableWS) {
                            if(ch == '\r') {
                                normalizedCR = true;
                                if(!usePC) {
                                    posEnd = pos -1;
                                    if(posEnd > posStart) {
                                        joinPC();
                                    } else {
                                        usePC = true;
                                        pcStart = pcEnd = 0;
                                    }
                                }
                                //assert usePC == true;
                                if(pcEnd >= pc.length) ensurePC(pcEnd);
                                pc[pcEnd++] = '\n';
                            } else if(ch == '\n') {
                                if(!normalizedCR && usePC) {
                                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                                    pc[pcEnd++] = '\n';
                                }
                                normalizedCR = false;
                            } else {
                                if(usePC) {
                                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                                    pc[pcEnd++] = ch;
                                }
                                normalizedCR = false;
                            }
                        }
                    } else {
                        throw new XmlPullParserException(
                            "in epilog non whitespace content is not allowed but got "+printable(ch),
                            this, null);
                    }
                    ch = more();
                    if(reachedEnd) {
                        break;
                    }

                }
            }
        } catch(EOFException ex) {
            reachedEnd = true;
        }
        if(reachedEnd) {
            if(tokenize && gotS) {
                posEnd = pos; // well - this is LAST available character pos
                return eventType = IGNORABLE_WHITESPACE;
            }
            return eventType = END_DOCUMENT;
        } else {
            throw new XmlPullParserException("internal error in parseEpilog");
        }
    }


    public int parseEndTag() throws XmlPullParserException, IOException {
        char ch = more();
        if(!isNameStartChar(ch)) {
            throw new XmlPullParserException(
                "expected name start and not "+printable(ch), this, null);
        }
        posStart = pos - 3;
        final int nameStart = pos - 1 + bufAbsoluteStart;
        do {
            ch = more();
        } while(isNameChar(ch));

        int off = nameStart - bufAbsoluteStart;
        final int len = (pos - 1) - off;
        final char[] cbuf = elRawName[depth];
        if(elRawNameEnd[depth] != len) {
            // construct strings for exception
            final String startname = new String(cbuf, 0, elRawNameEnd[depth]);
            final String endname = new String(buf, off, len);
            throw new XmlPullParserException(
                "end tag name </"+endname+"> must match start tag name <"+startname+">"
                    +" from line "+elRawNameLine[depth], this, null);
        }
        for (int i = 0; i < len; i++)
        {
            if(buf[off++] != cbuf[i]) {
                // construct strings for exception
                final String startname = new String(cbuf, 0, len);
                final String endname = new String(buf, off - i - 1, len);
                throw new XmlPullParserException(
                    "end tag name </"+endname+"> must be the same as start tag <"+startname+">"
                        +" from line "+elRawNameLine[depth], this, null);
            }
        }

        while(isS(ch)) {
            ch = more();
        } // skip additional white spaces
        if(ch != '>') {
            throw new XmlPullParserException(
                "expected > to finish end tag not "+printable(ch)
                    +" from line "+elRawNameLine[depth], this, null);
        }
        posEnd = pos;
        pastEndTag = true;
        return eventType = END_TAG;
    }

    public int parseStartTag() throws XmlPullParserException, IOException {
        ++depth;
        posStart = pos - 2;
        emptyElementTag = false;
        attributeCount = 0;
        final int nameStart = pos - 1 + bufAbsoluteStart;
        int colonPos = -1;
        char ch = buf[ pos - 1];
        if(ch == ':' && processNamespaces) throw new XmlPullParserException(
                "when namespaces processing enabled colon can not be at element name start",
                this, null);
        while(true) {
            ch = more();
            if(!isNameChar(ch)) break;
            if(ch == ':' && processNamespaces) {
                if(colonPos != -1) throw new XmlPullParserException(
                        "only one colon is allowed in name of element when namespaces are enabled",
                        this, null);
                colonPos = pos - 1 + bufAbsoluteStart;
            }
        }
        ensureElementsCapacity();
        int elLen = (pos - 1) - (nameStart - bufAbsoluteStart);
        if(elRawName[ depth ] == null || elRawName[ depth ].length < elLen) {
            elRawName[ depth ] = new char[ 2 * elLen ];
        }
        System.arraycopy(buf, nameStart - bufAbsoluteStart, elRawName[ depth ], 0, elLen);
        elRawNameEnd[ depth ] = elLen;
        elRawNameLine[ depth ] = lineNumber;

        String name = null;

        // work on prefixes and namespace URI
        String prefix = null;
        if(processNamespaces) {
            if(colonPos != -1) {
                prefix = elPrefix[ depth ] = newString(buf, nameStart - bufAbsoluteStart,
                                                       colonPos - nameStart);
                name = elName[ depth ] = newString(buf, colonPos + 1 - bufAbsoluteStart,
                                                   //(pos -1) - (colonPos + 1));
                                                   pos - 2 - (colonPos - bufAbsoluteStart));
            } else {
                prefix = elPrefix[ depth ] = null;
                name = elName[ depth ] = newString(buf, nameStart - bufAbsoluteStart, elLen);
            }
        } else {
            name = elName[ depth ] = newString(buf, nameStart - bufAbsoluteStart, elLen);
        }


        while(true) {
            while(isS(ch)) {
                ch = more();
            }

            if(ch == '>') {
                break;
            } else if(ch == '/') {
                if(emptyElementTag) throw new XmlPullParserException(
                        "repeated / in tag declaration", this, null);
                emptyElementTag = true;
                ch = more();
                if(ch != '>') throw new XmlPullParserException(
                        "expected > to end empty tag not "+printable(ch), this, null);
                break;
            } else if(isNameStartChar(ch)) {
                ch = parseAttribute();
                ch = more();
                continue;
            } else {
                throw new XmlPullParserException(
                    "start tag unexpected character "+printable(ch), this, null);
            }
        }

        // now when namespaces were declared we can resolve them
        if(processNamespaces) {
            String uri = getNamespace(prefix);
            if(uri == null) {
                if(prefix == null) { // no prefix and no uri => use default namespace
                    uri = NO_NAMESPACE;
                } else {
                    throw new XmlPullParserException(
                        "could not determine namespace bound to element prefix "+prefix,
                        this, null);
                }

            }
            elUri[ depth ] = uri;

            for (int i = 0; i < attributeCount; i++)
            {
                final String attrPrefix = attributePrefix[ i ];
                if(attrPrefix != null) {
                    final String attrUri = getNamespace(attrPrefix);
                    if(attrUri == null) {
                        throw new XmlPullParserException(
                            "could not determine namespace bound to attribute prefix "+attrPrefix,
                            this, null);

                    }
                    attributeUri[ i ] = attrUri;
                } else {
                    attributeUri[ i ] = NO_NAMESPACE;
                }
            }

            for (int i = 1; i < attributeCount; i++)
            {
                for (int j = 0; j < i; j++)
                {
                    if( attributeUri[j] == attributeUri[i]
                           && (allStringsInterned && attributeName[j].equals(attributeName[i])
                                   || (!allStringsInterned
                                           && attributeNameHash[ j ] == attributeNameHash[ i ]
                                           && attributeName[j].equals(attributeName[i])) )

                      ) {
                        // prepare data for nice error message?
                        String attr1 = attributeName[j];
                        if(attributeUri[j] != null) attr1 = attributeUri[j]+":"+attr1;
                        String attr2 = attributeName[i];
                        if(attributeUri[i] != null) attr2 = attributeUri[i]+":"+attr2;
                        throw new XmlPullParserException(
                            "duplicated attributes "+attr1+" and "+attr2, this, null);
                    }
                }
            }


        } else {
            for (int i = 1; i < attributeCount; i++)
            {
                for (int j = 0; j < i; j++)
                {
                    if((allStringsInterned && attributeName[j].equals(attributeName[i])
                            || (!allStringsInterned
                                    && attributeNameHash[ j ] == attributeNameHash[ i ]
                                    && attributeName[j].equals(attributeName[i])) )

                      ) {
                        // prepare data for nice error message?
                        final String attr1 = attributeName[j];
                        final String attr2 = attributeName[i];
                        throw new XmlPullParserException(
                            "duplicated attributes "+attr1+" and "+attr2, this, null);
                    }
                }
            }
        }

        elNamespaceCount[ depth ] = namespaceEnd;
        posEnd = pos;
        return eventType = START_TAG;
    }

    protected char parseAttribute() throws XmlPullParserException, IOException
    {
        final int prevPosStart = posStart + bufAbsoluteStart;
        final int nameStart = pos - 1 + bufAbsoluteStart;
        int colonPos = -1;
        char ch = buf[ pos - 1 ];
        if(ch == ':' && processNamespaces) throw new XmlPullParserException(
                "when namespaces processing enabled colon can not be at attribute name start",
                this, null);


        boolean startsWithXmlns = processNamespaces && ch == 'x';
        int xmlnsPos = 0;

        ch = more();
        while(isNameChar(ch)) {
            if(processNamespaces) {
                if(startsWithXmlns && xmlnsPos < 5) {
                    ++xmlnsPos;
                    if(xmlnsPos == 1) { if(ch != 'm') startsWithXmlns = false; }
                    else if(xmlnsPos == 2) { if(ch != 'l') startsWithXmlns = false; }
                    else if(xmlnsPos == 3) { if(ch != 'n') startsWithXmlns = false; }
                    else if(xmlnsPos == 4) { if(ch != 's') startsWithXmlns = false; }
                    else if(xmlnsPos == 5) {
                        if(ch != ':') throw new XmlPullParserException(
                                "after xmlns in attribute name must be colon"
                                    +"when namespaces are enabled", this, null);
                        //colonPos = pos - 1 + bufAbsoluteStart;
                    }
                }
                if(ch == ':') {
                    if(colonPos != -1) throw new XmlPullParserException(
                            "only one colon is allowed in attribute name"
                                +" when namespaces are enabled", this, null);
                    colonPos = pos - 1 + bufAbsoluteStart;
                }
            }
            ch = more();
        }

        ensureAttributesCapacity(attributeCount);

        String name = null;
        String prefix = null;
        if(processNamespaces) {
            if(xmlnsPos < 4) startsWithXmlns = false;
            if(startsWithXmlns) {
                if(colonPos != -1) {
                    //prefix = attributePrefix[ attributeCount ] = null;
                    final int nameLen = pos - 2 - (colonPos - bufAbsoluteStart);
                    if(nameLen == 0) {
                        throw new XmlPullParserException(
                            "namespace prefix is required after xmlns: "
                                +" when namespaces are enabled", this, null);
                    }
                    name = //attributeName[ attributeCount ] =
                        newString(buf, colonPos - bufAbsoluteStart + 1, nameLen);
                }
            } else {
                if(colonPos != -1) {
                    int prefixLen = colonPos - nameStart;
                    prefix = attributePrefix[ attributeCount ] =
                        newString(buf, nameStart - bufAbsoluteStart,prefixLen);
                    //colonPos - (nameStart - bufAbsoluteStart));
                    int nameLen = pos - 2 - (colonPos - bufAbsoluteStart);
                    name = attributeName[ attributeCount ] =
                        newString(buf, colonPos - bufAbsoluteStart + 1, nameLen);
                } else {
                    prefix = attributePrefix[ attributeCount ]  = null;
                    name = attributeName[ attributeCount ] =
                        newString(buf, nameStart - bufAbsoluteStart,
                                  pos - 1 - (nameStart - bufAbsoluteStart));
                }
                if(!allStringsInterned) {
                    attributeNameHash[ attributeCount ] = name.hashCode();
                }
            }

        } else {
            name = attributeName[ attributeCount ] =
                newString(buf, nameStart - bufAbsoluteStart,
                          pos - 1 - (nameStart - bufAbsoluteStart));
            if(!allStringsInterned) {
                attributeNameHash[ attributeCount ] = name.hashCode();
            }
        }

        while(isS(ch)) {
            ch = more();
        } // skip additional spaces
        if(ch != '=') {
            throw new XmlPullParserException(
                    "expected = after attribute name '"+name+processNamespaces+"'", this, null);
        }
        ch = more();
        while(isS(ch)) {
            ch = more();
        } // skip additional spaces

        final char delimit = ch;
        if(delimit != '"' && delimit != '\'') throw new XmlPullParserException(
                "attribute value must start with quotation or apostrophe not "
                    +printable(delimit), this, null);
        boolean normalizedCR = false;
        usePC = false;
        pcStart = pcEnd;
        posStart = pos;

        while(true) {
            ch = more();
            if(ch == delimit) {
                break;
            } if(ch == '<') {
                throw new XmlPullParserException(
                    "markup not allowed inside attribute value - illegal < ", this, null);
            } if(ch == '&') {
                posEnd = pos - 1;
                if(!usePC) {
                    final boolean hadCharData = posEnd > posStart;
                    if(hadCharData) {
                        // posEnd is already set correctly!!!
                        joinPC();
                    } else {
                        usePC = true;
                        pcStart = pcEnd = 0;
                    }
                }
                final char[] resolvedEntity = parseEntityRef();
                if(resolvedEntity == null) {
                    if(entityRefName == null) {
                        entityRefName = newString(buf, posStart, posEnd - posStart);
                    }
                    throw new XmlPullParserException(
                        "could not resolve entity named '"+printable(entityRefName)+"'",
                        this, null);
                }
                for (int i = 0; i < resolvedEntity.length; i++)
                {
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = resolvedEntity[ i ];
                }
            } else if(ch == '\t' || ch == '\n' || ch == '\r') {
                if(!usePC) {
                    posEnd = pos - 1;
                    if(posEnd > posStart) {
                        joinPC();
                    } else {
                        usePC = true;
                        pcEnd = pcStart = 0;
                    }
                }
                //assert usePC == true;
                if(pcEnd >= pc.length) ensurePC(pcEnd);
                if(ch != '\n' || !normalizedCR) {
                    pc[pcEnd++] = ' '; //'\n';
                }

            } else {
                if(usePC) {
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = ch;
                }
            }
            normalizedCR = ch == '\r';
        }


        if(processNamespaces && startsWithXmlns) {
            String ns = null;
            if(!usePC) {
                ns = newStringIntern(buf, posStart, pos - 1 - posStart);
            } else {
                ns = newStringIntern(pc, pcStart, pcEnd - pcStart);
            }
            ensureNamespacesCapacity(namespaceEnd);
            int prefixHash = -1;
            if(colonPos != -1) {
                if(ns.length() == 0) {
                    throw new XmlPullParserException(
                        "non-default namespace can not be declared to be empty string", this, null);
                }
                // declare new namespace
                namespacePrefix[ namespaceEnd ] = name;
                if(!allStringsInterned) {
                    prefixHash = namespacePrefixHash[ namespaceEnd ] = name.hashCode();
                }
            } else {
                // declare  new default namespace ...
                namespacePrefix[ namespaceEnd ] = null;
                if(!allStringsInterned) {
                    prefixHash = namespacePrefixHash[ namespaceEnd ] = -1;
                }
            }
            namespaceUri[ namespaceEnd ] = ns;
            final int startNs = elNamespaceCount[ depth - 1 ];
            for (int i = namespaceEnd - 1; i >= startNs; --i)
            {
                if(((allStringsInterned || name == null) && namespacePrefix[ i ] == name)
                       || (!allStringsInterned && name != null &&
                               namespacePrefixHash[ i ] == prefixHash
                               && name.equals(namespacePrefix[ i ])
                          ))
                {
                    final String s = name == null ? "default" : "'"+name+"'";
                    throw new XmlPullParserException(
                        "duplicated namespace declaration for "+s+" prefix", this, null);
                }
            }

            ++namespaceEnd;

        } else {
            if(!usePC) {
                attributeValue[ attributeCount ] =
                    new String(buf, posStart, pos - 1 - posStart);
            } else {
                attributeValue[ attributeCount ] =
                    new String(pc, pcStart, pcEnd - pcStart);
            }
            ++attributeCount;
        }
        posStart = prevPosStart - bufAbsoluteStart;
        return ch;
    }

    protected char[] charRefOneCharBuf = new char[1];

    protected char[] parseEntityRef()
        throws XmlPullParserException, IOException
    {
        entityRefName = null;
        posStart = pos;
        char ch = more();
        if(ch == '#') {
            // parse character reference
            char charRef = 0;
            ch = more();
            if(ch == 'x') {
                //encoded in hex
                while(true) {
                    ch = more();
                    if(ch >= '0' && ch <= '9') {
                        charRef = (char)(charRef * 16 + (ch - '0'));
                    } else if(ch >= 'a' && ch <= 'f') {
                        charRef = (char)(charRef * 16 + (ch - ('a' - 10)));
                    } else if(ch >= 'A' && ch <= 'F') {
                        charRef = (char)(charRef * 16 + (ch - ('A' - 10)));
                    } else if(ch == ';') {
                        break;
                    } else {
                        throw new XmlPullParserException(
                            "character reference (with hex value) may not contain "
                                +printable(ch), this, null);
                    }
                }
            } else {
                // encoded in decimal
                while(true) {
                    if(ch >= '0' && ch <= '9') {
                        charRef = (char)(charRef * 10 + (ch - '0'));
                    } else if(ch == ';') {
                        break;
                    } else {
                        throw new XmlPullParserException(
                            "character reference (with decimal value) may not contain "
                                +printable(ch), this, null);
                    }
                    ch = more();
                }
            }
            posEnd = pos - 1;
            charRefOneCharBuf[0] = charRef;
            if(tokenize) {
                text = newString(charRefOneCharBuf, 0, 1);
            }
            return charRefOneCharBuf;
        } else {
            // [68]     EntityRef          ::=          '&' Name ';'
            // scan name until ;
            if(!isNameStartChar(ch)) {
                throw new XmlPullParserException(
                    "entity reference names can not start with character '"
                        +printable(ch)+"'", this, null);
            }
            while(true) {
                ch = more();
                if(ch == ';') {
                    break;
                }
                if(!isNameChar(ch)) {
                    throw new XmlPullParserException(
                        "entity reference name can not contain character "
                            +printable(ch)+"'", this, null);
                }
            }
            posEnd = pos - 1;
            // determine what name maps to
            final int len = posEnd - posStart;
            if(len == 2 && buf[posStart] == 'l' && buf[posStart+1] == 't') {
                if(tokenize) {
                    text = "<";
                }
                charRefOneCharBuf[0] = '<';
                return charRefOneCharBuf;
            } else if(len == 3 && buf[posStart] == 'a'
                          && buf[posStart+1] == 'm' && buf[posStart+2] == 'p') {
                if(tokenize) {
                    text = "&";
                }
                charRefOneCharBuf[0] = '&';
                return charRefOneCharBuf;
            } else if(len == 2 && buf[posStart] == 'g' && buf[posStart+1] == 't') {
                if(tokenize) {
                    text = ">";
                }
                charRefOneCharBuf[0] = '>';
                return charRefOneCharBuf;
            } else if(len == 4 && buf[posStart] == 'a' && buf[posStart+1] == 'p'
                          && buf[posStart+2] == 'o' && buf[posStart+3] == 's')
            {
                if(tokenize) {
                    text = "'";
                }
                charRefOneCharBuf[0] = '\'';
                return charRefOneCharBuf;
            } else if(len == 4 && buf[posStart] == 'q' && buf[posStart+1] == 'u'
                          && buf[posStart+2] == 'o' && buf[posStart+3] == 't')
            {
                if(tokenize) {
                    text = "\"";
                }
                charRefOneCharBuf[0] = '"';
                return charRefOneCharBuf;
            } else {
                final char[] result = lookuEntityReplacement(len);
                if(result != null) {
                    return result;
                }
            }
            if(tokenize) text = null;
            return null;
        }
    }

    protected char[] lookuEntityReplacement(int entitNameLen)
        throws XmlPullParserException, IOException

    {
        if(!allStringsInterned) {
            final int hash = fastHash(buf, posStart, posEnd - posStart);
            LOOP:
            for (int i = entityEnd - 1; i >= 0; --i)
            {
                if(hash == entityNameHash[ i ] && entitNameLen == entityNameBuf[ i ].length) {
                    final char[] entityBuf = entityNameBuf[ i ];
                    for (int j = 0; j < entitNameLen; j++)
                    {
                        if(buf[posStart + j] != entityBuf[j]) continue LOOP;
                    }
                    if(tokenize) text = entityReplacement[ i ];
                    return entityReplacementBuf[ i ];
                }
            }
        } else {
            entityRefName = newString(buf, posStart, posEnd - posStart);
            for (int i = entityEnd - 1; i >= 0; --i)
            {
                // take advantage that interning for newStirng is enforced
                if(entityRefName == entityName[ i ]) {
                    if(tokenize) text = entityReplacement[ i ];
                    return entityReplacementBuf[ i ];
                }
            }
        }
        return null;
    }


    protected void parseComment()
        throws XmlPullParserException, IOException
    {
        // implements XML 1.0 Section 2.5 Comments

        //ASSUMPTION: seen <!-
        char ch = more();
        if(ch != '-') throw new XmlPullParserException(
                "expected <!-- for comment start", this, null);
        if(tokenize) posStart = pos;

        final int curLine = lineNumber;
        final int curColumn = columnNumber;
        try {
            final boolean normalizeIgnorableWS = tokenize == true && roundtripSupported == false;
            boolean normalizedCR = false;

            boolean seenDash = false;
            boolean seenDashDash = false;
            while(true) {
                // scan until it hits -->
                ch = more();
                if(seenDashDash && ch != '>') {
                    throw new XmlPullParserException(
                        "in comment after two dashes (--) next character must be >"
                            +" not "+printable(ch), this, null);
                }
                if(ch == '-') {
                    if(!seenDash) {
                        seenDash = true;
                    } else {
                        seenDashDash = true;
                        seenDash = false;
                    }
                } else if(ch == '>') {
                    if(seenDashDash) {
                        break;  // found end sequence!!!!
                    } else {
                        seenDashDash = false;
                    }
                    seenDash = false;
                } else {
                    seenDash = false;
                }
                if(normalizeIgnorableWS) {
                    if(ch == '\r') {
                        normalizedCR = true;
                        //posEnd = pos -1;
                        //joinPC();
                        // posEnd is already set
                        if(!usePC) {
                            posEnd = pos -1;
                            if(posEnd > posStart) {
                                joinPC();
                            } else {
                                usePC = true;
                                pcStart = pcEnd = 0;
                            }
                        }
                        //assert usePC == true;
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = '\n';
                    } else if(ch == '\n') {
                        if(!normalizedCR && usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = '\n';
                        }
                        normalizedCR = false;
                    } else {
                        if(usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = ch;
                        }
                        normalizedCR = false;
                    }
                }
            }

        } catch(EOFException ex) {
            throw new XmlPullParserException(
                "comment started on line "+curLine+" and column "+curColumn+" was not closed",
                this, ex);
        }
        if(tokenize) {
            posEnd = pos - 3;
            if(usePC) {
                pcEnd -= 2;
            }
        }
    }

    protected boolean parsePI()
        throws XmlPullParserException, IOException
    {
        if(tokenize) posStart = pos;
        final int curLine = lineNumber;
        final int curColumn = columnNumber;
        int piTargetStart = pos + bufAbsoluteStart;
        int piTargetEnd = -1;
        final boolean normalizeIgnorableWS = tokenize == true && roundtripSupported == false;
        boolean normalizedCR = false;

        try {
            boolean seenQ = false;
            char ch = more();
            if(isS(ch)) {
                throw new XmlPullParserException(
                    "processing instruction PITarget must be exactly after <? and not white space character",
                    this, null);
            }
            while(true) {
                // scan until it hits ?>
                //ch = more();

                if(ch == '?') {
                    seenQ = true;
                } else if(ch == '>') {
                    if(seenQ) {
                        break;  // found end sequence!!!!
                    }
                    seenQ = false;
                } else {
                    if(piTargetEnd == -1 && isS(ch)) {
                        piTargetEnd = pos - 1 + bufAbsoluteStart;

                        // [17] PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))
                        if((piTargetEnd - piTargetStart) == 3) {
                            if((buf[piTargetStart] == 'x' || buf[piTargetStart] == 'X')
                                   && (buf[piTargetStart+1] == 'm' || buf[piTargetStart+1] == 'M')
                                   && (buf[piTargetStart+2] == 'l' || buf[piTargetStart+2] == 'L')
                              )
                            {
                                if(piTargetStart > 3) {  //<?xml is allowed as first characters in input ...
                                    throw new XmlPullParserException(
                                        "processing instruction can not have PITarget with reserveld xml name",
                                        this, null);
                                } else {
                                    if(buf[piTargetStart] != 'x'
                                           && buf[piTargetStart+1] != 'm'
                                           && buf[piTargetStart+2] != 'l')
                                    {
                                        throw new XmlPullParserException(
                                            "XMLDecl must have xml name in lowercase",
                                            this, null);
                                    }
                                }
                                parseXmlDecl(ch);
                                if(tokenize) posEnd = pos - 2;
                                final int off = piTargetStart - bufAbsoluteStart + 3;
                                final int len = pos - 2 - off;
                                xmlDeclContent = newString(buf, off, len);
                                return false;
                            }
                        }
                    }
                    seenQ = false;
                }
                if(normalizeIgnorableWS) {
                    if(ch == '\r') {
                        normalizedCR = true;
                        //posEnd = pos -1;
                        //joinPC();
                        // posEnd is already set
                        if(!usePC) {
                            posEnd = pos -1;
                            if(posEnd > posStart) {
                                joinPC();
                            } else {
                                usePC = true;
                                pcStart = pcEnd = 0;
                            }
                        }
                        //assert usePC == true;
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = '\n';
                    } else if(ch == '\n') {
                        if(!normalizedCR && usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = '\n';
                        }
                        normalizedCR = false;
                    } else {
                        if(usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = ch;
                        }
                        normalizedCR = false;
                    }
                }
                ch = more();
            }
        } catch(EOFException ex) {
            throw new XmlPullParserException(
                "processing instruction started on line "+curLine+" and column "+curColumn
                    +" was not closed",
                this, ex);
        }
        if(piTargetEnd == -1) {
            piTargetEnd = pos - 2 + bufAbsoluteStart;
        }
        piTargetStart -= bufAbsoluteStart;
        piTargetEnd -= bufAbsoluteStart;
        if(tokenize) {
            posEnd = pos - 2;
            if(normalizeIgnorableWS) {
                --pcEnd;
            }
        }
        return true;
    }

    protected final static char[] VERSION = "version".toCharArray();
    protected final static char[] NCODING = "ncoding".toCharArray();
    protected final static char[] TANDALONE = "tandalone".toCharArray();
    protected final static char[] YES = "yes".toCharArray();
    protected final static char[] NO = "no".toCharArray();



    protected void parseXmlDecl(char ch)
        throws XmlPullParserException, IOException
    {
        preventBufferCompaction = true;
        bufStart = 0; // necessary to keep pos unchanged during expansion!

        ch = skipS(ch);
        ch = requireInput(ch, VERSION);
        ch = skipS(ch);
        if(ch != '=') {
            throw new XmlPullParserException(
                "expected equals sign (=) after version and not "+printable(ch), this, null);
        }
        ch = more();
        ch = skipS(ch);
        if(ch != '\'' && ch != '"') {
            throw new XmlPullParserException(
                "expected apostrophe (') or quotation mark (\") after version and not "
                    +printable(ch), this, null);
        }
        final char quotChar = ch;
        final int versionStart = pos;
        ch = more();
        while(ch != quotChar) {
            if((ch  < 'a' || ch > 'z') && (ch  < 'A' || ch > 'Z') && (ch  < '0' || ch > '9')
                   && ch != '_' && ch != '.' && ch != ':' && ch != '-')
            {
                throw new XmlPullParserException(
                    "<?xml version value expected to be in ([a-zA-Z0-9_.:] | '-')"
                        +" not "+printable(ch), this, null);
            }
            ch = more();
        }
        final int versionEnd = pos - 1;
        parseXmlDeclWithVersion(versionStart, versionEnd);
        preventBufferCompaction = false; // alow again buffer commpaction - pos MAY chnage
    }

    protected void parseXmlDeclWithVersion(int versionStart, int versionEnd)
        throws XmlPullParserException, IOException
    {
        String oldEncoding = this.inputEncoding;

        // check version is "1.0"
        if((versionEnd - versionStart != 3)
               || buf[versionStart] != '1'
               || buf[versionStart+1] != '.'
               || buf[versionStart+2] != '0')
        {
            throw new XmlPullParserException(
                "only 1.0 is supported as <?xml version not '"
                    +printable(new String(buf, versionStart, versionEnd - versionStart))+"'", this, null);
        }
        xmlDeclVersion = newString(buf, versionStart, versionEnd - versionStart);
        char ch = more();
        ch = skipS(ch);
        if(ch == 'e') {
            ch = more();
            ch = requireInput(ch, NCODING);
            ch = skipS(ch);
            if(ch != '=') {
                throw new XmlPullParserException(
                    "expected equals sign (=) after encoding and not "+printable(ch), this, null);
            }
            ch = more();
            ch = skipS(ch);
            if(ch != '\'' && ch != '"') {
                throw new XmlPullParserException(
                    "expected apostrophe (') or quotation mark (\") after encoding and not "
                        +printable(ch), this, null);
            }
            final char quotChar = ch;
            final int encodingStart = pos;
            ch = more();
            // [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
            if((ch  < 'a' || ch > 'z') && (ch  < 'A' || ch > 'Z'))
            {
                throw new XmlPullParserException(
                    "<?xml encoding name expected to start with [A-Za-z]"
                        +" not "+printable(ch), this, null);
            }
            ch = more();
            while(ch != quotChar) {
                if((ch  < 'a' || ch > 'z') && (ch  < 'A' || ch > 'Z') && (ch  < '0' || ch > '9')
                       && ch != '.' && ch != '_' && ch != '-')
                {
                    throw new XmlPullParserException(
                        "<?xml encoding value expected to be in ([A-Za-z0-9._] | '-')"
                            +" not "+printable(ch), this, null);
                }
                ch = more();
            }
            final int encodingEnd = pos - 1;
            inputEncoding = newString(buf, encodingStart, encodingEnd - encodingStart);
            ch = more();
        }

        ch = skipS(ch);
        if(ch == 's') {
            ch = more();
            ch = requireInput(ch, TANDALONE);
            ch = skipS(ch);
            if(ch != '=') {
                throw new XmlPullParserException(
                    "expected equals sign (=) after standalone and not "+printable(ch),
                    this, null);
            }
            ch = more();
            ch = skipS(ch);
            if(ch != '\'' && ch != '"') {
                throw new XmlPullParserException(
                    "expected apostrophe (') or quotation mark (\") after encoding and not "
                        +printable(ch), this, null);
            }
            char quotChar = ch;
            int standaloneStart = pos;
            ch = more();
            if(ch == 'y') {
                ch = requireInput(ch, YES);
                //Boolean standalone = new Boolean(true);
                xmlDeclStandalone = new Boolean(true);
            } else if(ch == 'n') {
                ch = requireInput(ch, NO);
                //Boolean standalone = new Boolean(false);
                xmlDeclStandalone = new Boolean(false);
            } else {
                throw new XmlPullParserException(
                    "expected 'yes' or 'no' after standalone and not "
                        +printable(ch), this, null);
            }
            if(ch != quotChar) {
                throw new XmlPullParserException(
                    "expected "+quotChar+" after standalone value not "
                        +printable(ch), this, null);
            }
            ch = more();
        }


        ch = skipS(ch);
        if(ch != '?') {
            throw new XmlPullParserException(
                "expected ?> as last part of <?xml not "
                    +printable(ch), this, null);
        }
        ch = more();
        if(ch != '>') {
            throw new XmlPullParserException(
                "expected ?> as last part of <?xml not "
                    +printable(ch), this, null);
        }

    }
    protected void parseDocdecl()
        throws XmlPullParserException, IOException
    {
        //ASSUMPTION: seen <!D
        char ch = more();
        if(ch != 'O') throw new XmlPullParserException(
                "expected <!DOCTYPE", this, null);
        ch = more();
        if(ch != 'C') throw new XmlPullParserException(
                "expected <!DOCTYPE", this, null);
        ch = more();
        if(ch != 'T') throw new XmlPullParserException(
                "expected <!DOCTYPE", this, null);
        ch = more();
        if(ch != 'Y') throw new XmlPullParserException(
                "expected <!DOCTYPE", this, null);
        ch = more();
        if(ch != 'P') throw new XmlPullParserException(
                "expected <!DOCTYPE", this, null);
        ch = more();
        if(ch != 'E') throw new XmlPullParserException(
                "expected <!DOCTYPE", this, null);
        posStart = pos;
        int bracketLevel = 0;
        final boolean normalizeIgnorableWS = tokenize == true && roundtripSupported == false;
        boolean normalizedCR = false;
        while(true) {
            ch = more();
            if(ch == '[') ++bracketLevel;
            if(ch == ']') --bracketLevel;
            if(ch == '>' && bracketLevel == 0) break;
            if(normalizeIgnorableWS) {
                if(ch == '\r') {
                    normalizedCR = true;
                    //posEnd = pos -1;
                    //joinPC();
                    // posEnd is alreadys set
                    if(!usePC) {
                        posEnd = pos -1;
                        if(posEnd > posStart) {
                            joinPC();
                        } else {
                            usePC = true;
                            pcStart = pcEnd = 0;
                        }
                    }
                    //assert usePC == true;
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = '\n';
                } else if(ch == '\n') {
                    if(!normalizedCR && usePC) {
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = '\n';
                    }
                    normalizedCR = false;
                } else {
                    if(usePC) {
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = ch;
                    }
                    normalizedCR = false;
                }
            }

        }
        posEnd = pos - 1;
    }

    protected void parseCDSect(boolean hadCharData)
        throws XmlPullParserException, IOException
    {
        char ch = more();
        if(ch != 'C') throw new XmlPullParserException(
                "expected <[CDATA[ for comment start", this, null);
        ch = more();
        if(ch != 'D') throw new XmlPullParserException(
                "expected <[CDATA[ for comment start", this, null);
        ch = more();
        if(ch != 'A') throw new XmlPullParserException(
                "expected <[CDATA[ for comment start", this, null);
        ch = more();
        if(ch != 'T') throw new XmlPullParserException(
                "expected <[CDATA[ for comment start", this, null);
        ch = more();
        if(ch != 'A') throw new XmlPullParserException(
                "expected <[CDATA[ for comment start", this, null);
        ch = more();
        if(ch != '[') throw new XmlPullParserException(
                "expected <![CDATA[ for comment start", this, null);

        final int cdStart = pos + bufAbsoluteStart;
        final int curLine = lineNumber;
        final int curColumn = columnNumber;
        final boolean normalizeInput = tokenize == false || roundtripSupported == false;
        try {
            if(normalizeInput) {
                if(hadCharData) {
                    if(!usePC) {
                        // posEnd is correct already!!!
                        if(posEnd > posStart) {
                            joinPC();
                        } else {
                            usePC = true;
                            pcStart = pcEnd = 0;
                        }
                    }
                }
            }
            boolean seenBracket = false;
            boolean seenBracketBracket = false;
            boolean normalizedCR = false;
            while(true) {
                // scan until it hits "]]>"
                ch = more();
                if(ch == ']') {
                    if(!seenBracket) {
                        seenBracket = true;
                    } else {
                        seenBracketBracket = true;
                        //seenBracket = false;
                    }
                } else if(ch == '>') {
                    if(seenBracket && seenBracketBracket) {
                        break;  // found end sequence!!!!
                    } else {
                        seenBracketBracket = false;
                    }
                    seenBracket = false;
                } else {
                    if(seenBracket) {
                        seenBracket = false;
                    }
                }
                if(normalizeInput) {
                    // deal with normalization issues ...
                    if(ch == '\r') {
                        normalizedCR = true;
                        posStart = cdStart - bufAbsoluteStart;
                        posEnd = pos - 1; // posEnd is alreadys set
                        if(!usePC) {
                            if(posEnd > posStart) {
                                joinPC();
                            } else {
                                usePC = true;
                                pcStart = pcEnd = 0;
                            }
                        }
                        //assert usePC == true;
                        if(pcEnd >= pc.length) ensurePC(pcEnd);
                        pc[pcEnd++] = '\n';
                    } else if(ch == '\n') {
                        if(!normalizedCR && usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = '\n';
                        }
                        normalizedCR = false;
                    } else {
                        if(usePC) {
                            if(pcEnd >= pc.length) ensurePC(pcEnd);
                            pc[pcEnd++] = ch;
                        }
                        normalizedCR = false;
                    }
                }
            }
        } catch(EOFException ex) {
            throw new XmlPullParserException(
                "CDATA section started on line "+curLine+" and column "+curColumn+" was not closed",
                this, ex);
        }
        if(normalizeInput) {
            if(usePC) {
                pcEnd = pcEnd - 2;
            }
        }
        posStart = cdStart - bufAbsoluteStart;
        posEnd = pos - 3;
    }

    protected void fillBuf() throws IOException, XmlPullParserException {
        if(reader == null) throw new XmlPullParserException(
                "reader must be set before parsing is started");

        if(bufEnd > bufSoftLimit) {
            boolean compact = bufStart > bufSoftLimit;
            boolean expand = false;
            if(preventBufferCompaction) {
                compact = false;
                expand = true;
            } else if(!compact) {
                //freeSpace
                if(bufStart < buf.length / 2) {
                    expand = true;
                } else {
                    // at least half of buffer can be reclaimed --> worthwhile effort!!!
                    compact = true;
                }
            }

            // if buffer almost full then compact it
            if(compact) {
                System.arraycopy(buf, bufStart, buf, 0, bufEnd - bufStart);
            } else if(expand) {
                final int newSize = 2 * buf.length;
                final char[] newBuf = new char[ newSize ];
                System.arraycopy(buf, bufStart, newBuf, 0, bufEnd - bufStart);
                buf = newBuf;
                if(bufLoadFactor > 0) {
                    bufSoftLimit = (int) (( ((long) bufLoadFactor) * buf.length ) /100);
                }

            } else {
                throw new XmlPullParserException("internal error in fillBuffer()");
            }
            bufEnd -= bufStart;
            pos -= bufStart;
            posStart -= bufStart;
            posEnd -= bufStart;
            bufAbsoluteStart += bufStart;
            bufStart = 0;
        }
        final int len = buf.length - bufEnd > READ_CHUNK_SIZE ? READ_CHUNK_SIZE : buf.length - bufEnd;
        final int ret = reader.read(buf, bufEnd, len);
        if(ret > 0) {
            bufEnd += ret;
            return;
        }
        if(ret == -1) {
            if(bufAbsoluteStart == 0 && pos == 0) {
                throw new EOFException("input contained no data");
            } else {
                if(seenRoot && depth == 0) {
                    reachedEnd = true;
                    return;
                } else {
                    StringBuffer expectedTagStack = new StringBuffer();
                    if(depth > 0) {
                        expectedTagStack.append(" - expected end tag");
                        if(depth > 1) {
                            expectedTagStack.append("s"); //more than one end tag
                        }
                        expectedTagStack.append(" ");
                        for (int i = depth; i > 0; i--)
                        {
                            String tagName = new String(elRawName[i], 0, elRawNameEnd[i]);
                            expectedTagStack.append("</").append(tagName).append('>');
                        }
                        expectedTagStack.append(" to close");
                        for (int i = depth; i > 0; i--)
                        {
                            if(i != depth) {
                                expectedTagStack.append(" and"); //more than one end tag
                            }
                            String tagName = new String(elRawName[i], 0, elRawNameEnd[i]);
                            expectedTagStack.append(" start tag <"+tagName+">");
                            expectedTagStack.append(" from line "+elRawNameLine[i]);
                        }
                        expectedTagStack.append(", parser stopped on");
                    }
                    throw new EOFException("no more data available"
                                               +expectedTagStack.toString()+getPositionDescription());
                }
            }
        } else {
            throw new IOException("error reading input, returned "+ret);
        }
    }

    protected char more() throws IOException, XmlPullParserException {
        if(pos >= bufEnd) {
            fillBuf();
            if(reachedEnd) {
                return (char)-1;
            }
        }
        final char ch = buf[pos++];
        if(ch == '\n') {
            ++lineNumber; columnNumber = 1;
        }
        else {
            ++columnNumber;
        }
        return ch;
    }

    protected void ensurePC(int end) {
        final int newSize = end > READ_CHUNK_SIZE ? 2 * end : 2 * READ_CHUNK_SIZE;
        final char[] newPC = new char[ newSize ];
        System.arraycopy(pc, 0, newPC, 0, pcEnd);
        pc = newPC;
    }

    protected void joinPC() {
        final int len = posEnd - posStart;
        final int newEnd = pcEnd + len + 1;
        if(newEnd >= pc.length) {
            ensurePC(newEnd);
        }
        System.arraycopy(buf, posStart, pc, pcEnd, len);
        pcEnd += len;
        usePC = true;

    }

    protected char requireInput(char ch, char[] input)
        throws XmlPullParserException, IOException
    {
        for (int i = 0; i < input.length; i++)
        {
            if(ch != input[i]) {
                throw new XmlPullParserException(
                    "expected "+printable(input[i])+" in "+new String(input)
                        +" and not "+printable(ch), this, null);
            }
            ch = more();
        }
        return ch;
    }

    protected char requireNextS()
        throws XmlPullParserException, IOException
    {
        final char ch = more();
        if(!isS(ch)) {
            throw new XmlPullParserException(
                "white space is required and not "+printable(ch), this, null);
        }
        return skipS(ch);
    }

    protected char skipS(char ch)
        throws XmlPullParserException, IOException
    {
        while(isS(ch)) { ch = more(); } // skip additional spaces
        return ch;
    }

    protected static final int LOOKUP_MAX = 0x400;
    protected static final char LOOKUP_MAX_CHAR = (char)LOOKUP_MAX;
    protected static boolean[] lookupNameStartChar = new boolean[ LOOKUP_MAX ];
    protected static boolean[] lookupNameChar = new boolean[ LOOKUP_MAX ];

    private static void setName(char ch)
    {
        lookupNameChar[ ch ] = true;
    }
    private static void setNameStart(char ch)
    {
        lookupNameStartChar[ ch ] = true; setName(ch);
    }

    static {
        setNameStart(':');
        for (char ch = 'A'; ch <= 'Z'; ++ch) setNameStart(ch);
        setNameStart('_');
        for (char ch = 'a'; ch <= 'z'; ++ch) setNameStart(ch);
        for (char ch = '\u00c0'; ch <= '\u02FF'; ++ch) setNameStart(ch);
        for (char ch = '\u0370'; ch <= '\u037d'; ++ch) setNameStart(ch);
        for (char ch = '\u037f'; ch < '\u0400'; ++ch) setNameStart(ch);

        setName('-');
        setName('.');
        for (char ch = '0'; ch <= '9'; ++ch) setName(ch);
        setName('\u00b7');
        for (char ch = '\u0300'; ch <= '\u036f'; ++ch) setName(ch);
    }

    //private final static boolean isNameStartChar(char ch) {
    protected boolean isNameStartChar(char ch) {
        return (ch < LOOKUP_MAX_CHAR && lookupNameStartChar[ ch ])
            || (ch >= LOOKUP_MAX_CHAR && ch <= '\u2027')
            || (ch >= '\u202A' &&  ch <= '\u218F')
            || (ch >= '\u2800' &&  ch <= '\uFFEF') ;

    }

    protected boolean isNameChar(char ch) {
        return (ch < LOOKUP_MAX_CHAR && lookupNameChar[ ch ])
            || (ch >= LOOKUP_MAX_CHAR && ch <= '\u2027')
            || (ch >= '\u202A' &&  ch <= '\u218F')
            || (ch >= '\u2800' &&  ch <= '\uFFEF')
                || ch=='@' || ch=='$';
    }

    protected boolean isS(char ch) {
        return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t');
    }

    protected String printable(char ch) {
        if(ch == '\n') {
            return "\\n";
        } else if(ch == '\r') {
            return "\\r";
        } else if(ch == '\t') {
            return "\\t";
        } else if(ch == '\'') {
            return "\\'";
        } if(ch > 127 || ch < 32) {
            return "\\u"+Integer.toHexString((int)ch);
        }
        return ""+ch;
    }

    protected String printable(String s) {
        if(s == null) return null;
        final int sLen = s.length();
        StringBuffer buf = new StringBuffer(sLen + 10);
        for(int i = 0; i < sLen; ++i) {
            buf.append(printable(s.charAt(i)));
        }
        s = buf.toString();
        return s;
    }
}


