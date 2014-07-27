package net.dheera.wearcamera;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Handler;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.util.Log;
import android.view.View;
public class MenuAdapter extends FragmentGridPagerAdapter {

    private final Context mContext;
    private final Handler mHandler;
    public final CameraFragment mCameraFragment;

    public static final int MESSAGE_SNAP = 1;
    public static final int MESSAGE_SWITCH = 2;
    public static final int MESSAGE_TIMER = 3;
    public static final int MESSAGE_FLASH = 4;

    private static int currentFlash = 0;
    int[] currentFlashText = { R.string.action_flash_0, R.string.action_flash_1, R.string.action_flash_2 };
    int[] currentFlashIcon = { R.drawable.action_flash_0, R.drawable.action_flash_1, R.drawable.action_flash_2 };

    private static int currentCamera = 0;
    int[] currentCameraText = { R.string.action_switch_0, R.string.action_switch_1 };
    int[] currentCameraIcon = { R.drawable.action_switch_0, R.drawable.action_switch_1 };

    private static int currentTimer = 0;
    int[] currentTimerText = { R.string.action_timer_0, R.string.action_timer_1, R.string.action_timer_2 };
    int[] currentTimerIcon = { R.drawable.action_timer_0, R.drawable.action_timer_1, R.drawable.action_timer_2 };

    public MenuAdapter(Context ctx, FragmentManager fm, Handler h) {
        super(fm);
        mContext = ctx;
        mHandler = h;
        mCameraFragment = new CameraFragment();
        mCameraFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("blah", "clicked snap");
                mHandler.obtainMessage(MESSAGE_SNAP).sendToTarget();
            }
        });
    }

    @Override
    public int getColumnCount(int arg0) {
        return 4;
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public Fragment getFragment(int rowNum, int colNum) {
        Log.d("blah", String.format("getFragment(%d, %d)", rowNum, colNum));

        if(colNum == 0) {
            return mCameraFragment;
        }

        if(colNum == 1) {
            final ActionFragment switchAction = ActionFragment.newInstance(currentCameraIcon[currentCamera], currentCameraText[currentCamera]);
            switchAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("blah", "clicked switch");
                    currentCamera = ( currentCamera + 1 ) % currentCameraText.length;
                    switchAction.setTextRes(currentCameraText[currentCamera]);
                    switchAction.setIconRes(currentCameraIcon[currentCamera]);
                    mHandler.obtainMessage(MESSAGE_SWITCH, currentCamera, -1).sendToTarget();
                }
            });
            return switchAction;
        }

        if(colNum == 2) {
            final ActionFragment flashAction = ActionFragment.newInstance(currentFlashIcon[currentFlash], currentFlashText[currentFlash]);
            flashAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("blah", "clicked flash");
                    currentFlash = ( currentFlash + 1 ) % currentFlashText.length;
                    flashAction.setTextRes(currentFlashText[currentFlash]);
                    flashAction.setIconRes(currentFlashIcon[currentFlash]);
                    mHandler.obtainMessage(MESSAGE_FLASH, currentFlash, -1).sendToTarget();
                }
            });
            return flashAction;
        }

        if(colNum == 3) {
            final ActionFragment timerAction = ActionFragment.newInstance(currentTimerIcon[currentTimer], currentTimerText[currentTimer]);
            timerAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("blah", "clicked timer");
                    currentTimer = ( currentTimer + 1 ) % currentTimerText.length;
                    timerAction.setTextRes(currentTimerText[currentTimer]);
                    timerAction.setIconRes(currentTimerIcon[currentTimer]);
                    mHandler.obtainMessage(MESSAGE_TIMER, currentTimer, -1).sendToTarget();
                }
            });
            return timerAction;
        }
        return null;
    }

}
