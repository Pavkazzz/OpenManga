package org.nv95.openmanga.components.pager;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by nv95 on 01.07.16.
 */

public class SlidePageTransformer implements ViewPager.PageTransformer {

    private final boolean mReversed;

    public SlidePageTransformer(boolean reversed) {
        mReversed = reversed;
    }

    @Override
    public void transformPage(View page, float position) {
        int pageWidth = page.getWidth();
        if (position < -1) { // [-Infinity,-1)
            page.setAlpha(0);
        } else if (position <= 0) { // [-1,0]
            page.setAlpha(1);
            page.setTranslationX(mReversed ? pageWidth * -position : 0);
        } else if (position <= 1) { // (0,1]
            page.setAlpha(1);
            page.setTranslationX(mReversed ? 0 : pageWidth * -position);
        } else { // (1,+Infinity]
            page.setAlpha(0);
        }
    }
}
