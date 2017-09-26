package com.game.chaitanya.towersofhanoiremastered;

import android.annotation.SuppressLint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and
 * navigation/system bar) with user interaction.
 */
public class MainGame extends AppCompatActivity implements View.OnTouchListener {

  //TODO: Add a stop button while solving
  //TODO: Fix jankiness of solve button

  private int plateNum; //Number of active plates
  private View plates[]; //Array of all plate views, bottom plate first in list.
  private final ImageButton bases[] = new ImageButton[3]; //Array of all base views, Left base first in list.
  private TextView mainText;
  private TextView moveCountText;
  private HashMap<Integer, Integer> weightToIndex;
  private Button resetButton;
  private Button solveButton;
  private Button menuButton;
  private int moveCount;

  private LinkedList[] stacks = {new LinkedList<Integer>(), new LinkedList<Integer>(),
      new LinkedList<Integer>()}; //Array of backend representations of plates,
  // with bottom plate first in list.

  private int plateDrawables[] = {R.drawable.w0, R.drawable.w1, R.drawable.w2,
      R.drawable.w3, R.drawable.w4, R.drawable.w5, R.drawable.w6}; //Plate drawables
  private final float BASE_X_COORDS[] = new float[3]; //Horizontal hitbox bounds
  private final float Y_BOUNDS[] = new float[2]; //Vertical hitbox bounds
  private float firstPlateY = 0;

  private float initX; //Initial x value of any item that has been picked up
  private float initY; //Initial y value of any item that has been picked up
  private int initColNum; //Initial column of any item that has been picked up
  private float dX; //Change in x as item is dragged
  private float dY; //Change in y as item is dragged

  private final Handler mHideHandler = new Handler();
  private Handler mSolveHandler;
  private View mContentView;

  private boolean mVisible;
  private final Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      hide();
    }
  };

  private static int moveInfo[] = new int[3];
  private Thread solveThread;
  private boolean pickedUp;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main_game);

    mVisible = true;
    pickedUp = false;

    //Getting plateNum from intent.
    Intent intent = getIntent();
    plateNum = intent.getIntExtra("plateNum", 3);
    plates = new ImageButton[6];

    moveCount = 0;
    mainText = (TextView) findViewById(R.id.mainText);
    moveCountText = (TextView) findViewById(R.id.moveCountText);
    weightToIndex = new HashMap<Integer, Integer>();
    resetButton = (Button) findViewById(R.id.resetButton);
    solveButton = (Button) findViewById(R.id.solveButton);
    menuButton = (Button) findViewById(R.id.menuButton);

    //Buttons in array are ordered from bottom to top.
    plates[0] = (ImageButton) findViewById(R.id.imageButton0);
    plates[1] = (ImageButton) findViewById(R.id.imageButton1);
    plates[2] = (ImageButton) findViewById(R.id.imageButton2);
    plates[3] = (ImageButton) findViewById(R.id.imageButton3);
    plates[4] = (ImageButton) findViewById(R.id.imageButton4);
    plates[5] = (ImageButton) findViewById(R.id.imageButton5);

    //Housework for active plates.
    for (int i = 0; i < plateNum; i++) {
      plates[i].setOnTouchListener(this);
      ((ImageButton) plates[i]).setBackground(getDrawable(plateDrawables[plateNum - i]));
      plates[i].setId(plateNum - i); //ID is weight of plate
      stacks[0].add(plateNum - i); //Stacks contain weights
      weightToIndex.put(plateNum - i, i); //To get index number from weight.
    }

    //Making sure top plate is clickable
    plates[plateNum - 1].setClickable(true);

    //Creating objects for bases.
    bases[0] = (ImageButton) findViewById(R.id.lbase);
    bases[1] = (ImageButton) findViewById(R.id.cbase);
    bases[2] = (ImageButton) findViewById(R.id.rbase);

    mSolveHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        Bundle b = msg.getData();
        if (b.getString("command").equals("moveCount")) {
          incrementMoveCount();
        } else if (b.getString("command").equals("start")) {
          resetButton.setClickable(false);
          solveButton.setClickable(false);
          menuButton.setClickable(false);
          for (int i = 0; i < plateNum; i++) {
            plates[i].setClickable(false);
          }
          mainText.setText(" Solving...");
        } else if (b.getString("command").equals("end")) {
          resetButton.setClickable(true);
          solveButton.setClickable(true);
          menuButton.setClickable(true);
          endGame();
        }
      }
    };

    //Things that need to be done after views are displayed.
    bases[2].post(new Runnable() {
      @Override
      public void run() {
        //Setting Horizontal bounds of hitbox.
        for (int i = 0; i < 3; i++) {
          BASE_X_COORDS[i] = bases[i].getX();
        }
        //Setting Vertical Bounds of hitbox.
        Y_BOUNDS[0] = plates[plateNum - 1].getY() - (plates[0].getHeight() / 2); //Top bound
        Y_BOUNDS[1] = bases[1].getY() + (2 * plates[0].getHeight()); //Bottom bound
      }
    });
  }

  @Override
  public boolean onTouch(View v, MotionEvent e) {
    switch (e.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (moveCount == 0) {
          firstPlateY = plates[0].getY();
        }
        if(!pickedUp) {
          //On the very first turn, remember the y-value of bottom plate.
          pickedUp = true;
          initColNum = whichCol(v.getX() + (v.getWidth() / 2), v.getY() + (v.getHeight() / 2));
          System.out.println("initColNum: " + initColNum);

          for (int i = 0; i < 3; i++) {
            if (i != initColNum && !stacks[i].isEmpty()) {
              plates[weightToIndex.get(stacks[i].getLast())].setClickable(false);
            }
          }

          //Remember initial coords of item that has been picked up.
          initX = v.getX();
          initY = v.getY();

          dX = initX - e.getRawX();
          dY = initY - e.getRawY();
        }
        break;
      case MotionEvent.ACTION_MOVE:
        v.setX(e.getRawX() + dX);
        v.setY(e.getRawY() + dY);
        break;
      case MotionEvent.ACTION_UP:
        //Figure out which column item was dropped in.
        int colNum = whichCol(v.getX() + (v.getWidth() / 2), v.getY() + (v.getHeight() / 2));

        //If the move was legal
        if (colNum != 3 && isMoveLegal(v, colNum)) {
          //Make the top plate in the new column unclickable.
          if (!stacks[colNum].isEmpty()) {
            plates[weightToIndex.get(stacks[colNum].getLast())].setClickable(false);
          }

          moveTopPlate(initColNum, colNum);

          //Make the top plate in the previous column clickable.
          if (!stacks[initColNum].isEmpty()) {
            plates[weightToIndex.get(stacks[initColNum].getLast())].setClickable(true);
          }

          System.out.println("After move: ");
          for (int i = 0; i < 3; i++) {
            for (int j = 0; j < stacks[i].size(); j++) {
              System.out.print(stacks[i].get(j) + ",");
            }
            System.out.println("-");
          }

          //Update and display moveCount and mainText
          incrementMoveCount();
          mainText.setText(" ");

          if (winCheck()) {
            endGame();
          }

        } else { //If item is dropped in invalid location, put it back.
          v.setX(initX);
          v.setY(initY);
          mainText.setText(" Invalid Move");
        }
        for (int i = 0; i < 3; i++) {
          if (!stacks[i].isEmpty()) {
            plates[weightToIndex.get(stacks[i].getLast())].setClickable(true);
          }
        }
        pickedUp = false;
        break;
    }
    return false;
  }

  private void moveTopPlate(int oldColNum, int newColNum) {

    int movingPlate = (Integer) stacks[oldColNum].removeLast();
    stacks[newColNum].add(movingPlate);

    View v = plates[weightToIndex.get(movingPlate)];
    v.setX(BASE_X_COORDS[newColNum] + (bases[0].getWidth() / 2) - (v.getWidth() / 2));

    v.setY(calculatePlateY(newColNum));
  }

  private int calculatePlateY(int column) {
    int heights = 0;
    for (Object plateByWeight : stacks[column]) {
      heights += plates[weightToIndex.get(plateByWeight)].getHeight();
    }
    heights -= plates[weightToIndex.get(stacks[column].getFirst())].getHeight();
    return (int) (firstPlateY - heights);
  }

  private void incrementMoveCount() {
    moveCount++;
    moveCountText.setText(moveCount + " Moves ");
  }

  /**
   * Returns which column
   */
  private int whichCol(float x, float y) {

    if (y < Y_BOUNDS[0] || y > Y_BOUNDS[1]) {
      return 3;
    }
    for (int i = 0; i < 3; i++) {
      if (BASE_X_COORDS[i] <= x && x <= BASE_X_COORDS[i] + bases[0].getWidth()) {
        return i;
      }
    }

    return 3;
  }

  private boolean isMoveLegal(View v, int newCol) {
    //Check weights of item and target location.
    if (stacks[newCol].isEmpty()) {
      return true;
    } else {
      return v.getId() < (Integer) stacks[newCol].getLast();
    }
  }

  public boolean winCheck() {
    if (stacks[2].size() != plateNum) {
      return false;
    } else {
      for (int i = 0; i < plateNum; i++) {
        if ((int) stacks[2].get(i) != plateNum - i) {
          return false;
        }
      }
      return true;
    }
  }

  public void endGame() {
    mainText.setText(" Congrats! You won!");
    plates[weightToIndex.get(stacks[2].get(plateNum - 1))].setClickable(false);
  }

  public void solve(View view) {
    reset();

    solveThread = new Thread(new Runnable() {
      public void run() {
        Message m = Message.obtain();
        Bundle b = new Bundle();
        b.putString("command", "start");
        m.setData(b);
        mSolveHandler.sendMessage(m);

        moveStack(0, 2, plateNum);

        Message m1 = Message.obtain();
        b = new Bundle();
        b.putString("command", "end");
        m1.setData(b);
        mSolveHandler.sendMessage(m1);
      }
    });

    solveThread.start();
  }

  public void menu(View view) {
    reset();
    Intent intent = new Intent(this, TitleScreen.class);
    startActivity(intent);
  }


  public static int[] getMoveInfo() {
    return moveInfo;
  }

  public void reset(View view) {
    reset();
  }

  private void reset() {
    //If it's the very first turn, remember the y-value of bottom plate.
    if (moveCount == 0) {
      firstPlateY = plates[0].getY();
    }

    //Reset the backend structures
    for (int i = 0; i < 3; i++) {
      stacks[i].removeAll(stacks[i]);
    }

    //Reset the views
    for (int i = 0; i < plateNum; i++) {
      stacks[0].add(plateNum - i); //Stacks contain weights
      plates[i].setX(BASE_X_COORDS[0] + (bases[0].getWidth() / 2) - (plates[i].getWidth() / 2));
      plates[i].setY(calculatePlateY(0));
      plates[i].setClickable(false);
    }

    //Other housework
    plates[plateNum - 1].setClickable(true);
    mainText.setText(" Drag a plate to begin.");
    moveCount = 0;
    moveCountText.setText(moveCount + " Moves ");

  }

  public void moveStack(int moveFrom, int moveTo, int moveNum) {

    moveInfo[0] = moveFrom;
    moveInfo[1] = moveTo;
    moveInfo[2] = moveNum;

//    System.out.println("moveStack called.");

    if (stacks[moveFrom].isEmpty() || moveNum > stacks[moveFrom].size() || moveNum == 0
        || moveFrom == moveTo) {
//      System.out.println("Base Case reached.");
    } else {

//      System.out.println("Recursive Case Reached with " + moveNum);
      int otherStack = 3 - (moveFrom + moveTo);
      moveStack(moveFrom, otherStack, moveNum - 1);
//      System.out.println("Stack moved of size " + (moveNum - 1));

      moveTopPlate(moveFrom, moveTo);
      Message m = Message.obtain();
      Bundle b = new Bundle();
      b.putString("command", "moveCount");
      m.setData(b);
      mSolveHandler.sendMessage(m);
      try {
        Thread.sleep(300);
      } catch (InterruptedException ignore) {
      }

//      System.out.println("Top plate moved.");
      moveStack(otherStack, moveTo, moveNum - 1);
//      System.out.println("Once again Stack moved of size " + (moveNum - 1));
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    delayedHide(0);
  }

  private void hide() {
    // Hide UI first
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    mVisible = false;
  }

  @SuppressLint("InlinedApi")
  private void show() {
    // Show the system bar
    mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    mVisible = true;
  }

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }

  @Override
  public void onBackPressed() {
    moveTaskToBack(true);
  }


}


