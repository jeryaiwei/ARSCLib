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
package com.reandroid.arsc.io;

import com.reandroid.arsc.header.InfoHeader;
import com.reandroid.arsc.header.SpecHeader;
import com.reandroid.arsc.header.TypeHeader;

import java.io.*;


public class BlockReader extends InputStream {
    private final Object mLock=new Object();
    private byte[] BUFFER;
    private final int mStart;
    private final int mLength;
    private int mPosition;
    private boolean mIsClosed;
    private int mMark;
    public BlockReader(byte[] buffer, int start, int length) {
        this.BUFFER=buffer;
        this.mStart=start;
        this.mLength=length;
        this.mPosition =0;
    }
    public BlockReader(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }
    public BlockReader(InputStream in) throws IOException {
        this(loadBuffer(in));
    }
    public BlockReader(InputStream in, int length) throws IOException {
        this(loadBuffer(in, length));
    }
    public BlockReader(File file) throws IOException {
        this(loadBuffer(file));
    }
    public int readUnsignedShort() throws IOException {
        return 0x0000ffff & readShort();
    }
    public short readShort() throws IOException {
        int pos = getPosition();
        byte[] bts = new byte[2];
        readFully(bts);
        seek(pos);
        return toShort(bts, 0);
    }
    public SpecHeader readSpecHeader() throws IOException{
        SpecHeader specHeader = new SpecHeader();
        if(available() < specHeader.countBytes()){
            return null;
        }
        int pos = getPosition();
        specHeader.readBytes(this);
        seek(pos);
        return specHeader;
    }
    public TypeHeader readTypeHeader() throws IOException{
        TypeHeader typeHeader = new TypeHeader(false);
        if(available() < typeHeader.getMinimumSize()){
            return null;
        }
        int pos = getPosition();
        typeHeader.readBytes(this);
        seek(pos);
        return typeHeader;
    }
    public InfoHeader readHeaderBlock() throws IOException {
        InfoHeader infoHeader = new InfoHeader();
        if(available() < infoHeader.getMinimumSize()){
            return null;
        }
        int pos = getPosition();
        infoHeader.readBytes(this);
        seek(pos);
        return infoHeader;
    }
    public int searchNextIntPosition(int bytesOffset, int value){
        if(mIsClosed || mPosition>=mLength){
            return -1;
        }
        synchronized (mLock){
            int actPos=mStart+mPosition+bytesOffset;
            int max=available()/4;
            for(int i=0;i<max;i++){
                int pos=actPos+(i*4);
                int valCur=toInt(BUFFER, pos);
                if(valCur==value){
                    return pos-mStart;
                }
            }
            return -1;
        }
    }
    private int toInt(byte[] bts, int offset){
        return bts[offset] & 0xff |
                (bts[offset+1] & 0xff) << 8 |
                (bts[offset+2] & 0xff) << 16 |
                (bts[offset+3] & 0xff) << 24;
    }
    private short toShort(byte[] bts, int offset){
        return (short) (bts[offset] & 0xff |
                (bts[offset+1] & 0xff) << 8);
    }
    public byte[] getBytes(){
        int len = length();
        if(this.BUFFER.length == len){
            return BUFFER;
        }
        byte[] bytes = new byte[len];
        if(len==0){
            return bytes;
        }
        System.arraycopy(BUFFER, mStart, bytes, 0, len);
        return bytes;
    }
    public BlockReader create(int len){
        return create(getPosition(), len);
    }
    public BlockReader create(int start, int len){
        int max = start + len;
        if(len < 0 || max > this.mLength){
            len = this.mLength - start;
        }
        start = start + this.mStart;
        return new BlockReader(BUFFER, start, len);
    }
    public boolean isAvailable(){
        if(mIsClosed){
            return false;
        }
        return available()>0;
    }
    public void offset(int off){
        int pos=getPosition()+off;
        seek(pos);
    }
    public void seek(int relPos){
        if(relPos<0){
            relPos=0;
        }else if(relPos>length()){
            relPos=length();
        }
        setPosition(relPos);
    }
    private void setPosition(int pos){
        if(pos==mPosition){
            return;
        }
        synchronized (mLock){
            mPosition=pos;
        }
    }
    public int length(){
        return mLength;
    }
    public byte[] readBytes(int len) throws IOException {
        byte[] result=new byte[len];
        if(len==0){
            return result;
        }
        int len2=read(result);
        if(len2<0){
            throw new EOFException("Finished reading: "+ mPosition);
        }
        if(len==len2){
            return result;
        }
        byte[] result2=new byte[len2];
        System.arraycopy(result, 0, result2, 0, len2);
        return result2;
    }
    public int readFully(byte[] bts) throws IOException{
        return readFully(bts, 0, bts.length);
    }
    public int readFully(byte[] bts, int length) throws IOException{
        if(length==0){
            return 0;
        }
        return readFully(bts, 0, length);
    }

    public int readFully(byte[] bts, int start, int length) throws IOException {
        if(length==0){
            return 0;
        }
        if(mIsClosed){
            throw new IOException("Stream is closed");
        }
        if(mPosition>=mLength){
            throw new EOFException("Finished reading: "+mPosition);
        }
        int len=bts.length;
        if(length<len){
            len=length;
        }
        synchronized (mLock){
            int actPos=mStart+mPosition;
            int i;
            for(i=0;i<len;i++){
                bts[start+i] = BUFFER[actPos+i];
                mPosition++;
                if(mPosition>=mLength){
                    i++;
                    break;
                }
            }
            return i;
        }
    }
    public int getPosition(){
        return mPosition;
    }
    public int getActualPosition(){
        return mStart + mPosition;
    }
    public int getStartPosition(){
        return mStart;
    }
    @Override
    public int read() throws IOException {
        if(mIsClosed){
            throw new IOException("Stream is closed");
        }
        int i=mPosition;
        if(i>=mLength){
            throw new EOFException("Finished reading: "+i);
        }
        synchronized (mLock){
            int actPos=mStart+i;
            int val=BUFFER[actPos] & 0xff;
            mPosition++;
            return val;
        }
    }
    @Override
    public void mark(int pos){
        mMark=pos;
    }
    @Override
    public int available(){
        return mLength-mPosition;
    }
    @Override
    public void reset() throws IOException{
        if(mIsClosed){
            throw new IOException("Can not reset stream is closed");
        }
        mPosition=mMark;
    }
    @Override
    public void close(){
        mIsClosed=true;
        BUFFER=null;
        mMark=0;
    }
    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(": ");
        if(mIsClosed){
            builder.append("Closed");
        }else{
            int av=available();
            if(av==0){
                builder.append("Finished: ");
                builder.append(getPosition());
            }else {
                if(mStart>0){
                    builder.append("START=");
                    builder.append(mStart);
                    builder.append(", ACTUAL=");
                    builder.append(getActualPosition());
                    builder.append(", ");
                }
                builder.append("POS=");
                builder.append(getPosition());
                builder.append(", available=");
                builder.append(av);
            }
        }
        return builder.toString();
    }


    private static byte[] loadBuffer(File file) throws IOException {
        FileInputStream in=new FileInputStream(file);
        byte[] result = loadBuffer(in);
        in.close();
        return result;
    }
    private static byte[] loadBuffer(InputStream in) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buff=new byte[40960];
        int len;
        while((len=in.read(buff))>0){
            outputStream.write(buff, 0, len);
        }
        if(in instanceof FileInputStream){
            in.close();
        }
        outputStream.close();
        return outputStream.toByteArray();
    }
    private static byte[] loadBuffer(InputStream in, int length) throws IOException {
        byte[] buff=new byte[length];
        if(length==0){
            return buff;
        }
        int readLength = in.read(buff, 0, length);
        if(readLength < length){
            throw new IOException("Read length is less than expected: length="
                    +length+", read="+readLength);
        }
        return buff;
    }
    public static InfoHeader readHeaderBlock(File file) throws IOException{
        return InfoHeader.readHeaderBlock(file);
    }
    public static InfoHeader readHeaderBlock(InputStream inputStream) throws IOException{
        return InfoHeader.readHeaderBlock(inputStream);
    }
    public static InfoHeader readHeaderBlock(byte[] bytes) throws IOException{
        return InfoHeader.readHeaderBlock(bytes);
    }

    private static final int MAX_FILE_SIZE = 1024 * 1000 * 40;
}
