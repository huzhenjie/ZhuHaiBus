package com.scrat.zhuhaibus.module.bus.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scrat.zhuhaibus.R;
import com.scrat.zhuhaibus.databinding.FragmentMainBinding;
import com.scrat.zhuhaibus.framework.common.BaseFragment;
import com.scrat.zhuhaibus.framework.common.BaseOnItemClickListener;
import com.scrat.zhuhaibus.framework.common.BaseRecyclerViewAdapter;
import com.scrat.zhuhaibus.framework.common.BaseRecyclerViewHolder;
import com.scrat.zhuhaibus.framework.common.ViewAnnotation;
import com.scrat.zhuhaibus.data.modle.BusLine;
import com.scrat.zhuhaibus.framework.view.IosDialog;
import com.scrat.zhuhaibus.framework.view.SelectorPopupWindow;
import com.scrat.zhuhaibus.module.bus.detail.BusDetailActivity;
import com.scrat.zhuhaibus.module.bus.search.SearchActivity;
import com.scrat.zhuhaibus.module.feedback.FeedbackActivity;

import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Created by scrat on 2018/3/24.
 */
@ViewAnnotation(modelName = "busList")
public class BusListFragment extends BaseFragment implements BusListContract.View {

    private static final int REQUEST_CODE_SEARCH = 11;
    public static BusListFragment newInstance() {
        return new BusListFragment();
    }

    private FragmentMainBinding binding;
    private BusListContract.Presenter presenter;
    private Adapter adapter;
    private IosDialog dialog;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        binding.searchBar.setOnClickListener(v -> {
            SearchActivity.show(getActivity(), REQUEST_CODE_SEARCH);
        });

        dialog = new IosDialog(getContext())
                .setTitle(getString(R.string.delete_history))
                .setNegative(getString(R.string.cancel));

        adapter = new Adapter(new OnItemClickListener() {
            @Override
            public void onItemClick(BusLine line) {
                presenter.updateHistoryPos(line.getId());
                BusDetailActivity.show(
                        getContext(),
                        line.getId(),
                        line.getName(),
                        line.getFromStation(),
                        line.getToStation());
            }

            @Override
            public void onItemDelete(BusLine line) {
                dialog.setContent(String.format("确定删除 \"%s 开往 %s\" ？", line.getName(), line.getToStation()))
                        .setPositive(getString(R.string.confirm), v -> presenter.deleteHistory(line.getId()));
                dialog.show(binding.list);
            }
        });
        binding.list.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.list.setHasFixedSize(true);
        binding.list.setAdapter(adapter);

        new BusListPresenter(getApplicationContext(), this);
        presenter.loadHistory();
        return binding.getRoot();
    }

    public void refreshHistory() {
        if (presenter == null) {
            return;
        }
        presenter.loadHistory();
    }

    @Override
    public void onDestroyView() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_SEARCH:
                presenter.loadHistory();
                BusLine line = SearchActivity.parseData(data);
                BusDetailActivity.show(
                        getContext(),
                        line.getId(),
                        line.getName(),
                        line.getFromStation(),
                        line.getToStation());
                break;
            default:
                break;
        }
    }

    @Override
    public void setPresenter(BusListContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void showHistoryEmpty() {
        adapter.clearData();
        binding.emptyHistoryView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showHistory(List<BusLine> list) {
        binding.emptyHistoryView.setVisibility(View.GONE);
        adapter.replaceData(list);
    }

    private interface OnItemClickListener {
        void onItemClick(BusLine line);

        void onItemDelete(BusLine line);
    }

    private static class Adapter extends BaseRecyclerViewAdapter<BusLine> {
        private OnItemClickListener listener;

        private Adapter(OnItemClickListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onBindItemViewHolder(
                BaseRecyclerViewHolder holder, int position, BusLine line) {
            String title = String.format("%s  开往  %s", line.getName(), line.getToStation());
            holder.setText(R.id.content, title)
                    .setOnClickListener(v -> listener.onItemClick(line))
                    .setOnClickListener(R.id.delete, v -> listener.onItemDelete(line));
        }

        @Override
        protected BaseRecyclerViewHolder onCreateRecycleItemView(ViewGroup parent, int viewType) {
            return BaseRecyclerViewHolder.newInstance(parent, R.layout.item_list_history);
        }
    }
}
