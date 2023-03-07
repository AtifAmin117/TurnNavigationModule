package com.example.turnbyturntestproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.MediaController;
import android.widget.VideoView;

public class RNVideoPlayer extends VideoView {
    VideoView videoView;
    private Boolean autoPlay = true;
    private Boolean paused = true;
    public RNVideoPlayer(Context context) {
        super(context);
        videoView = this;
        MediaController mediaController= new MediaController(context);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        setupListeners();
    }
    public void setAutoPlay(Boolean autoPlay) {
        this.autoPlay = autoPlay;
    }
    public void setPaused(Boolean paused) {
        this.paused = paused;
        if(this.videoView.canPause() && paused) {
            this.videoView.pause();
        } else if(!paused) {
            this.videoView.resume();
        }
    }
    private void setupListeners() {
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
             }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                 if(autoPlay) {
                    videoView.start();
                }
            }
        });
    }
}