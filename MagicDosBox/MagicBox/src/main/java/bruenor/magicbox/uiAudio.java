/*
 *  Copyright (C) 2011-2012 Locnet (android.locnet@gmail.com)
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

/*
 *  Copyright (C) 2013-2016 Antony Hornacek (magicbox@imejl.sk)
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package bruenor.magicbox;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import magiclib.dosbox.DosboxConfig;

class uiAudio
{
	public boolean useAndroidAudioHack;

	private MagicLauncher mParent = null;
	private boolean mAudioRunning=true;
	private AudioTrack mAudio = null;

	public short[] mAudioBuffer = null;

	uiAudio(MagicLauncher context) {
		mParent = context;
	}

	public int initAudio(int rate, int channels, int encoding, int bufSize)
	{
		if( mAudio == null )
		{
			int bufSize2 = bufSize;

			channels = ( channels == 1 ) ? AudioFormat.CHANNEL_IN_MONO :
					AudioFormat.CHANNEL_IN_STEREO;
			encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
					AudioFormat.ENCODING_PCM_8BIT;

			if( AudioTrack.getMinBufferSize( rate, channels, encoding ) > bufSize )
			{
				//bufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );
				bufSize2 = AudioTrack.getMinBufferSize( rate, channels, encoding );
				//TODO
				bufSize2 = Math.max(bufSize2, bufSize * 2);
				//bufSize2 = Math.max(bufSize2, bufSize << 3);
			}

			mAudioMinUpdateInterval = 1000*(bufSize >> 1)/((channels == AudioFormat.CHANNEL_IN_MONO)?1:2)/rate;

			//TODO
			//mAudioBuffer = new short[bufSize >> 1];
			//mAudioBuffer = new short[bufSize >> ((mParent.mPrefOperationMode == 1)?2:1)];
			mAudioBuffer = new short[bufSize >> (useAndroidAudioHack?2:1)];
			//mAudioBuffer = new short[bufSize >> 2];

/*
			Log.log("Audio rate[" + rate + "] channels[" + channels + "] encoding[" + encoding + "] bufSize[" + bufSize + "] bufSize>>2[" +
					(bufSize >> 2) + "] bufSize >> 1[" + (bufSize >> 1)  + "] bufSize >> 3[" + (bufSize >> 3) + "]");
*/

			mAudio = new AudioTrack(AudioManager.STREAM_MUSIC,
					rate,
					channels,
					encoding,
					//bufSize,
					bufSize2,
					AudioTrack.MODE_STREAM );
			mAudio.pause();

			return bufSize;
		}

		return 0;
	}

	public void shutDownAudio() {
		if (mAudio != null) {
			mAudio.stop();
			mAudio.release();
			mAudio = null;
		}
		mAudioBuffer = null;
	}

	private long mLastWriteBufferTime = 0;
	private int mAudioMinUpdateInterval = 50;

	public void AudioWriteBuffer(int size)
	{
		if ((mAudioBuffer != null) && mAudioRunning)
		{
			long now = System.currentTimeMillis();

			if ((!mParent.mTurboOn) || ((now - mLastWriteBufferTime) > mAudioMinUpdateInterval))
			{
				if (size > 0)
					writeSamples( mAudioBuffer, (size << 1 ) );

				mLastWriteBufferTime = now;
			}
		}
	}

	public void setRunning()
	{
		mAudioRunning = !mAudioRunning;

		if (!mAudioRunning)
			mAudio.pause();
	}

	public void writeSamples(short[] samples, int size)
	{
		if (mAudioRunning)
		{
			if (mAudio != null)
			{
				mAudio.write( samples, 0, size );

				if (mAudio.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
					play();
			}
		}
	}

	public void play()
	{
		if (mAudio != null)
			mAudio.play();
	}

	public void pause()
	{
		if (mAudio != null)
			mAudio.pause();
	}
}

/*
import java.util.Stack;

class uiAudio
{
	class AudioThread extends Thread
	{
		private long mLastWriteBufferTime = 0;
		private int mAudioMinUpdateInterval = 50;
		public int rate;
		public int channels;
		public int encoding;
		public int bufSize;
		public volatile boolean running = false;
		public Stack<Integer> stack = new Stack<Integer>();
		private AudioTrack mAudio = null;

		@Override
		public void run()
		{
			running = true;
			initAudio();

			while (running) {
				while (!stack.isEmpty())

					if ((mAudioBuffer != null) && mAudioRunning)
					{
						int size = stack.pop();
						long now = System.currentTimeMillis();

						if ((!mParent.mTurboOn) || ((now - mLastWriteBufferTime) > mAudioMinUpdateInterval))
						{
							if (size > 0)
								writeSamples( mAudioBuffer, (size << 1 ) );

							mLastWriteBufferTime = now;
						}
					}

					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}

			if (mAudio != null) {
				mAudio.stop();
				mAudio.release();
				mAudio = null;
			}
			mAudioBuffer = null;
		}

		public void writeSamples(short[] samples, int size)
		{
			mAudio.write( samples, 0, size );
			if (mAudio.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
				play();
		}

		public int initAudio()
		{
		if( mAudio == null )
		{
			int bufSize2 = bufSize;

			channels = ( channels == 1 ) ? AudioFormat.CHANNEL_IN_MONO :
											AudioFormat.CHANNEL_IN_STEREO;
			encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
											AudioFormat.ENCODING_PCM_8BIT;

			if( AudioTrack.getMinBufferSize( rate, channels, encoding ) > bufSize )
			{
				//bufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );
				bufSize2 = AudioTrack.getMinBufferSize( rate, channels, encoding );
				//TODO
				bufSize2 = Math.max(bufSize2, bufSize * 2);
				//bufSize2 = Math.max(bufSize2, bufSize << 3);
			}

			mAudioMinUpdateInterval = 1000*(bufSize >> 1)/((channels == AudioFormat.CHANNEL_IN_MONO)?1:2)/rate;

			//TODO
			//mAudioBuffer = new short[bufSize >> 1];
			//mAudioBuffer = new short[bufSize >> ((mParent.mPrefOperationMode == 1)?2:1)];

			mAudio = new AudioTrack(AudioManager.STREAM_MUSIC,
										rate,
										channels,
										encoding,
										//bufSize,
										bufSize2,
										AudioTrack.MODE_STREAM );
			mAudio.pause();
			//thread.start();

			return bufSize;
		}

		return 0;
		}
	}

	public AudioThread thread = new AudioThread();
	private MagicLauncher mParent = null;
	private boolean mAudioRunning=true;


	public short[] mAudioBuffer = null;

	uiAudio(MagicLauncher context) {
		mParent = context;
	}

	public int initAudio(int rate, int channels, int encoding, int bufSize)
	{
		thread.rate = rate;
		thread.channels = channels;
		thread.encoding = encoding;
		thread.bufSize = bufSize;
		mAudioBuffer = new short[bufSize >> ((LayoutManager.config.isAutomaticPerformance())?2:1)];
		thread.start();
		return bufSize;
	}
   
	public void shutDownAudio() {
	   thread.running = false;
	}

	public void AudioWriteBuffer(int size)
	{
		thread.stack.add(size);
	}
   
   public void play()
   {
	   if (thread.mAudio != null)
		   thread.mAudio.play();
   }
   
   public void pause() 
   {
	   if (thread.mAudio != null)
		   thread.mAudio.pause();
   }   
}*/