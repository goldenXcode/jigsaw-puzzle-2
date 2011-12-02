package com.yqg.puzzle;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.JetPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yqg.puzzle.utils.PlayTimer;
import com.yqg.puzzle.utils.PlayTimer.TimerCallBack;
import com.yqg.puzzle.view.PuzzleView;
import com.yqg.puzzle.view.TileView;
import com.yqg.puzzle.view.TileView.PuzzleCallBack;

public class JigdrawPuzzleMain extends Activity {
	private static final String TAG = "JigdrawPuzzleMain";
	
	private static final String PLAY_TIME = "play_time";
	private static final int TIMER_MESSAGE = 1;
	
	//private field
	private PuzzleView mGameView = null;
	private LinearLayout mPuzzleLayout = null;
	private Bitmap mOrigBitmap = null;
	private TextView mTxtView = null;
	private RelativeLayout mImageLayout = null;
	private Intent mIntent = null;
	
	//state fields
	private boolean isOrigImageShow = false;
	private boolean isNewImageGet = false;
	private int mGameViewWidth = 0;
	private int mGameViewHeight = 0;
	private int mLevel = 2;
	
	private PlayTimer mTimer = new PlayTimer();
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case TIMER_MESSAGE:
				CharSequence time = msg.getData().getCharSequence(PLAY_TIME);
				String timeConsume = getString(R.string.str_time_consume);
				String strTime = String.format(timeConsume, time);
				mTxtView.setText(strTime);
				break;
			default:break;
			}
		};
	};
	
	
	private JetPlayer mJet = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        mTimer.setCallback(timerCallback);
        
        prepareJetPlayer();
        
        Display display = getWindowManager().getDefaultDisplay(); 
        mGameViewWidth = display.getWidth();
        int dheight = display.getHeight();
        
        //320*240 [80]
        int bannerHeight = dheight - mGameViewWidth;
        if(bannerHeight > 80){
        	bannerHeight = 80;
        }
        mGameViewHeight = dheight - bannerHeight;
        //get default bitmap
        Drawable dbmp = getResources().getDrawable(R.drawable.test);
        mOrigBitmap = ((BitmapDrawable)dbmp).getBitmap();
        
        initView(mLevel);
		//random disrupt.
        mGameView.randomDisrupt();
    }
    
	/**
	 * init the view .
	 */
	private void initView(int level){
		//init view
        mGameView = new PuzzleView(this);
        mGameView.setPuzzleCallBack(puzzleCallback);
        
        setContentView(R.layout.puzzleview);
        if(!mGameView.init(level, mGameViewWidth, mGameViewHeight, mOrigBitmap)){//init fail.
        	//TODO process;go to choose panel.
        	Toast.makeText(this, "The image is too small!", Toast.LENGTH_SHORT);
        }else{//init suc,show
        	mPuzzleLayout = (LinearLayout) findViewById(R.id.puzzle_view);
            mTxtView = (TextView) mPuzzleLayout.findViewById(R.id.txt_view_timer);
            mPuzzleLayout.addView(mGameView);
        }
        
		TextView tv = (TextView) findViewById(R.id.textView_level);
		String strLevel = getString(R.string.str_level);
		String levelValue = null;
		switch(level){
		case 1:
			levelValue = getString(R.string.str_level_low);
			break;
		case 2:
			levelValue = getString(R.string.str_level_medium);
			break;
		case 3:
			levelValue = getString(R.string.str_level_high);
			break;
		default:
			levelValue = getString(R.string.str_level_low);
			break;
		}
		
		String sb = String.format(strLevel, levelValue);
		tv.setText(sb.toString());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.menu_choose:
			chooseGameImage();
			break;
		case R.id.menu_check_image:
			showOriginImage(true);
			break;
		case R.id.menu_setting_levelsetting:
			break;
		case R.id.menu_setting_oudiosetting:
			break;
		case R.id.menu_save:
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus){
			changeTimerState(true);
			mJet.play();
		}else{
			changeTimerState(false);
			mJet.pause();
		}
	}
	int REQUEST_CODE_CHOOSE_IMAGE = 110;
	private void chooseGameImage(){
		
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, REQUEST_CODE_CHOOSE_IMAGE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_CODE_CHOOSE_IMAGE){
			if(resultCode == Activity.RESULT_OK){
				isNewImageGet = true;
				mIntent = data;
			}else{
				isNewImageGet = false;
			}
		}
	}
	
	private void showOriginImage(boolean show){
		if(mImageLayout == null){
			LayoutInflater inflater = getLayoutInflater();
			mImageLayout = (RelativeLayout) inflater.inflate(R.layout.origin_image_viewlayout, null);
			ImageView mOrigImg = (ImageView) mImageLayout.findViewById(R.id.origin_image_view);
			mOrigImg.setImageBitmap(mOrigBitmap);
		}
		RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.puzzle_relative_view);
		if(show ){
			if(!isOrigImageShow){
				isOrigImageShow = true;
				rlayout.addView(mImageLayout);
				changeTimerState(false);
			}
		}else{
			if(isOrigImageShow){
				isOrigImageShow = false;
				rlayout.removeView(mImageLayout);	
				changeTimerState(true);
			}
		}
		rlayout.postInvalidate();
	}
	
	private void changeTimerState(boolean turnOn){
		if(turnOn){
			if(!isOrigImageShow){
				mTimer.startTimer();	
			}
		}else{
			mTimer.pauseTimer();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && isOrigImageShow){
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && isOrigImageShow){
			showOriginImage(false);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.puzzle_main_menu, menu);
		com.yqg.puzzle.utils.UtilFuncs.logE(TAG,">>>>>>>>>>>>>>> run");
		return true;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    	if(isNewImageGet){
    		mTimer.stopTimer();
    		Uri uri = mIntent.getData();
    		if(uri != null){
    			try {
    				mOrigBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    		initView(mLevel);
    		//random disrupt.
            mGameView.randomDisrupt();
    	}
    	isNewImageGet = false;
    	changeTimerState(true);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	stopJetPlayer();
    	changeTimerState(false);
    }
    
    private TimerCallBack timerCallback = new PlayTimer.TimerCallBack() {
		
		@Override
		public boolean updateTime(String time) {
			Message msg = mHandler.obtainMessage(TIMER_MESSAGE);
			Bundle data = new Bundle();
			data.putCharSequence(PLAY_TIME, time);
			msg.setData(data);
			mHandler.sendMessage(msg);
			return false;
		}
	};
	
	private PuzzleCallBack puzzleCallback = new TileView.PuzzleCallBack() {
		
		@Override
		public void gameOverCallBack() {
			changeTimerState(false);
			Toast.makeText(JigdrawPuzzleMain.this, "You win!", Toast.LENGTH_SHORT).show();
		}
	};
	
	private void prepareJetPlayer(){
		new JetPlayerPrepareTask().execute(new String[]{});
        //mJet.play();
	}
	
	private void stopJetPlayer(){
		mJet.pause();
	}
	
	private class JetPlayerPrepareTask extends AsyncTask<String,Void,Void>{

		@Override
		protected Void doInBackground(String... params) {
			synchronized (this) {
				mJet = JetPlayer.getJetPlayer();
		        mJet.loadJetFile(getResources().openRawResourceFd(R.raw.level1));
		        byte segmentId = 0;
		        // JET info: end game music, loop 4 times normal pitch
		        mJet.queueJetSegment(1, 0, -1, 0, 0, segmentId);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mJet.play();
		}
		
	}
}