package com.game.chaitanya.towersofhanoiremastered;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.content.Intent;

public class TitleScreen extends AppCompatActivity {

  //TODO: Fix UI


  private int plateNum;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_title_screen);

    NumberPicker np = (NumberPicker) findViewById(R.id.np);

    np.setMinValue(2);
    np.setMaxValue(6);
    np.setWrapSelectorWheel(false);

    plateNum = 3;
    np.setValue(plateNum);

    np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
      @Override
      public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        plateNum = newVal;
      }
    });
  }

  public void cont(View view) {
    Intent intent = new Intent(this, MainGame.class);

    intent.putExtra("plateNum", plateNum);
    startActivity(intent);
  }
}
