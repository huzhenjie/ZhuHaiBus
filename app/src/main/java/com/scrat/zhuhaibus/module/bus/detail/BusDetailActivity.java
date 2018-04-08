package com.scrat.zhuhaibus.module.bus.detail;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import com.scrat.zhuhaibus.R;
import com.scrat.zhuhaibus.data.local.Preferences;
import com.scrat.zhuhaibus.data.modle.BusStation;
import com.scrat.zhuhaibus.data.modle.BusStop;
import com.scrat.zhuhaibus.databinding.ActivityBusDetailBinding;
import com.scrat.zhuhaibus.framework.common.BaseActivity;
import com.scrat.zhuhaibus.framework.common.BaseRecyclerViewAdapter;
import com.scrat.zhuhaibus.framework.common.BaseRecyclerViewHolder;
import com.scrat.zhuhaibus.framework.common.ViewAnnotation;
import com.scrat.zhuhaibus.framework.view.SelectorPopupWindow;
import com.scrat.zhuhaibus.module.feedback.FeedbackActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by scrat on 2018/3/25.
 */
@ViewAnnotation(modelName = "bus_detail")
public class BusDetailActivity extends BaseActivity implements BusDetailContract.BusDetailView {
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String FROM = "from";
    private static final String TO = "to";

    public static void show(
            Context context, String busId, String lineName, String fromStation, String toStation) {
        Intent i = new Intent(context, BusDetailActivity.class);
        i.putExtra(ID, busId);
        i.putExtra(NAME, lineName);
        i.putExtra(FROM, fromStation);
        i.putExtra(TO, toStation);
        context.startActivity(i);
    }

    private BusDetailContract.BusDetailPresenter presenter;
    private ActivityBusDetailBinding binding;
    private SelectorPopupWindow selector;
    private Adapter adapter;
    private boolean autoRefresh;
    private static final int REFRESH_SECOND = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bus_detail);

        adapter = new Adapter();
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setHasFixedSize(true);
        binding.list.setAdapter(adapter);

        String busId = getIntent().getStringExtra(ID);
        String lineName = getIntent().getStringExtra(NAME);
        String fromStation = getIntent().getStringExtra(FROM);
        String toStation = getIntent().getStringExtra(TO);
        new BusDetailPresenter(getApplicationContext(), this, busId, lineName, fromStation, toStation);
        binding.title.setText(String.format("%s 开往 %s", lineName, toStation));

        binding.srl.setOnRefreshListener(() -> {
            presenter.refreshStation();
        });

        selector = new SelectorPopupWindow(this);

        presenter.initBusStop();
        autoRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.autoRefresh = Preferences.getInstance().isAutoRefresh();
        autoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.autoRefresh = false;
    }

    @Override
    protected void onDestroy() {
        if (selector.isShowing()) {
            selector.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void setPresenter(BusDetailContract.BusDetailPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void showLoadingBusStop() {
        binding.srl.setRefreshing(true);
    }

    @Override
    public void showLoadBusStopError(int resId) {
        binding.srl.setRefreshing(false);
        showMessage(resId);
    }

    @Override
    public void showBusStop(List<BusStop> stopList) {
        binding.srl.setRefreshing(false);
        adapter.replaceData(stopList);
    }

    @Override
    public void showRefreshStation() {
        binding.srl.setRefreshing(true);
    }

    @Override
    public void showRefreshStationError(int resId) {
        binding.srl.setRefreshing(false);
        showMessage(resId);
    }

    @Override
    public void refreshStation(List<BusStation> stations) {
        binding.srl.setRefreshing(false);
        adapter.refreshStation(stations);
    }

    public void more(View view) {
        Map<String, View.OnClickListener> items = new TreeMap<>();
        items.put(getString(R.string.feedback), v -> FeedbackActivity.show(this));
        if (Preferences.getInstance().isAutoRefresh()) {
            items.put(getString(R.string.auto_refresh_switch_off), v -> {
                Preferences.getInstance().setAutoRefresh(false);
                autoRefresh = false;
                showMessage(R.string.already_switch_off_auto_refresh);
            });
        } else {
            items.put(getString(R.string.auto_refresh_switch_on), v -> {
                Preferences.getInstance().setAutoRefresh(true);
                autoRefresh = true;
                autoRefresh();
                showMessage(R.string.already_switch_on_auto_refresh);
            });
        }

        selector.refreshItems(items).show(view);
    }

    public void refresh(View view) {
        presenter.initBusStop();
    }

    private void autoRefresh() {
        binding.srl.postDelayed(() -> {
            if (isFinishing()) {
                return;
            }

            if (!autoRefresh) {
                return;
            }

            presenter.refreshStation();
            autoRefresh();
        }, REFRESH_SECOND * 1000L);
    }

    private static class Adapter extends BaseRecyclerViewAdapter<BusStop> {
        private Map<String, Integer> busArrivedCountMap;
        private Map<String, Integer> busLeaveCountMap;

        private Adapter() {
            busArrivedCountMap = new HashMap<>();
            busLeaveCountMap = new HashMap<>();
        }

        private void refreshStation(List<BusStation> stations) {
            busArrivedCountMap.clear();
            busLeaveCountMap.clear();
            for (BusStation station : stations) {
                if (station.isArrival()) {
                    Integer count = busArrivedCountMap.get(station.getCurrentStation());
                    if (count == null) {
                        count = 0;
                    }
                    count++;
                    busArrivedCountMap.put(station.getCurrentStation(), count);
                } else {
                    Integer count = busLeaveCountMap.get(station.getCurrentStation());
                    if (count == null) {
                        count = 0;
                    }
                    count++;
                    busLeaveCountMap.put(station.getCurrentStation(), count);
                }

            }
            notifyDataSetChanged();
        }

        @Override
        protected void onBindItemViewHolder(
                BaseRecyclerViewHolder holder, int position, BusStop busStop) {

            if (busArrivedCountMap.containsKey(busStop.getName())) {
                Integer count = busArrivedCountMap.get(busStop.getName());
                String tip = "";
                if (count != null && count > 1) {
                    tip = "x " + count;
                }
                holder.setVisibility(R.id.bus_stop_icon, View.VISIBLE)
                        .setText(R.id.bus_stop_arrived_tip, tip)
                        .setVisibility(R.id.bus_stop_left_icon, View.INVISIBLE)
                        .setVisibility(R.id.bus_stop_name_arrived, true)
                        .setVisibility(R.id.bus_stop_name, false)
                        .setText(R.id.bus_stop_name_arrived, busStop.getName());
                return;
            }

            String tip = "";
            if (busLeaveCountMap.containsKey(busStop.getName())) {
                Integer count = busLeaveCountMap.get(busStop.getName());
                if (count != null && count > 1) {
                    tip = "x " + count;
                }
                holder.setVisibility(R.id.bus_stop_left_icon, View.VISIBLE);
            } else {
                holder.setVisibility(R.id.bus_stop_left_icon, View.INVISIBLE);
            }

            holder.setVisibility(R.id.bus_stop_icon, View.INVISIBLE)
                    .setText(R.id.bus_stop_leave_tip, tip)
                    .setVisibility(R.id.bus_stop_name_arrived, false)
                    .setVisibility(R.id.bus_stop_name, true)
                    .setText(R.id.bus_stop_name, busStop.getName());
        }

        @Override
        protected BaseRecyclerViewHolder onCreateRecycleItemView(ViewGroup parent, int viewType) {
            return BaseRecyclerViewHolder.newInstance(parent, R.layout.item_list_bus_detail);
        }
    }
}
