package org.nv95.openmanga.activities;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import org.nv95.openmanga.R;
import org.nv95.openmanga.adapters.ProvidersAdapter;
import org.nv95.openmanga.components.DividerItemDecoration;
import org.nv95.openmanga.providers.staff.MangaProviderManager;
import org.nv95.openmanga.providers.staff.ProviderSummary;

import java.util.Collections;
import java.util.List;

/**
 * Selecting used manga providers
 * Created by nv95 on 14.10.15.
 */
public class ProviderSelectActivity extends BaseAppActivity implements ProvidersAdapter.OnStartDragListener {

    private List<ProviderSummary> mProviders;
    private MangaProviderManager mProviderManager;
    private ProvidersAdapter mAdapter;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provselect);
        setSupportActionBar(R.id.toolbar);
        enableHomeAsUp();

        mProviderManager = new MangaProviderManager(this);
        mProviders = mProviderManager.getOrderedProviders();


        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        assert recyclerView != null;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter = new ProvidersAdapter(this, mProviders, this));
        mAdapter.setActiveCount(mProviderManager.getProvidersCount());
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        mItemTouchHelper = new ItemTouchHelper(new OrderManager());
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private class OrderManager extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            if (toPosition == mProviders.size() + 1 || toPosition == mAdapter.getActiveCount()) { //drop to footer/divider
                return false;
            }

            if (viewHolder instanceof ProvidersAdapter.DividerHolder) { //divider
                if (toPosition == 0) {
                    return false; //enabled providers count must be > 0
                }
                mAdapter.setActiveCount(toPosition);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                mProviderManager.setProvidersCount(toPosition);
                return true;
            }

            if (fromPosition > mAdapter.getActiveCount() && toPosition < mAdapter.getActiveCount()) {
                return false;
            }

            if (fromPosition < mAdapter.getActiveCount() && toPosition > mAdapter.getActiveCount()) {
                return false;
            }

            if (fromPosition > mAdapter.getActiveCount()) {
                fromPosition--;
            }

            if (toPosition > mAdapter.getActiveCount()) {
                toPosition--;
            }

            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(mProviders, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(mProviders, i, i - 1);
                }
            }

            mAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            mProviderManager.updateOrder(mProviders);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }
    }
}
