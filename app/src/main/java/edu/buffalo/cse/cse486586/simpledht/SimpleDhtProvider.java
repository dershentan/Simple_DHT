package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.http.entity.ByteArrayEntity;

import static java.lang.Math.abs;

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] REMOTE_PORTS = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    static final int SERVER_PORT = 10000;

    private static String myPortStr = "";
    private static String predecessor = "";
    private static String successor = "";
    private static String myID = null;
    private static String predID = null;
    private static String succID = null;
    private HashMap<Integer,Integer> test1 = new HashMap<Integer, Integer>();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Context context = getContext();
        File path = new File(getContext().getFilesDir().getAbsolutePath());
        if (selection.equals("*")) {
            File list[] = path.listFiles();
            //Log.v("before*Delete", list.length+"");
            for( int i=0; i< list.length; i++)
            {
                context.deleteFile(list[i].getName());
            }
        } else if (selection.equals("@")) {
            File list[] = path.listFiles();
            //Log.v("before@Delete", list.length+"");
            for( int i=0; i< list.length; i++)
            {
                context.deleteFile(list[i].getName());
            }
        } else {
            context.deleteFile(selection);
        }

        //Log.v("afterDelete", path.getParent() + " : " +  path.listFiles().length);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        //ref: https://developer.android.com/training/data-storage/files.html
        String filename = values.getAsString("key"); // pass in key value to as filename
        String fileContents = values.getAsString("value"); // pass in value as a string
        FileOutputStream outputStream;
        try {
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        //Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager)this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        myPortStr = portStr;

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }

        String node_id = "";

        try {
            node_id = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Can't generate Hash for Node ID\n" + e.getMessage());
        }

        predecessor = portStr;
        successor = portStr;
        myID = node_id;
        predID = node_id;
        succID = node_id;

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Join", portStr, node_id);

        Log.v("oncreate", ByteBuffer.wrap(node_id.getBytes()).getInt() + "");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        if (selection.equals("*")) {
            File file = new File(getContext().getFilesDir().getAbsolutePath());
            File list[] = file.listFiles();
            for( int i=0; i< list.length; i++)
            {
                try {
                    FileInputStream inputStream = getContext().openFileInput(list[i].getName());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }

                    } finally {
                        reader.close();
                    }

                    //ref: https://www.codota.com/android/classes/android.database.MatrixCursor
                    //cursor = new MatrixCursor(new String[]{"key", "value"});
                    cursor.moveToFirst();
                    cursor.addRow(new String[]{list[i].getName(), stringBuilder.toString()});

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (selection.equals("@")){
            File file = new File(getContext().getFilesDir().getAbsolutePath());
            File list[] = file.listFiles();
            for( int i=0; i< list.length; i++)
            {
                try {
                    FileInputStream inputStream = getContext().openFileInput(list[i].getName());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }

                    } finally {
                        reader.close();
                    }

                    //ref: https://www.codota.com/android/classes/android.database.MatrixCursor
                    //cursor = new MatrixCursor(new String[]{"key", "value"});
                    cursor.moveToFirst();
                    cursor.addRow(new String[]{list[i].getName(), stringBuilder.toString()});

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                FileInputStream inputStream = getContext().openFileInput(selection);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                } finally {
                    reader.close();
                }

                //ref: https://www.codota.com/android/classes/android.database.MatrixCursor
                //cursor = new MatrixCursor(new String[]{"key", "value"});
                cursor.moveToFirst();
                cursor.addRow(new String[]{selection, stringBuilder.toString()});

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Log.v("query", selection);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        Log.v("genHash", input + " : " + formatter.toString());
        return formatter.toString();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            Socket socket = null;
            while(true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String inStr;
                try {
                    inStr = in.readLine();
                    if(inStr != null) {
                        publishProgress(inStr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String[] st = strings[0].split("\\t");
            if (st[0].trim().equals("Join")) {
                final String id2LookUp = st[2].trim();
                if ((ByteBuffer.wrap(id2LookUp.getBytes()).getInt()) > (ByteBuffer.wrap(predID.getBytes()).getInt())) {

                }
            }

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(final String... msgs) {
            if (msgs[0].trim().equals("Join") {
                try {
                    final String portStrFrom = msgs[0].trim();
                    final String nodeIDFrom = msgs[2].trim();
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT0));

                    String msgToSend = "Join" + "\t" + portStrFrom + "\t" + nodeIDFrom;
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    out.println(msgToSend);
                    //socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }

            }

            return null;
        }
    }
}
