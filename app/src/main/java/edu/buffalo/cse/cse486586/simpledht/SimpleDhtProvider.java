package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;


class Node implements  Comparable<Node>{

    Node predecessor = null;
    Node successor = null;
    String node_id = null;
    String original_port = null;
    String original_id = null;

    Node(String node_id, Node predecessor, Node successor, String original_port, String original_id){
        this.predecessor = predecessor;
        this.successor = successor;
        this.node_id = node_id;
        this.original_port = original_port;
        this.original_id = original_id;
    }

    @Override
    public int compareTo(Node another) {
        if(this.node_id.compareTo(another.node_id)<0)
            return -1;
        else if(this.node_id.compareTo(another.node_id)>0)
            return 1;
        return 0;
    }

}

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final LinkedList<String> REMOTE_PORTS = new LinkedList<String>();
    static final int SERVER_PORT = 10000;
    static LinkedList<Node> nodeList = new LinkedList<Node>();
    static LinkedList<String> storedOriginalKeys = new LinkedList<String>();
    static String node_id = null;
    static String starString;
    static String headChord;

    static String my_id;
    static String predecessor;
    static String successor;
    static String predId;
    static String SuccId;
    static String queryVal;
    static String queryKey;
    boolean visited = false;

//-------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub

        String filename = selection;
        Log.i("key to delete",filename);
        String hash_key = null;
        try {
            hash_key = genHash(filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.i(TAG,"HASH_KEY :"+hash_key);
        Log.i(TAG,"My id :"+my_id);

        if(my_id != null && predId!= null && SuccId!= null){           //This part of the code executes after the first phase
            String predHash = null;
            try {
                predHash = genHash(predId);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            Log.i(TAG,"Pred HASH "+predId+" "+predHash);

            if(my_id.equals(headChord) &&
                    (hash_key.compareTo(my_id)<0 || hash_key.compareTo(predHash)>0)){
                deleteFunction(filename);
            } else if(hash_key.compareTo(my_id)<0 && hash_key.compareTo(predHash)>0){
                deleteFunction(filename);
            } else{
                //This part of the code is to be executed in order to forward the data in the chord
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, filename);
            }
        }else{ //This loop executes only for initial run
            deleteFunction(filename);
        }


        return 0;
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
//-------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        String filename = values.get("key").toString();
        Log.i("key",filename);
        String hash_key = null;
        try {
            hash_key = genHash(filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.i(TAG,"HASH_KEY :"+hash_key);
        Log.i(TAG,"My id :"+my_id);

        if(my_id != null && predId!= null && SuccId!= null){           //This part of the code executes after the first phase
            String predHash = null;
            try {
                predHash = genHash(predId);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            Log.i(TAG,"Pred HASH "+predId+" "+predHash);

            if(my_id.equals(headChord) &&
                    (hash_key.compareTo(my_id)<0 || hash_key.compareTo(predHash)>0)){
                insertFunction(filename, values);
            } else if(hash_key.compareTo(my_id)<0 && hash_key.compareTo(predHash)>0){
                insertFunction(filename, values);
            } else{
                //This part of the code is to be executed in order to forward the data in the chord
                String data = values.get("value").toString();
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, filename, data, successor,"Forwarding to next node");
            }
        }else{ //This loop executes only for initial run
            insertFunction(filename, values);
        }

        return uri;
    }

    //----------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub

        Log.i(TAG,"Ports adding");
        REMOTE_PORTS.add(REMOTE_PORT0);
        REMOTE_PORTS.add(REMOTE_PORT1);
        REMOTE_PORTS.add(REMOTE_PORT2);
        REMOTE_PORTS.add(REMOTE_PORT3);
        REMOTE_PORTS.add(REMOTE_PORT4);
        Log.i(TAG,"Ports added");

        Context context = getContext();

        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        Log.i(TAG,"Emulator number: "+portStr);
        try {
            node_id = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.i(TAG,"Hashed node id : "+node_id);

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.i(TAG,"Server socket created");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, node_id, myPort, portStr);
        return true;
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub

        Context ctxt = getContext();
        String[] columns = new String[]{"key","value"};
        MatrixCursor mc = new MatrixCursor(columns);
        Log.i("Query",selection);

        if(my_id != null && predId != null && SuccId!= null){

            if(selection.equals("@")){
                mc = localDump(ctxt, mc);
                Log.i(TAG,"LOCAL DUMP "+storedOriginalKeys.size());
                return mc;
            }

            if(selection.equals("*")){
                    mc = localDump(ctxt, mc);
                    Log.i(TAG,Integer.toString(mc.getCount()));
                    visited = true;
                    starString = "*:";
                    if (mc.getCount()!=0) {
                        mc.moveToFirst();
                        do {
                            for (int i = 0; i < mc.getColumnCount(); i++) {
                                starString += mc.getString(i)+":";
                            }
                        }while (mc.moveToNext());
                    }

                    Log.e(TAG,"String formed of size "+starString.split(":").length);
                    String temp = globalDump(starString);
                    String[] tempArray = temp.split(":");
                    Log.e(TAG,"FINALLY GOT QUERY :"+tempArray.length+ "  "+visited+ "  "+temp);
                    for(int i=1;i<tempArray.length-1;i+=2){
                        mc.addRow(new String[] { tempArray[i], tempArray[i+1] });
                    }

                    starString = null;
                    visited = false;
                    Log.i(TAG,Integer.toString(mc.getCount()));

                    return mc;
            }

            String hash_key = null;
            try {
                hash_key = genHash(selection);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            Log.i(TAG,"HASH_KEY :"+hash_key);
            Log.i(TAG,"My id :"+my_id);


            String predHash = null;
            try {
                predHash = genHash(predId);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            Log.i(TAG,"Pred HASH "+predId+" "+predHash);

            if(my_id.equals(headChord) &&
                    (hash_key.compareTo(my_id)<0 || hash_key.compareTo(predHash)>0)){
                mc = dump(ctxt, selection, mc);
                return mc;
            } else if(hash_key.compareTo(my_id)<0 && hash_key.compareTo(predHash)>0){
                mc = dump(ctxt, selection, mc);
                return mc;
            } else{
                mc = forwardQuery(selection, mc);
                return mc;
            }

        } else{
            if(selection.equals("*")){
                mc = localDump(ctxt, mc); //This has to be changed to globalDump
                return mc;
            } else if(selection.equals("@")){
                mc = localDump(ctxt, mc);
                return mc;
            } else{
                mc = dump(ctxt, selection, mc);
                return mc;
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

//----------------------------------------------------------------------------------------------------------------------------------------------------

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.e(TAG, "Server Task");
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            for (int i = 0; i < 9999; i++) {       //This loop ensures that messages can be sent 9999 times on both sides
                try {
                    Socket s1 = serverSocket.accept();
                    Log.e(TAG, "Listening...");
                    DataOutputStream dOS = new DataOutputStream(s1.getOutputStream());
                    DataInputStream dIS = new DataInputStream(s1.getInputStream());

                    String message = dIS.readUTF();
                    String array[] = message.split(":");

                    if(array[0].equals("*")){
                        Log.e(TAG,"Successor received flag :"+visited);
                        if(visited==false){
                            visited = true;
                            starString = "*:";
                            if(array.length>1){
                                for(int x=1;x<array.length;x++){
                                    starString += array[x]+":";
                                }

                                Log.e(TAG,"Message added "+starString.split(":").length);

                            }

                            Context ctxt = getContext();
                            String[] columns = new String[]{"key","value"};
                            MatrixCursor mc = new MatrixCursor(columns);

                            mc = localDump(ctxt, mc);

                            if (mc.getCount() != 0) {
                                mc.moveToFirst();
                                do {
                                    for (int x = 0; x < mc.getColumnCount(); x++) {
                                        starString += mc.getString(x)+":";
                                    }
                                }while (mc.moveToNext());
                            }

                            Log.e(TAG,"Message updated "+starString.split(":").length);

                            starString = globalDump(starString);
                            Log.i(TAG,"Sending Back");
                            dOS.writeUTF(starString);
                            visited = false;
                            starString = null;

                        } else{
                            starString = message;
                            Log.i(TAG,"Already reached so sending back");
                            dOS.writeUTF(starString);
                        }

                    } else if(array.length==1){
                        Cursor resultCursor;
                        Log.i(TAG,"Forwarded message for querying received");
                        String selection = array[0];
                        resultCursor = query(providerUri, null, selection, null, null);

                        if(resultCursor.getCount()==0){
                            dOS.writeUTF(queryKey+":"+queryVal);
                        }else{
                            int keyIndex = resultCursor.getColumnIndex("key");
                            int valueIndex = resultCursor.getColumnIndex("value");
                            resultCursor.moveToFirst();

                            String returnKey = resultCursor.getString(keyIndex);
                            String returnValue = resultCursor.getString(valueIndex);

                            Log.i(TAG,"Return value :"+returnValue);
                            dOS.writeUTF(returnKey+":"+returnValue);
                        }

                    } else if(array.length==2){

                        Log.i(TAG,"Forwarded message received");

                        String key = array[0];
                        String value = array[1];

                        ContentValues keyValueToInsert = new ContentValues();
                        keyValueToInsert.put("key",key);
                        keyValueToInsert.put("value",value);

                        insert(providerUri, keyValueToInsert);
                        dOS.writeUTF("Ack");

                    } else if(array.length ==4){

                        Log.i(TAG,"Forwarded message for deleting received");
                        String filename = array[0];
                        delete(providerUri,filename, null);
                        dOS.writeUTF("Ack");

                    } else if(array.length==6){                 //Main variables
                        my_id = array[0];  //Hashed emulator name of self
                        predecessor = array[1]; //Predecessor port num
                        successor = array[2];  //Successor port num
                        predId = array[3];  //Predecessor emulator name
                        SuccId = array[4];  //Successor emulator name
                        headChord = array[5];
                        Log.e(TAG,"ID:"+my_id+" Pred:"+predecessor+" Succ: "+successor+
                        " Pred ID :"+predId+" Succ Id: "+SuccId);

                        dOS.writeUTF("Ack");

                    } else if(array.length==3){                     //Node join request for avd0
                        String request_node = array[0];
                        String original_port = array[1];
                        String original_id = array[2];
                        Log.i(TAG,"Received node join request from "+array[0]);
                        Node node = new Node(request_node, null, null,original_port,original_id);
                        nodeList.add(node);

                        if(nodeList.size()>1){
                            for(int j=0;j<nodeList.size();j++){
                                Log.i(TAG,"Node :"+nodeList.get(j).node_id);
                            }

                            Collections.sort(nodeList, new Comparator<Node>() {
                                @Override
                                public int compare(Node lhs, Node rhs) {
                                    if(lhs.node_id.compareTo(rhs.node_id)<0)
                                        return -1;
                                    else if(lhs.node_id.compareTo(rhs.node_id)>0)
                                        return 1;
                                    return 0;
                                }
                            });

                            Collections.sort(nodeList);

                            for(int j=0;j<nodeList.size();j++){ //Code to set successor of node
                                if(j==nodeList.size()-1){
                                    nodeList.get(j).successor = nodeList.get(0);
                                    nodeList.get(0).predecessor = nodeList.get(j);
                                }
                                else{
                                    nodeList.get(j).successor = nodeList.get(j+1);
                                    nodeList.get(j+1).predecessor = nodeList.get(j);
                                }
                            }

                            headChord = nodeList.get(0).node_id;

                            Log.e(TAG,"AFTER SORTING!! Minimum "+headChord);
                            for(int j=0;j<nodeList.size();j++){
                                Log.i(TAG,"Node :"+nodeList.get(j).node_id+" Prev: "+nodeList.get(j).predecessor.original_id+" Successor: "+
                                        nodeList.get(j).successor.original_id+" Original "+nodeList.get(j).original_id);
                            }

                            dOS.writeUTF("Join");
                            sendBroadcast();

                        } else{
                            Log.e(TAG,"Initial message received");
                            dOS.writeUTF("Ack");
                        }

                    }else{
                        Log.e(TAG, "Message Received ");
                        dOS.writeUTF("Ack");
                    }

                    dOS.flush();
                    dIS.close();
                    dOS.close();
                    s1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;

        }
    }

//----------------------------------------------------------------------------------------------------------------------------------------------------

    private class ClientTask extends AsyncTask<String, Void, Void> {

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

        @Override
        protected Void doInBackground(String... msgs) {

            if(msgs.length==1) {

                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(successor));  //client socket connects to server socket by connecting to ip:port
                    Log.i(TAG,"Sending delete message to "+successor);
                    DataInputStream dIS = new DataInputStream(socket.getInputStream());
                    DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                    String filename = msgs[0];
                    dOut.writeUTF(filename+":a:b:c");
                    String ack;
                    ack = dIS.readUTF();

                    dOut.close();
                    dIS.close();

                    if(ack.equals("Ack")){
                        socket.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else if(msgs.length==4){
                String filename = msgs[0];
                String data = msgs[1];
                String remotePort = msgs[2];
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));  //client socket connects to server socket by connecting to ip:port
                    Log.i(TAG,"Sending message to "+remotePort);
                    DataInputStream dIS = new DataInputStream(socket.getInputStream());
                    DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                    dOut.writeUTF(filename+":"+data);
                    String ack;
                    ack = dIS.readUTF();

                    dOut.close();
                    dIS.close();

                    if(ack.equals("Ack")){
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else if(msgs.length==3){
                try {
                    Log.e(TAG,"Client Task");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT0));  //client socket connects to server socket by connecting to ip:port
                    String msgToSend = msgs[0]+":"+msgs[1]+":"+msgs[2];  //1 port number 2 emulator name
                    Log.i(TAG,"Message to send "+msgToSend);
                    DataInputStream dIS = new DataInputStream(socket.getInputStream());
                    DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                    dOut.writeUTF(msgToSend);
                    Log.e(TAG,"Message Sent");
                    dOut.flush();
                    String ack;
                    ack = dIS.readUTF();

                    dIS.close();
                    dOut.close();

                    if(ack.equals("Join")){
                        Log.i(TAG,"Node join to be initiated as ack has been received");
                        socket.close();
                    }else if(ack.equals("Ack")){
                        socket.close();
                        Log.i(TAG,ack + "Received");
                        Log.i(TAG,"Socket closed");
                    }

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }

            } else if(msgs.length==7){   //Broadcast by emulator 5554 to all live emulators!
                try {
                    Log.e(TAG,"Client Task");

                    String nodeToSend = msgs[0];
                    String previous = msgs[1];
                    String next = msgs[2];
                    String remotePort = msgs[3];
                    String idPrev = msgs[4];
                    String idSucc = msgs[5];
                    headChord = msgs[6];

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));  //client socket connects to server socket by connecting to ip:port
                    DataInputStream dIS = new DataInputStream(socket.getInputStream());
                    DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                    dOut.writeUTF(nodeToSend+":"+previous+":"+next+":"+idPrev+":"+idSucc+":"+headChord);
                    dOut.flush();
                    String ack;
                    ack = dIS.readUTF();

                    dOut.close();
                    dIS.close();

                    if(ack.equals("Ack")){
                        socket.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

//----------------------------------------------------------------------------------------------------------------------------------------------------

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------

    public void insertFunction(String filename, ContentValues values){
        String data = values.get("value").toString();
        Log.i("value",data);
        Context context = getContext();

        try{
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            storedOriginalKeys.add(filename);
            fos.write(data.getBytes());
            Log.i("Insert","Inserted "+data+" successfully where key is "+filename);
            fos.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------

    public MatrixCursor localDump(Context ctxt, MatrixCursor mc){
        for(int i=0; i<storedOriginalKeys.size(); i++){
            try {
                String selection = storedOriginalKeys.get(i);
                FileInputStream fIS = ctxt.openFileInput(selection);
                InputStreamReader iSR = new InputStreamReader(fIS);
                BufferedReader bReader = new BufferedReader(iSR);
                String finalS = bReader.readLine();
                Log.i(TAG,"Returned : "+storedOriginalKeys.get(i)+" : "+finalS);
                mc.addRow(new String[] { selection, finalS });
                if(i==storedOriginalKeys.size()-1)
                    return mc;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  mc;
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------

    public String globalDump(String starString){

        Log.e(TAG,"ENTERED GLOBAL DUMP send to"+successor);

        try {
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(successor));
            DataInputStream dIS = new DataInputStream(socket.getInputStream());
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.writeUTF(starString);
            starString = dIS.readUTF();

            dOut.flush();
            dIS.close();
            dOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG,"Returning back dump ");
        return starString;
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------

    public MatrixCursor dump(Context ctxt, String selection, MatrixCursor mc){
        try {
            FileInputStream fIS = ctxt.openFileInput(selection);
            InputStreamReader iSR = new InputStreamReader(fIS);
            BufferedReader bReader = new BufferedReader(iSR);
            String finalS = bReader.readLine();
            Log.i(TAG,"Returned : "+finalS);
            mc.addRow(new String[] { selection, finalS });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mc;
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------

    public MatrixCursor forwardQuery(String selection, MatrixCursor mc){
        try {
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(successor));

            DataInputStream dIS = new DataInputStream(socket.getInputStream());
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.writeUTF(selection);
            String mainStr = dIS.readUTF();
            String stringArray[] = mainStr.split(":");
            mc.addRow(new String[] { stringArray[0], stringArray[1] });

            dOut.flush();
            dIS.close();
            dOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return mc;
    }

//----------------------------------------------------------------------------------------------------------------------------------------------------

    public void deleteFunction(String filename){
        Log.e(TAG,"INSIDE DELETEFUNCTION TO DELETE "+filename);
        Context context = getContext();
        context.deleteFile(filename);
    }

//----------------------------------------------------------------------------------------------------------------------------------------------------

    public void sendBroadcast(){
        for(int i=0;i<nodeList.size();i++){
            Log.i(TAG,"Send broadcast to "+nodeList.get(i).original_port);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, nodeList.get(i).node_id,
                    nodeList.get(i).predecessor.original_port, nodeList.get(i).successor.original_port, nodeList.get(i).original_port,
                    nodeList.get(i).predecessor.original_id, nodeList.get(i).successor.original_id,headChord);
        }
    }
}

//----------------------------------------------------------------------------------------------------------------------------------------------------
