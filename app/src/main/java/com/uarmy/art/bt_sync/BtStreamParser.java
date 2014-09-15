package com.uarmy.art.bt_sync;

import java.nio.charset.Charset;
import android.util.Log;
import java.nio.ByteBuffer;

/**
 * Created by a on 8/25/14.
 */
public class BtStreamParser {
    private static final String TAG = "BTStreamParser";

    private static final byte[] HEADER_MAGIC = {'D','T','A','0'};
    private static final byte[] CHECKSUM_MAGIC = {'E','D','T','A'};
    private static final int HEADER_LENGTH = HEADER_MAGIC.length;
    private static final int CHECKSUM_LENGTH = CHECKSUM_MAGIC.length;

    private static final String ENCODING = "UTF-8";

    private static final int READ_MAGIC = 0;
    private static final int MAGIC_READY = 1;
    private static final int READ_HEADER = 2;
    private static final int HEADER_READY = 3;
    private static final int READ_BODY = 4;
    private static final int READ_CHECKSUM = 5;
    private static final int CHECKSUM_READY = 6;
    private static final int END_PARSE = 7;
    private static final int DATA_ERROR = 8;

    private byte[] myData = null;
    private int myPayloadSize = 0;
    private int myState = READ_MAGIC;
    private int myOffset = 0;
    private byte[] tmp = {0,0,0,0};

    // hope this is safe and UTF-8 always there
    private final static Charset myCharset = Charset.availableCharsets().get("UTF-8");

    public BtStreamParser() {
        reset();
    }

    public void reset() {
        myState = READ_MAGIC;
        myOffset = 0;
        myData = null;
        myPayloadSize = 0;
    }

    public static byte[] encode(String msg) {
        //byte[] msg = {'D', 'T', 'A', '0', 0, 0, 0, 6, 'l','a','l','a','l', 'a', 'E','D','T','A' };

        int control_len = HEADER_LENGTH + CHECKSUM_LENGTH + 4; // 4 - sizeof(int);
        byte[] msg_bytes = msg.getBytes(myCharset);
        int msg_len = msg_bytes.length;

        byte[] raw = new byte[control_len + msg_len];
        ByteBuffer bb = ByteBuffer.wrap(raw);// can't use "allocateDirect", because in this case ByteBuffer is not backed by byte array
        bb.put(HEADER_MAGIC);
        bb.putInt(msg_len);
        bb.put(msg_bytes);
        bb.put(CHECKSUM_MAGIC);
        return raw;
    }

    public static String decode(byte[] b) {
        return myCharset.decode( ByteBuffer.wrap(b) ).toString();
    }

    public boolean IsComplete() { return myState == END_PARSE; }

    public String getDataAsString() {
        if(myData==null)
            return "";
        return decode(myData);
    }

    public byte[] getData() { return myData; }

    public int parse(byte[] bytes, int num_avail) {
        int start = 0;

        while(num_avail>0 && myState != END_PARSE)
        {
            switch(myState) {
                case DATA_ERROR:
                    Log.d(TAG, "DATA_ERROR");
                    Log.d(TAG, "tmp had:" + tmp.toString());
                    return -1;

                case READ_MAGIC:
                    if (myOffset < 4) {
                        int num2read = Math.min(4 - myOffset, num_avail);
                        for (int i = 0; i < num2read; ++i)
                            tmp[myOffset + i] = bytes[start + i];
                        start += num2read;
                        num_avail -= num2read;
                        myOffset += num2read;
                    }
                    if(myOffset==4) {
                        myState = MAGIC_READY;
                    }
                    break;

                case MAGIC_READY:
                    if(tmp[0]==HEADER_MAGIC[0] && tmp[1]==HEADER_MAGIC[1] && tmp[2]==HEADER_MAGIC[2] && tmp[3]==HEADER_MAGIC[3]) {
                        myState = READ_HEADER;
                        myOffset = 0; // free tmp
                        Log.d(TAG, "Header Magic - OK");
                    }
                    else
                        myState = DATA_ERROR;
                    break;

                case READ_HEADER:
                    if(myOffset<HEADER_LENGTH) {
                        int num2read = Math.min(HEADER_LENGTH - myOffset, num_avail);
                        for (int i = 0; i < num2read; ++i)
                            tmp[myOffset + i] = bytes[start + i];
                        start += num2read;
                        num_avail -= num2read;
                        myOffset += num2read;
                    }
                    if(myOffset==HEADER_LENGTH)
                        myState = HEADER_READY;
                    break;

                case HEADER_READY:
                    myPayloadSize = ByteBuffer.wrap(tmp).asIntBuffer().get(0);
                    if(myPayloadSize<0)
                        myState = DATA_ERROR;
                    else {
                        Log.d(TAG, "Header - OK");
                        myState = READ_BODY;
                        myData = new byte[myPayloadSize];
                        myOffset = 0;
                    }
                    break;

                case READ_BODY:
                    if(myOffset<myPayloadSize) {
                        int num2read = Math.min(myPayloadSize - myOffset, num_avail);
                        for (int i = 0; i < num2read; ++i)
                            myData[myOffset + i] = bytes[start + i];
                        start += num2read;
                        num_avail -= num2read;
                        myOffset += num2read;
                    }
                    if(myOffset==myPayloadSize) {
                        myOffset = 0;
                        myState = READ_CHECKSUM;
                    }
                    break;

                case READ_CHECKSUM:
                    if(myOffset<4) {
                        int num2read = Math.min(4 - myOffset, num_avail);
                        for (int i = 0; i < num2read; ++i)
                            tmp[myOffset + i] = bytes[start + i];
                        start += num2read;
                        num_avail -= num2read;
                        myOffset += num2read;
                    }
                    if(myOffset==CHECKSUM_LENGTH)
                    {
                        //myState = CHECKSUM_READY;
                        if(tmp[0]==CHECKSUM_MAGIC[0] && tmp[1]==CHECKSUM_MAGIC[1] && tmp[2]==CHECKSUM_MAGIC[2] && tmp[3]==CHECKSUM_MAGIC[3]) {
                            myState = END_PARSE;
                            myOffset = 0; // free tmp
                            Log.d(TAG, "Checksum - OK");
                        }
                        else
                            myState = DATA_ERROR;
                    }
                    break;

            }
        }

        if(myState == END_PARSE)
            return 0;

        return 1; // continue suck in data
    }
}
