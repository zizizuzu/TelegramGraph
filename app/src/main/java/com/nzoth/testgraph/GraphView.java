package com.nzoth.testgraph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GraphView extends View {
    private static final int TABLE_HORIZONTAL_LINE_COUNT = 6;
    private static final int TABLE_TEXT_PADDING = 20;
    private static final int TABLE_TEXT_SIZE = 30;

    private static final int DATE_BLOCK_HEIGHT = 200;

    private GraphItem graphItem;

    private int screenWidth;
    private int screenHeight;

    private int lineWeight = 100;

    private Paint linePaint;
    private Paint tablePaint;

    private Direction mCurrentScrollDirection = Direction.NONE;
    private GestureDetectorCompat gestureDetector;

    private float mXScrollingSpeed = 1f;
    private PointF mCurrentOrigin = new PointF(0f, 0f);
    private int mScaledTouchSlop;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.US);


    private enum Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    public GraphView(Context context, GraphItem graphItem) {
        super(context);
        this.graphItem = graphItem;
        this.gestureDetector = new GestureDetectorCompat(context, gestureListener);
        this.mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();


        initLinePaint();
        initTablePaint();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        screenWidth = getWidth();
        screenHeight = getHeight();
    }

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            switch (mCurrentScrollDirection) {
                case NONE: {
                    // Allow scrolling only in one direction.
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            mCurrentScrollDirection = Direction.LEFT;
                        } else {
                            mCurrentScrollDirection = Direction.RIGHT;
                        }
                    } else {
                        mCurrentScrollDirection = Direction.VERTICAL;
                    }
                    break;
                }
                case LEFT: {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && (distanceX < -mScaledTouchSlop)) {
                        mCurrentScrollDirection = Direction.RIGHT;
                    }
                    break;
                }
                case RIGHT: {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && (distanceX > mScaledTouchSlop)) {
                        mCurrentScrollDirection = Direction.LEFT;
                    }
                    break;
                }
            }


            // Calculate the new origin after scroll.
            switch (mCurrentScrollDirection) {
                case LEFT:
                case RIGHT:
                    mCurrentOrigin.x -= distanceX * mXScrollingSpeed;
                    ViewCompat.postInvalidateOnAnimation(GraphView.this);
                    break;
                case VERTICAL:
                    mCurrentOrigin.y -= distanceY;
                    ViewCompat.postInvalidateOnAnimation(GraphView.this);
                    break;
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {

            return true;
        }
    };


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        linePaint.setColor(Color.BLACK);
//        canvas.drawText(String.valueOf((int) mCurrentOrigin.x), 100, 100, linePaint);


        drawTable(canvas);
        drawLines(canvas);
    }

    private void drawTable(Canvas canvas) {
        float height = screenHeight - DATE_BLOCK_HEIGHT;
        float rowHeight = height / TABLE_HORIZONTAL_LINE_COUNT;

        float[] table = new float[TABLE_HORIZONTAL_LINE_COUNT * 4];
        for (int i = 0; i < TABLE_HORIZONTAL_LINE_COUNT; i++) {
            int position = i * 4;
            float y = (i + 1) * rowHeight;
            table[position] = 0;
            table[position + 1] = y;
            table[position + 2] = screenWidth;
            table[position + 3] = y;
        }

        tablePaint.setColor(Color.LTGRAY);
        canvas.drawLines(table, tablePaint);
    }

    private void drawTableYText(Canvas canvas, int maxYPoints) {
        float height = screenHeight - DATE_BLOCK_HEIGHT;
        float rowHeight = height / TABLE_HORIZONTAL_LINE_COUNT;
        int partOfPoints = maxYPoints / TABLE_HORIZONTAL_LINE_COUNT;

        tablePaint.setColor(Color.GRAY);
        for (int i = 0; i < TABLE_HORIZONTAL_LINE_COUNT; i++) {
            canvas.drawText(
                    FormatUtils.formatBigValue(i > 0 ? i * partOfPoints : 0),
                    TABLE_TEXT_PADDING,
                    (height - i * rowHeight) - TABLE_TEXT_PADDING,
                    tablePaint
            );
        }
    }

    private void drawTableDateText(
            Canvas canvas,
            float scrollX,
            int startScreenPoint,
            int endScreenPoint,
            int visibleDateValue
    ) {
        float height = screenHeight - DATE_BLOCK_HEIGHT + TABLE_TEXT_SIZE + TABLE_TEXT_PADDING;
        int startPosition = startScreenPoint - startScreenPoint % visibleDateValue;
        for (int i = 0; i < endScreenPoint - startPosition; i += visibleDateValue) {
            int rootArrayPosition = i + startPosition;
            float positionX = scrollX - (i > 0 ? lineWeight * rootArrayPosition : 0);

            Date date = new Date(graphItem.getDateList()[rootArrayPosition]);
            String text = dateFormat.format(date);
            float x = positionX - tablePaint.measureText(text) / 2;
            canvas.drawText(text, x, height, tablePaint);
        }
    }

    private void drawLines(Canvas canvas) {
        float scrollX = mCurrentOrigin.x + screenWidth;
        int listLength = graphItem.getDateList().length;

        int startScreenPoint = getPreviousPosition((int) Math.ceil(mCurrentOrigin.x / lineWeight));
        int endScreenPointTerm = (int) Math.ceil(scrollX / lineWeight) + 1;
        int endScreenPoint = listLength > endScreenPointTerm ? endScreenPointTerm : listLength;
        int toArrayCount = endScreenPoint - startScreenPoint;

        long[][] graphArray = graphItem.getGraphList();

        if (toArrayCount > 0) {
            float maxYPoints = getMaxYHeight(graphArray, startScreenPoint, endScreenPoint);

            for (int graphCount = 0; graphCount < graphArray.length; graphCount++) {
                String title = graphItem.getGraphTitleList()[graphCount];
                int color = Color.parseColor(graphItem.getGraphColorList()[graphCount]);

                float[] toArray = getChainArrayFromArray(graphArray[graphCount], toArrayCount, startScreenPoint, scrollX, maxYPoints);

                linePaint.setColor(color);
                canvas.drawLines(toArray, linePaint);

            }

            drawTableYText(canvas, (int) maxYPoints);
        }

        drawTableDateText(
                canvas,
                scrollX,
                startScreenPoint,
                endScreenPoint,
                2 //Todo нужно высчитать коефициент сжатия таблицы
        );
    }

    private float[] getChainArrayFromArray(
            long[] fromArray,
            int toArrayCount,
            int startScreenPoint,
            float scrollX,
            float maxYPoints
    ) {
        float height = screenHeight - DATE_BLOCK_HEIGHT;
        float itemY = height / maxYPoints;
        float[] toArray = new float[toArrayCount * 4];

        for (int i = 0; i < toArrayCount; i++) {
            int toArrayPosition = i * 4;
            int rootArrayPosition = i + startScreenPoint;
            int previousPosition = getPreviousPosition(rootArrayPosition);

            toArray[toArrayPosition] = scrollX - (previousPosition > 0 ? lineWeight * previousPosition : 0);
            toArray[toArrayPosition + 1] = height - (fromArray[previousPosition] * itemY);
            toArray[toArrayPosition + 2] = scrollX - (i > 0 ? lineWeight * rootArrayPosition : 0);
            toArray[toArrayPosition + 3] = height - (fromArray[rootArrayPosition] * itemY);
        }
        return toArray;
    }

//     private void drawLines(Canvas canvas) {
//        float scrollX = mCurrentOrigin.x + screenWidth;
//        int listLength = graphItem.getDateList().length;
//
//        int startScreenPoint = getPreviousPosition((int) Math.ceil(mCurrentOrigin.x / lineWeight));
//        int endScreenPointTerm = (int) Math.ceil(scrollX / lineWeight) + 1;
//        int endScreenPoint = listLength > endScreenPointTerm ? endScreenPointTerm : listLength;
//        int toArrayCount = endScreenPoint - startScreenPoint;
//
//        long[][] graphArray = graphItem.getGraphList();
//
//        if (toArrayCount > 0) {
//            float maxYPoints = getMaxYHeight(graphArray, startScreenPoint, endScreenPoint);
//
//            for (int i = 0; i < graphArray.length; i++) {
//                String title = graphItem.getGraphTitleList()[i];
//                int color = Color.parseColor(graphItem.getGraphColorList()[i]);
//
//                float[] toArray = getChainArrayFromArray(graphArray[i], toArrayCount, startScreenPoint, scrollX, maxYPoints);
//
//                linePaint.setColor(color);
//                canvas.drawLines(toArray, linePaint);
//            }
//
//            drawTableYText(canvas, (int) maxYPoints);
//        }
//    }

    private float getMaxYHeight(long[][] graphArray, int startScreenPoint, int endScreenPoint) {
        float maxY = 0;
        int toArrayCount = endScreenPoint - startScreenPoint;
        float scrollX = mCurrentOrigin.x + screenWidth;

        for (long[] array : graphArray) {

            int arrayLength = array.length;
            float startY1 = array[arrayLength - 1 > startScreenPoint ? startScreenPoint + 1 : arrayLength - 1];
            float startY2 = array[startScreenPoint];
            float startX3 = 1 - (mCurrentOrigin.x / lineWeight - startScreenPoint);
            float startPosition = getPointOnLine(startY1, startY2, startX3);
            maxY = maxY < startPosition ? startPosition : maxY;

            float endY1 = array[endScreenPoint >= 2 ? endScreenPoint - 2 : endScreenPoint];
            float endY2 = array[endScreenPoint >= 1 ? endScreenPoint - 1 : endScreenPoint];
            float endX3 = scrollX / lineWeight - (float) Math.floor(scrollX / lineWeight);
            float endPosition = getPointOnLine(endY1, endY2, endX3);
            maxY = maxY < endPosition ? endPosition : maxY;

            for (int i = 1; i < toArrayCount - 1; i++) {
                int fromArrayItem = (int) array[i + startScreenPoint];
                maxY = maxY < fromArrayItem ? fromArrayItem : maxY;
            }
        }

        return maxY;
    }

    private float getPointOnLine(float y1, float y2, float x3) {
        float x1 = 0F;
        float x2 = 1F;
        return (x3 - x1) * (y1 - y2) / (x1 - x2) + y1;
    }
//
//    private float[] getChainArrayFromArray(
//            long[] fromArray,
//            int toArrayCount,
//            int startScreenPoint,
//            float scrollX,
//            float maxYPoints
//    ) {
//        float height = screenHeight - DATE_BLOCK_HEIGHT;
//        float itemY = height / maxYPoints;
//        float[] toArray = new float[toArrayCount * 4];
//
//        for (int i = 0; i < toArrayCount; i++) {
//            int toArrayPosition = i * 4;
//            int rootArrayPosition = i + startScreenPoint;
//            int previousPosition = getPreviousPosition(rootArrayPosition);
//
//            toArray[toArrayPosition] = scrollX - (previousPosition > 0 ? lineWeight * previousPosition : 0);
//            toArray[toArrayPosition + 1] = height - (fromArray[previousPosition] * itemY);
//            toArray[toArrayPosition + 2] = scrollX - (i > 0 ? lineWeight * rootArrayPosition : 0);
//            toArray[toArrayPosition + 3] = height - (fromArray[rootArrayPosition] * itemY);
//        }
//        return toArray;
//    }

    private int getPreviousPosition(int position) {
        return position <= 0 ? 0 : position - 1;
    }

    private void initLinePaint() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setTextSize(60);
        linePaint.setStrokeWidth(6);
    }

    private void initTablePaint() {
        tablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tablePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        tablePaint.setStrokeWidth(1);
        tablePaint.setTextSize(TABLE_TEXT_SIZE);
    }
}
