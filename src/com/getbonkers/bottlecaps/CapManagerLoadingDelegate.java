package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 1/26/12
 * Time: 6:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CapManagerLoadingDelegate {
    public void onCapSetLoadComplete();
    public void onWorkingCapSetAvailable();
}