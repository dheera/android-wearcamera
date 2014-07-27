package net.dheera.wearcamera;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraFragment extends Fragment {
    private View.OnClickListener mOnClickListener;

    public ImageView cameraPreview = null;
    public ImageView cameraResult;
    public TextView cameraTime;

    public ImageView mImageView_tip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.camera, container, false);
        cameraPreview = (ImageView)contentView.findViewById(R.id.camera_preview);
        cameraTime = (TextView)contentView.findViewById(R.id.camera_time);
        cameraResult = (ImageView)contentView.findViewById(R.id.camera_result);
        cameraPreview.setOnClickListener(mOnClickListener);

        SharedPreferences preferences = getActivity().getPreferences(0);
        if (preferences.getBoolean("tip0_hide", false) == false) {
            mImageView_tip = (ImageView) contentView.findViewById(R.id.tip);
            mImageView_tip.setImageResource(R.drawable.tip_0);
            mImageView_tip.setVisibility(View.VISIBLE);
            mImageView_tip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mImageView_tip.setVisibility(View.GONE);
                    SharedPreferences preferences = getActivity().getPreferences(0);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("tip0_hide", true);
                    editor.commit();
                }
            });
        }

        return contentView;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }
}