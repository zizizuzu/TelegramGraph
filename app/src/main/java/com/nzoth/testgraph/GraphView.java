package com.nzoth.testgraph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GraphView extends View {
    private static final int TABLE_LINE_COUNT = 6;
    private static final int TABLE_TEXT_PADDING_DP = 10;
    private static final int TABLE_TEXT_SIZE_SP = 12;

    private static final int GRAPH_LINE_MARGIN_DP = 4;
    private static final int GRAPH_BLOCK_LINE_WIDTH_DP = 2;

    private static final int SCROLL_BLOCK_HEIGHT_DP = 60;
    private static final int SCROLL_BLOCK_MIN_WIDTH_DP = 60;
    private static final int SCROLL_BLOCK_HOLD_LINE_WIDTH_DP = 4;
    private static final float SCROLL_BLOCK_LINE_WIDTH_DP = 0.8F;

    private static final float DATE_WIDTH_COEFICIENT_DP = 2F;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d", Locale.US);


    private Paint linePaint;
    private Paint tablePaint;
    private Paint scrollPaint;

    private GestureDetectorCompat gestureDetector;

    private float mXScrollingSpeed = 1f;


    private GraphItem graphItem;
    private ScrollType scrollTypeAction = ScrollType.NONE;

    private float[][] canvasScrollGraphArray;

    private float screenWidth;
    private float screenHeight;

    private float graphLineMarginTopBottom;

    private float graphBlockHeight;
    private float graphBlockTextSize;
    private float graphBlockTextPadding;
    private float graphBlockLineWidth;


    private float scrollBlockHeight;
    private float scrollBlockMinWidth;
    private float scrollBlockLineWidth;
    private float holdLineWidth;
    private float holdLineWidthHalf;
    private float holdLineLeftX;
    private float holdLineRightX;

    private float sizeCoeficient;
    private float graphBlockLineWeight;

    private int dateArrayLength;
    private float dateWidthCoeficient;

    private enum ScrollType {
        NONE, GRAPH, SCROLL_BLOCK, HOLD_LINE_LEFT, HOLD_LINE_RIGHT
    }

    public GraphView(Context context, GraphItem graphItem) {
        super(context);
        this.graphItem = graphItem;
        this.gestureDetector = new GestureDetectorCompat(context, gestureListener);
        this.dateArrayLength = graphItem.getDateList().length;
        this.canvasScrollGraphArray = new float[graphItem.getGraphList().length][dateArrayLength * 4];
        this.scrollBlockHeight = getPixelsFromDp(SCROLL_BLOCK_HEIGHT_DP);
        this.scrollBlockMinWidth = getPixelsFromDp(SCROLL_BLOCK_MIN_WIDTH_DP);
        this.holdLineWidth = getPixelsFromDp(SCROLL_BLOCK_HOLD_LINE_WIDTH_DP);
        this.holdLineWidthHalf = holdLineWidth / 2;
        this.graphBlockTextSize = getPixelsFromSp(TABLE_TEXT_SIZE_SP);
        this.graphBlockTextPadding = getPixelsFromDp(TABLE_TEXT_PADDING_DP);
        this.graphLineMarginTopBottom = getPixelsFromDp(GRAPH_LINE_MARGIN_DP);
        this.graphBlockLineWidth = getPixelsFromDp(GRAPH_BLOCK_LINE_WIDTH_DP);
        this.scrollBlockLineWidth = getPixelsFromDp(SCROLL_BLOCK_LINE_WIDTH_DP);
        this.dateWidthCoeficient = getPixelsFromDp(DATE_WIDTH_COEFICIENT_DP);

        initLinePaint();
        initTablePaint();
        initScrollPaint();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        screenWidth = getWidth();
        screenHeight = getHeight();
        graphBlockHeight = screenHeight - (scrollBlockHeight + graphBlockTextSize + graphBlockTextPadding * 2);
        holdLineLeftX = screenWidth - scrollBlockMinWidth + holdLineWidthHalf;
        holdLineRightX = screenWidth - holdLineWidthHalf;
        graphBlockLineWeight = getGraphBlockWidth();
        sizeCoeficient = getSizeCoeficient();
        rebuildCanvasScrollGraphArray();
    }

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float holdLeftX;
            float holdRightX;
            float scrolledDistance = mXScrollingSpeed * distanceX;

            switch (scrollTypeAction) {
                case GRAPH:
                    holdLeftX = holdLineLeftX + scrolledDistance / sizeCoeficient;
                    holdRightX = holdLineRightX + scrolledDistance / sizeCoeficient;

                    if (holdLeftX >= holdLineWidthHalf
                            && holdRightX <= screenWidth - holdLineWidthHalf
                    ) {
                        holdLineLeftX = holdLeftX;
                        holdLineRightX = holdRightX;

                        ViewCompat.postInvalidateOnAnimation(GraphView.this);
                    }
                    break;
                case SCROLL_BLOCK:
                    holdLeftX = holdLineLeftX - scrolledDistance;
                    holdRightX = holdLineRightX - scrolledDistance;

                    if (holdLeftX >= holdLineWidthHalf
                            && holdRightX <= screenWidth - holdLineWidthHalf) {
                        holdLineLeftX = holdLeftX;
                        holdLineRightX = holdRightX;

                        ViewCompat.postInvalidateOnAnimation(GraphView.this);
                    }
                    break;
                case HOLD_LINE_LEFT:
                    holdLeftX = holdLineLeftX - scrolledDistance;

                    if (holdLeftX >= holdLineWidthHalf
                            && holdLeftX <= holdLineRightX - scrollBlockMinWidth + holdLineWidthHalf) {
                        holdLineLeftX = holdLeftX;
                        graphBlockLineWeight = getGraphBlockWidth();
                        sizeCoeficient = getSizeCoeficient();

                        ViewCompat.postInvalidateOnAnimation(GraphView.this);
                    }
                    break;
                case HOLD_LINE_RIGHT:
                    holdRightX = holdLineRightX - scrolledDistance;

                    if (holdRightX <= screenWidth - holdLineWidthHalf
                            && holdRightX >= holdLineLeftX + scrollBlockMinWidth - holdLineWidthHalf) {
                        holdLineRightX = holdRightX;
                        graphBlockLineWeight = getGraphBlockWidth();
                        sizeCoeficient = getSizeCoeficient();

                        ViewCompat.postInvalidateOnAnimation(GraphView.this);
                    }
                    break;
                default:
                    //empty
                    break;
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float eventX = event.getX();
                float eventY = event.getY();

                if (eventY <= graphBlockHeight) {
                    scrollTypeAction = ScrollType.GRAPH;
                } else if (eventX >= holdLineLeftX - holdLineWidth
                        && eventX <= holdLineLeftX + holdLineWidth) {
                    scrollTypeAction = ScrollType.HOLD_LINE_LEFT;
                } else if (eventX >= holdLineRightX - holdLineWidth
                        && eventX <= holdLineRightX + holdLineWidth) {
                    scrollTypeAction = ScrollType.HOLD_LINE_RIGHT;
                } else if (eventX > holdLineLeftX && eventX < holdLineRightX) {
                    scrollTypeAction = ScrollType.SCROLL_BLOCK;
                } else {
                    scrollTypeAction = ScrollType.NONE;
                }

                break;
            case MotionEvent.ACTION_UP:
                scrollTypeAction = ScrollType.NONE;
                break;
        }

        return gestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTable(canvas);
        drawLines(canvas);
        drawScrollBlock(canvas);
    }

    private void drawTable(Canvas canvas) {
        float rowHeight = graphBlockHeight / (float) TABLE_LINE_COUNT;

        float[] table = new float[TABLE_LINE_COUNT * 4];
        for (int i = 0; i < TABLE_LINE_COUNT; i++) {
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
        float rowHeight = graphBlockHeight / (float) TABLE_LINE_COUNT;
        int partOfPoints = maxYPoints / TABLE_LINE_COUNT;

        tablePaint.setColor(Color.GRAY);
        for (int i = 0; i < TABLE_LINE_COUNT; i++) {
            canvas.drawText(
                    FormatUtils.formatBigValue(i > 0 ? i * partOfPoints : 0),
                    graphBlockTextPadding,
                    (graphBlockHeight - i * rowHeight) - graphBlockTextPadding,
                    tablePaint
            );
        }
    }

    private void drawTableDateText(
            Canvas canvas,
            float scrollX,
            int startScreenPoint,
            int endScreenPoint
    ) {
        float visibleDateValue = screenWidth / graphBlockLineWeight / dateWidthCoeficient;
        float height = graphBlockHeight + graphBlockTextSize + graphBlockTextPadding;
        float startPosition = startScreenPoint - startScreenPoint % visibleDateValue;

        for (int i = 1; i < endScreenPoint - startPosition; i += visibleDateValue) {
            float rootArrayPosition = i + startPosition;
            float positionX = scrollX - (i > 0 ? graphBlockLineWeight * rootArrayPosition : 0);

            Date date = new Date(graphItem.getDateList()[(int) rootArrayPosition]);
            String text = DATE_FORMAT.format(date);

            float x = positionX - tablePaint.measureText(text) / 2;
            canvas.drawText(text, x, height, tablePaint);
        }
    }

    private void drawLines(Canvas canvas) {
        float startScrollX = (-holdLineRightX - holdLineWidthHalf + screenWidth) * sizeCoeficient;
        float endScrollX = (-holdLineLeftX + holdLineWidthHalf + screenWidth) * sizeCoeficient;

        int startScreenPoint = getPreviousPosition((int) Math.ceil(startScrollX / graphBlockLineWeight));
        int endScreenPointTerm = (int) Math.ceil(endScrollX / graphBlockLineWeight) + 1;
        int endScreenPoint = dateArrayLength > endScreenPointTerm ? endScreenPointTerm : dateArrayLength;
        int toArrayCount = endScreenPoint - startScreenPoint;

        long[][] graphArray = graphItem.getGraphList();

        if (toArrayCount > 0) {
            float maxYPoints = getMaxYHeight(graphArray, startScreenPoint, endScreenPoint, startScrollX, endScrollX);

            for (int graphCount = 0; graphCount < graphArray.length; graphCount++) {
                String title = graphItem.getGraphTitleList()[graphCount];
                int color = Color.parseColor(graphItem.getGraphColorList()[graphCount]);

                float[] toArray = getChainArrayFromArray(graphArray[graphCount], toArrayCount, startScreenPoint, endScrollX, maxYPoints);

                linePaint.setColor(color);
                linePaint.setStrokeWidth(graphBlockLineWidth);
                canvas.drawLines(toArray, linePaint);
            }

            drawTableYText(canvas, (int) maxYPoints);
        }

        drawTableDateText(canvas, endScrollX, startScreenPoint, endScreenPoint);
    }

    private void drawScrollBlock(Canvas canvas) {
        //draw graph
        for (int i = 0; i < canvasScrollGraphArray.length; i++) {
            int color = Color.parseColor(graphItem.getGraphColorList()[i]);
            linePaint.setColor(color);
            linePaint.setStrokeWidth(scrollBlockLineWidth);
            canvas.drawLines(canvasScrollGraphArray[i], linePaint);
        }

        //lines for change graph sizing
        scrollPaint.setAlpha(50);
        scrollPaint.setStrokeWidth(holdLineWidth);
        canvas.drawLine(holdLineRightX, screenHeight - scrollBlockHeight, holdLineRightX, screenHeight, scrollPaint);
        canvas.drawLine(holdLineLeftX, screenHeight - scrollBlockHeight, holdLineLeftX, screenHeight, scrollPaint);

        //draw top and bottom lines
        float topBottomBorderWidth = 2;
        float line2BottomY = screenHeight - topBottomBorderWidth / 2;
        float line2TopY = line2BottomY - scrollBlockHeight + topBottomBorderWidth;
        float line2StartX = holdLineRightX - holdLineWidthHalf;
        float line2EndX = holdLineLeftX + holdLineWidthHalf;
        scrollPaint.setStrokeWidth(topBottomBorderWidth);
        canvas.drawLine(line2StartX, line2TopY, line2EndX, line2TopY, scrollPaint);
        canvas.drawLine(line2StartX, line2BottomY, line2EndX, line2BottomY, scrollPaint);

        //on graph fill color
        float scrollBackgroundY = screenHeight - (scrollBlockHeight / 2);
        scrollPaint.setAlpha(20);
        scrollPaint.setStrokeWidth(scrollBlockHeight);
        canvas.drawLine(0, scrollBackgroundY, holdLineLeftX - holdLineWidthHalf, scrollBackgroundY, scrollPaint);
        canvas.drawLine(screenWidth, scrollBackgroundY, holdLineRightX + holdLineWidthHalf, scrollBackgroundY, scrollPaint);
    }

    //for big graph
    private float[] getChainArrayFromArray(
            long[] fromArray,
            int toArrayCount,
            int startScreenPoint,
            float scrollX,
            float maxYPoints
    ) {
        float itemY = graphBlockHeight / maxYPoints;
        float[] toArray = new float[toArrayCount * 4];

        for (int i = 0; i < toArrayCount; i++) {
            int toArrayPosition = i * 4;
            int rootArrayPosition = i + startScreenPoint;
            int previousPosition = getPreviousPosition(rootArrayPosition);

            toArray[toArrayPosition] = scrollX - (previousPosition > 0 ? graphBlockLineWeight * previousPosition : 0);
            toArray[toArrayPosition + 1] = graphBlockHeight - (fromArray[previousPosition] * itemY);
            toArray[toArrayPosition + 2] = scrollX - (i > 0 ? graphBlockLineWeight * rootArrayPosition : 0);
            toArray[toArrayPosition + 3] = graphBlockHeight - (fromArray[rootArrayPosition] * itemY);
        }
        return toArray;
    }

    private void rebuildCanvasScrollGraphArray() {
        float lineHeight = getScrollBlockLineHeight(graphItem.getGraphList());

        for (int graphCount = 0; graphCount < canvasScrollGraphArray.length; graphCount++) {
            long[] fromArray = graphItem.getGraphList()[graphCount];
            float[] toArray = canvasScrollGraphArray[graphCount];

            int arrayLength = fromArray.length;
            float lineWidth = screenWidth / (arrayLength - 1);

            for (int i = 0; i < arrayLength; i++) {
                int toArrayPosition = i * 4;
                int previousPosition = getPreviousPosition(i);

                toArray[toArrayPosition] = screenWidth - (previousPosition > 0 ? lineWidth * previousPosition : 0);
                toArray[toArrayPosition + 1] = screenHeight - graphLineMarginTopBottom - (fromArray[previousPosition] * lineHeight);
                toArray[toArrayPosition + 2] = screenWidth - (i > 0 ? lineWidth * i : 0);
                toArray[toArrayPosition + 3] = screenHeight - graphLineMarginTopBottom - (fromArray[i] * lineHeight);
            }
        }
    }

    private float getScrollBlockLineHeight(long[][] graphArray) {
        float maxY = 0;
        for (long[] array : graphArray) {
            for (long i : array) {
                maxY = maxY < i ? i : maxY;
            }
        }
        return (scrollBlockHeight - graphLineMarginTopBottom * 2) / maxY;
    }

    private float getMaxYHeight(long[][] graphArray, int startScreenPoint, int endScreenPoint, float startScrollX, float endScrollX) {
        float maxY = 0;
        int toArrayCount = endScreenPoint - startScreenPoint;

        for (long[] array : graphArray) {

            int arrayLength = array.length;
            float startY1 = array[arrayLength - 1 > startScreenPoint ? startScreenPoint + 1 : arrayLength - 1];
            float startY2 = array[startScreenPoint];
            float startX3 = 1 - (startScrollX / graphBlockLineWeight - startScreenPoint);
            float startPosition = getPointOnLine(startY1, startY2, startX3);
            maxY = maxY < startPosition ? startPosition : maxY;

            float endY1 = array[endScreenPoint >= 2 ? endScreenPoint - 2 : endScreenPoint];
            float endY2 = array[endScreenPoint >= 1 ? endScreenPoint - 1 : endScreenPoint];
            float endX3 = endScrollX / graphBlockLineWeight - (float) Math.floor(endScrollX / graphBlockLineWeight);
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

    private int getPreviousPosition(int position) {
        return position <= 0 ? 0 : position - 1;
    }

    private float getGraphBlockWidth() {
        float itemsInScrollBlock = (holdLineRightX - holdLineLeftX + holdLineWidth) / (screenWidth / dateArrayLength);
        return screenWidth / itemsInScrollBlock;
    }

    private float getSizeCoeficient() {
        float linesCount = dateArrayLength - 1;
        float visibleGraphLinesCount = screenWidth / graphBlockLineWeight;
        return linesCount / visibleGraphLinesCount;
    }

    private float getPixelsFromDp(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float getPixelsFromSp(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private void initLinePaint() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setTextSize(60);
    }

    private void initTablePaint() {
        tablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tablePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        tablePaint.setStrokeWidth(1);
        tablePaint.setTextSize(graphBlockTextSize);
    }

    private void initScrollPaint() {
        scrollPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scrollPaint.setStyle(Paint.Style.STROKE);
        scrollPaint.setColor(Color.BLUE);
    }
}
