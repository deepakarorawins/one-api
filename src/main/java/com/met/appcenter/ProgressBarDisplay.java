package com.met.appcenter;

import com.met.appcenter.interfaces.IProgressDisplay;

public class ProgressBarDisplay implements IProgressDisplay {
    @Override
    public void displayProgress(int progress) {
        int progressBarWidth = 50;
        int completedBars = (progress * progressBarWidth) / 100;
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < progressBarWidth; i++) {
            progressBar.append(i < completedBars ? "=" : " ");
        }
        progressBar.append("] ").append(progress).append("%");
        System.out.print("\r" + progressBar);

        if (progress == 100) {
            System.out.println(); // Move to the next line
        }
    }
}