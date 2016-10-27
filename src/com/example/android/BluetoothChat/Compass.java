package com.example.android.BluetoothChat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
  
public class Compass extends View {
  
 private float direction;
   
 public Compass(Context context) {
  super(context);
  // TODO Auto-generated constructor stub
 }
  
 public Compass(Context context, AttributeSet attrs) {
  super(context, attrs);
  // TODO Auto-generated constructor stub
 }
  
 public Compass(Context context, AttributeSet attrs, int defStyle) {
  super(context, attrs, defStyle);
  // TODO Auto-generated constructor stub
 }
  
 @Override
 protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
  setMeasuredDimension(
    MeasureSpec.getSize(widthMeasureSpec),
    MeasureSpec.getSize(heightMeasureSpec));
 }
  
 @SuppressLint("DrawAllocation") @Override
 protected void onDraw(Canvas canvas) {
    
  int w = getMeasuredWidth();
  int h = getMeasuredHeight();
  int r;
  if(w > h){
   r = h/2;
  }else{
   r = w/2;
  }
    
  Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  paint.setStyle(Paint.Style.STROKE);
  paint.setStrokeWidth(15);
  paint.setColor(Color.WHITE);
    
  canvas.drawCircle(w/2, h/2, (float) (r*.9), paint);
    
  paint.setColor(Color.RED);
  
  Paint paint1=new Paint(Paint.ANTI_ALIAS_FLAG);
  paint1.setStyle(Paint.Style.FILL_AND_STROKE);
  paint1.setStrokeWidth(5);
  paint1.setColor(Color.RED);
  
  
  
  canvas.drawCircle((float) (w/2+r* Math.sin(-direction)*.9), (float) (h/2-5 - r * Math.cos(-direction)*.9), r/10, paint1);
 // canvas.drawLine(startX, startY, stopX, stopY, paint)
 /* canvas.drawLine(
    w/2,
    h/2,
    (float)(w/2 + r * Math.sin(-direction)),
    (float)(h/2 - r * Math.cos(-direction)),
    paint);
  
 }
 */
 }   
 public void update(float matrixValues){
  direction = matrixValues;
  invalidate();
 }
   
}