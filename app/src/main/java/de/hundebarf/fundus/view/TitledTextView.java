package de.hundebarf.fundus.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.hundebarf.fundus.R;

public class TitledTextView extends LinearLayout {

    private TextView mTitleTextView;
    private TextView mTextTextView;

    public TitledTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.titled_textview, 0, 0);
        String title = a.getString(R.styleable.titled_textview_title);
        String text = a.getString(R.styleable.titled_textview_text);
        a.recycle();

        setOrientation(LinearLayout.VERTICAL);
        //setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.titled_textview, this, true);

        mTitleTextView = (TextView) getChildAt(0);
        mTitleTextView.setText(title);

        mTextTextView = (TextView) getChildAt(1);
        mTextTextView.setText(text);
    }

    public TitledTextView(Context context) {
        this(context, null);
    }

    public void setText(String text) {
        mTextTextView.setText(text);
    }

}
