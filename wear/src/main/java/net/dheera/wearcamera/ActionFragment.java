package net.dheera.wearcamera;

import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ActionFragment extends Fragment {
    private View.OnClickListener mOnClickListener;

    private ImageView iconView;
    private TextView textView;

    public static ActionFragment newInstance(int actionIconRes, int actionTextRes) {
        Bundle args = new Bundle();
        args.putInt("action_icon_res", actionIconRes);
        args.putInt("action_text_res", actionTextRes);
        ActionFragment f = new ActionFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.action, container, false);
        iconView = (ImageView)contentView.findViewById(R.id.icon);
        iconView.setImageResource(getArguments().getInt("action_icon_res"));
        View actionButton = contentView.findViewById(R.id.action_button);
        actionButton.setOnClickListener(mOnClickListener);
        int textRes = getArguments().getInt("action_text_res");
        textView = (TextView)contentView.findViewById(R.id.text);
        textView.setText(getResources().getText(textRes));
        return contentView;
    }

    public void setTextRes(int actionTextRes) {
        if(textView != null) {
            textView.setText(getResources().getText(actionTextRes));
        }
    }

    public void setIconRes(int actionIconRes) {
        if(iconView != null) {
            iconView.setImageResource(actionIconRes);
        }
    }

    public Drawable getBackground() {
        return new ColorDrawable(getResources().getColor(R.color.action_background_color));
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }
}