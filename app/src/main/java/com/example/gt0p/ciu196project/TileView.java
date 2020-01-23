package com.example.gt0p.ciu196project;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;


public class TileView extends ImageView {
    // Attributes
    private boolean isSelected = false;
    private float borderThickness;

    private Paint borderPaint;

    // Constructors
    public TileView (Context context) {
        super(context);
        init(null);
    }

    public TileView (Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        // Get all attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.TileView, 0, 0);

        /*if(a.hasValue(R.styleable.TileView_image)) {
            image = a.getDrawable(R.styleable.TileView_image);
            image.setCallback(this);
            int h = image.getIntrinsicHeight();
            int w = image.getIntrinsicWidth();
            float aspectRatio = (float)w / (float)h;
        }*/
        isSelected = a.getBoolean(R.styleable.TileView_isSelected, false);
        borderThickness = a.getDimension(R.styleable.TileView_borderThickness, 0);

        a.recycle();

        // Define Paint objects
        borderPaint = new Paint();
        borderPaint.setAntiAlias(false);
        borderPaint.setStrokeWidth(borderThickness);
        borderPaint.setStyle(Paint.Style.STROKE);

        invalidateBorder();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Consider padding
        //int paddingLeft = (getWidth() - getDrawable().getIntrinsicWidth())/2 - (int)(borderThickness/2);//getPaddingLeft();
        int paddingLeft = getPaddingLeft() + (int) borderThickness/2;
        int paddingTop =  getPaddingTop() + (int) borderThickness/2;
        //int paddingTop =  (getHeight() - getDrawable().getIntrinsicHeight())/2 - (int)(borderThickness/2);
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        //int borderWidth = getDrawable().getIntrinsicWidth() + 2 * (int)borderThickness;// - paddingLeft - paddingRight;
        //int borderHeight = getDrawable().getIntrinsicHeight() + 2 * (int)borderThickness;//getHeight() - paddingTop - paddingBottom;

        int borderWidth = getWidth() - paddingLeft - paddingRight;
        int borderHeight = getHeight() - paddingTop - paddingBottom;

        int imageLeft = Math.round(borderThickness);
        int imageTop = paddingTop + Math.round(borderThickness);
        int imageWidth = borderWidth - (int)(1 * borderThickness);
        int imageHeight = borderHeight - (int)(1 * borderThickness);

        // Draw the border
        canvas.drawRect(paddingLeft, paddingTop, borderWidth, borderHeight, borderPaint);

        // Draw tile image
        /*if(image != null) {
            image.setBounds(imageLeft, imageTop, imageWidth, imageHeight);
            image.draw(canvas);
        }*/

        //getDrawable().draw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Make the view as big as possible

        /*int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);

        int minh = MeasureSpec.getSize(w) - getPaddingBottom() + getPaddingTop();
        int h = resolveSizeAndState(MeasureSpec.getSize(w), heightMeasureSpec, 1);

        // The final size
        int widthSize = w;
        int heightSize = h;

        float desiredAspectRatio = (float) image.getIntrinsicWidth() / (float) image.getIntrinsicHeight();
        float actualAspectRatio = (float) w / (float) h;

        // Resize width to fit width constrain and desired aspect ratio
        int newWidth = (int) (desiredAspectRatio * h);

        if(newWidth <= widthSize) {
            widthSize = newWidth;
        }

        int newHeight = (int) (widthSize / desiredAspectRatio);

        if(newHeight <= heightSize) {
            heightSize = newHeight;
        }*/

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);

    }

    // Define border color according to selection
    public void invalidateBorder() {
        int borderColor = getResources().getColor(R.color.colorTileDeselected);

        if(isSelected()) {
            borderColor = getResources().getColor(R.color.colorTileSelected);
        }

        borderPaint.setColor(borderColor);
    }

    public void invalidateImage() {
        /*image.setCallback(this);
        int h = image.getIntrinsicHeight();
        int w = image.getIntrinsicWidth();
        float aspectRatio = (float)w / (float)h;*/
    }


    public Drawable getImage() {
        return getDrawable();
    }

    public void setImage(Drawable image) {
        this.setImageDrawable(image);
        invalidateImage();
        invalidate();
        requestLayout();
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        isSelected = selected;
        invalidateBorder();
        requestLayout();
    }

    public float getBorderThickness() {
        return borderThickness;
    }

    public void setBorderThickness(float borderThickness) {
        this.borderThickness = borderThickness;
        invalidateBorder();
        requestLayout();
    }
}
