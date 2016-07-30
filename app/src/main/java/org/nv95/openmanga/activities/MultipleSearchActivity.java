package org.nv95.openmanga.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.nv95.openmanga.R;
import org.nv95.openmanga.adapters.GroupedAdapter;
import org.nv95.openmanga.helpers.ListModeHelper;
import org.nv95.openmanga.items.ThumbSize;
import org.nv95.openmanga.lists.MangaList;
import org.nv95.openmanga.providers.LocalMangaProvider;
import org.nv95.openmanga.providers.staff.MangaProviderManager;
import org.nv95.openmanga.providers.staff.ProviderSummary;
import org.nv95.openmanga.utils.LayoutUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by nv95 on 12.01.16.
 */
public class MultipleSearchActivity extends BaseAppActivity implements ListModeHelper.OnListModeListener,
        GroupedAdapter.OnMoreClickListener, DialogInterface.OnDismissListener {

    private ProgressBar mProgressBar;
    private LinearLayout mMessageBlock;
    private String mQuery;
    private GroupedAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private MangaProviderManager mProviderManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private ListModeHelper mListModeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multisearch);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mQuery = getIntent().getStringExtra("query");
        enableHomeAsUp();
        setSubtitle(mQuery);
        mMessageBlock = (LinearLayout) findViewById(R.id.block_message);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProviderManager = new MangaProviderManager(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        mAdapter = new GroupedAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mListModeHelper = new ListModeHelper(this, this);
        mListModeHelper.applyCurrent();
        mListModeHelper.enable();
        List<ProviderSummary> providers = mProviderManager.getOrderedProviders();
        mProgressBar.setMax(providers.size());
        mProgressBar.setProgress(0);
        new SearchTask(LocalMangaProvider.getProviderSummary(this)).executeOnExecutor(mExecutor);
        if (checkConnectionWithDialog(this)) {
            for (ProviderSummary o : providers) {
                new SearchTask(o).executeOnExecutor(mExecutor);
            }
        } else {
            mMessageBlock.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_multiple, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int viewMode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getInt("view_mode", 0);
        onListModeChanged(viewMode != 0, viewMode - 1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_listmode:
                mListModeHelper.showDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListModeChanged(boolean grid, int sizeMode) {
        int spans;
        ThumbSize thumbSize;
        switch (sizeMode) {
            case -1:
                spans = LayoutUtils.isTabletLandscape(this) ? 2 : 1;
                thumbSize = ThumbSize.THUMB_SIZE_LIST;
                break;
            case 0:
                spans = LayoutUtils.getOptimalColumnsCount(getResources(), thumbSize = ThumbSize.THUMB_SIZE_SMALL);
                break;
            case 1:
                spans = LayoutUtils.getOptimalColumnsCount(getResources(), thumbSize = ThumbSize.THUMB_SIZE_MEDIUM);
                break;
            case 2:
                spans = LayoutUtils.getOptimalColumnsCount(getResources(), thumbSize = ThumbSize.THUMB_SIZE_LARGE);
                break;
            default:
                return;
        }

        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstCompletelyVisibleItemPosition();
        layoutManager.setSpanCount(spans);
        layoutManager.setSpanSizeLookup(new AutoSpanSizeLookup(spans));
        mAdapter.setThumbnailsSize(thumbSize);
        if (mAdapter.setGrid(grid)) {
            mRecyclerView.setAdapter(mAdapter);
        }
        mRecyclerView.scrollToPosition(position);
    }

    @Override
    protected void onDestroy() {
        mListModeHelper.disable();
        super.onDestroy();
    }

    @Override
    public void onMoreClick(String title, ProviderSummary provider) {
        startActivity(new Intent(MultipleSearchActivity.this, SearchActivity.class)
                .putExtra("query", mQuery)
                .putExtra("title", title)
                .putExtra("provider", provider.id));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    private class SearchTask extends AsyncTask<Void, Void, MangaList> {
        private final ProviderSummary mProviderSummary;

        private SearchTask(ProviderSummary provider) {
            this.mProviderSummary = provider;
        }

        @Override
        protected MangaList doInBackground(Void... params) {
            try {
                return mProviderManager.instanceProvider(mProviderSummary.aClass).search(mQuery, 0);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(MangaList mangaInfos) {
            super.onPostExecute(mangaInfos);
            if (mangaInfos != null && mangaInfos.size() != 0) {
                mAdapter.append(mProviderSummary, mangaInfos);
            }
            mProgressBar.incrementProgressBy(1);
            if (mProgressBar.getProgress() == mProgressBar.getMax()) {
                mMessageBlock.setVisibility(View.GONE);
                if (mAdapter.getItemCount() == 0) {
                    Snackbar.make(mRecyclerView, R.string.no_manga_found, Snackbar.LENGTH_INDEFINITE)
                            .show();
                }
            }
        }
    }

    private class AutoSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        final int mCount;

        public AutoSpanSizeLookup(int mCount) {
            this.mCount = mCount;
        }

        @Override
        public int getSpanSize(int position) {
            return mAdapter.getItemViewType(position) == 0 ? mCount : 1;
        }
    }
}
