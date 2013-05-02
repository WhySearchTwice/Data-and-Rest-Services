package com.whysearchtwice.extensions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PageViewExtensionTest extends ExtensionTest {
    @Test
    public void createPageView() {
        System.out.println("Testing create new page view for existing user");
    }

    @Test
    public void updatePageView() {
        System.out.println("Testing update existing page view");
    }
    
    @Test
    public void createPageViewInvalidUser() {
        System.out.println("Testing create new page view for invalid user");
    }
}
