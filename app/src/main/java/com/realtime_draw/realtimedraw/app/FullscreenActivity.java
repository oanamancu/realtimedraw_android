package com.realtime_draw.realtimedraw.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.realtime_draw.realtimedraw.app.filesys.DrawingEncoder;
import com.realtime_draw.realtimedraw.app.filesys.DrawingToolBrush;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class FullscreenActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "com.realtime_draw.realtimedraw";
    private ImageButton currPaint, drawBtn, clearBtn, opacityBtn;
    private final WebSocketConnection mConnection = new WebSocketConnection();
    private int currentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        currentLayout = R.layout.home;
    }

    public void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(FullscreenActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void watch(View view) {
        setContentView(R.layout.watching_view);
        currentLayout = R.layout.watching_view;
        try {
            FileInputStream input = openFileInput("abc.rec");
            WatchingView watchingView = (WatchingView) findViewById(R.id.watching_view);
            watchingView.setActivity(this);
            watchingView.play(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play(View view){
        WatchingView watchingView = (WatchingView) findViewById(R.id.watching_view);
        if(watchingView.isFinished()){
            watchingView.stop();
            setContentView(R.layout.home);
            return;
        }
        watchingView.pause();
    }

    public void draw_now(View view) {
        setContentView(R.layout.drawing_view);
        currentLayout = R.layout.drawing_view;
        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paint_colors_row2);
        currPaint = (ImageButton) paintLayout.getChildAt(3);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
        drawBtn = (ImageButton) findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);
        DrawingView drawingView = (DrawingView) findViewById(R.id.drawing_view);
        drawingView.setBrushSize(DrawingToolBrush.SMALL);
        clearBtn = (ImageButton) findViewById(R.id.clear_btn);
        clearBtn.setOnClickListener(this);
        opacityBtn = (ImageButton) findViewById(R.id.opacity_btn);
        opacityBtn.setOnClickListener(this);
        drawingView.isRecording = true;
    }

    public void paintClicked(View view) {
        DrawingView drawingView = (DrawingView) findViewById(R.id.drawing_view);
        //use chosen color
        if (view != currPaint) {//update color
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            drawingView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint = (ImageButton) view;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View view) {//respond to clicks
        final DrawingView drawingView = (DrawingView) findViewById(R.id.drawing_view);
        if (view.getId() == R.id.draw_btn) {//draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBtn = (ImageButton) brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawingView.setBrushSize(DrawingToolBrush.SMALL);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton) brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawingView.setBrushSize(DrawingToolBrush.MEDIUM);
                    brushDialog.dismiss();
                }
            });

            ImageButton largeBtn = (ImageButton) brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawingView.setBrushSize(DrawingToolBrush.LARGE);
                    brushDialog.dismiss();
                }
            });

            brushDialog.show();
        } else if (view.getId() == R.id.clear_btn) {
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("Clear screen");
            newDialog.setMessage("This will paint white to all the screen");
            newDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    drawingView.clearScreen();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            newDialog.show();
        } else if (view.getId() == R.id.opacity_btn) {
            //launch opacity chooser
            final Dialog seekDialog = new Dialog(this);
            seekDialog.setTitle("Opacity level:");
            seekDialog.setContentView(R.layout.opacity_chooser);
            final TextView seekTxt = (TextView) seekDialog.findViewById(R.id.opq_txt);
            final SeekBar seekOpq = (SeekBar) seekDialog.findViewById(R.id.opacity_seek);
            seekOpq.setMax(100);
            int currLevel = drawingView.getPaintAlpha();
            seekTxt.setText(currLevel + "%");
            seekOpq.setProgress(currLevel);
            seekOpq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    seekTxt.setText(Integer.toString(progress) + "%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            Button opqBtn = (Button) seekDialog.findViewById(R.id.opq_ok);
            opqBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawingView.setPaintAlpha(seekOpq.getProgress());
                    seekDialog.dismiss();
                }
            });
            seekDialog.show();

        }

    }

    public void togglePlayButton(final Drawable drawable){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageButton)findViewById(R.id.playButton)).setImageDrawable(drawable);
            }
        });
    }

    private void testDrawing() {
        try {
            Thread.sleep(1000);
            System.out.println("Testing...");
            Bitmap bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream enc_out = new ByteArrayOutputStream();
            DrawingEncoder encoder = new DrawingEncoder(enc_out, bitmap);
            System.out.println("Starting encoder...");
            encoder.start();
            long start = System.nanoTime();

            for (short j = 0; j < 600; ++j) {
                for (short i = 0; i < 100; ++i) {
                    //DrawingAction action = new DrawingActionUseCoord(i, j);
                    //encoder.queueAction(j * 1000 + i, action);
                }
            }
            encoder.queueEOS();

            encoder.join();
            long end = System.nanoTime();
            System.out.println("Finished encoding witihin " + ((end - start) / 1000000) + " milliseconds");
            System.out.println("Output size is " + enc_out.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testws() {
        final String wsuri = "ws://192.168.1.132:9000";

        try {
            mConnection.connect(wsuri, new WebSocketHandler() {

                @Override
                public void onOpen() {
                    Log.d(TAG, "Status: Connected to " + wsuri);
                    mConnection.sendTextMessage("Hello, world!");
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG, "Got echo: " + payload);
                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d(TAG, "Connection lost.");
                }
            });
        } catch (WebSocketException e) {

            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onBackPressed() {
        switch (currentLayout){
            case R.layout.home:
                super.onBackPressed();
                break;
            case R.layout.watching_view:
                WatchingView watchingView = (WatchingView) findViewById(R.id.watching_view);
                watchingView.stop();
                setContentView(R.layout.home);
                break;
            case R.layout.drawing_view:
                try {
                    FileOutputStream out = openFileOutput("abc.rec", MODE_PRIVATE);
                    final DrawingView drawingView = (DrawingView) findViewById(R.id.drawing_view);
                    out.write(drawingView.stopRecording());
                    out.flush();
                    out.close();
                    drawingView.destroyDrawingCache();
                    showToast("Drawing saved to Gallery.");
                } catch (Throwable e) {
                    showToast("Recording could not be saved!");
                    e.printStackTrace();
                }
                setContentView(R.layout.home);
                break;
        }
    }
}
