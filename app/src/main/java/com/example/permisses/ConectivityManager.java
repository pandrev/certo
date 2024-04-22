package com.example.permisses;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ConectivityManager {
    public Tasks pendingOperation = null;
    public ConcurrentLinkedQueue<Tasks> operationQueue = new ConcurrentLinkedQueue<Tasks>();
    public Handler handler = new Handler();
    private Context ctx;

    private BLEController bleController;

    public static ConectivityManager instance;

    public ConectivityManager(Context ctx) {
        this.ctx = ctx;
    }

    public static ConectivityManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new ConectivityManager((ctx));
        }
        return instance;
    }

    public void startScan(Tasks task) {
        enqueueOperation(task);
    }

    public synchronized void enqueueOperation(Tasks task) {
        operationQueue.add(task);
        if(pendingOperation == null) {
            doNextOperation();
        }
    }

    public synchronized void doNextOperation() {
        if (pendingOperation != null) {
            Log.e("ConnectionManager", "an operation is pending! Aborting.");
            return;
        }

        Tasks operation = operationQueue.poll();

        if (operation == null) {
            Log.e("ConnectionManager", "The qeue is emppy!!!");
            return;
        }

        pendingOperation = operation;

        if (operation instanceof Scan) {
          
            //Log.d("LLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL", "LALKDLADALKDALKDLAKDLAKDLAKDLAKDLAKDLAKDLAKDLAKD");
        }


    }

    public synchronized void signalEndOfOperation() {
        //Log.d("ConnectionManager", "End of $pendingOperation");
        pendingOperation = null;
        if (!operationQueue.isEmpty()) {
            doNextOperation();
        }
    }
}
