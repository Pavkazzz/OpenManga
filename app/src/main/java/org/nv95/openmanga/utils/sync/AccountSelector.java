package org.nv95.openmanga.utils.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.nv95.openmanga.R;

/**
 * Created by nv95 on 24.07.16.
 */

public class AccountSelector {

    private final Activity mActivity;
    private OnAccountSelectListener mSelectListener;
    private int mSelectedItem = 0;

    public AccountSelector(Activity activity) {
        mActivity = activity;
    }

    public AccountSelector setOnAccountSelectListener(OnAccountSelectListener listener) {
        mSelectListener = listener;
        return this;
    }

    public void show() {
        AccountManager manager = AccountManager.get(mActivity);
        //noinspection MissingPermission
        Account[] accounts = manager.getAccountsByType("com.google");
        if (accounts.length == 0) {
            new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.no_accounts_available)
                    .setPositiveButton(R.string.close, null)
                    .setTitle(R.string.account)
                    .create()
                    .show();
            return;
        }
        final String[] emails = new String[accounts.length];
        for (int i=accounts.length - 1;i>=0;i--) {
            emails[i] = accounts[i].name;
        }

        new AlertDialog.Builder(mActivity)
                .setSingleChoiceItems(emails, mSelectedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedItem = which;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSelectListener != null) {
                            mSelectListener.onAccountSelected(emails[mSelectedItem]);
                        }
                    }
                })
                .setTitle(R.string.account)
                .create()
                .show();
    }

    public interface OnAccountSelectListener {
        void onAccountSelected(String email);
    }
}
