package com.annybudong;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annybudong.slipview.SlipView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new MyAdapter(mockData()));
    }

    private List<String> mockData() {
        List<String> data = new ArrayList<>();
        String temp = " item";
        for(int i = 0; i < 50; i++) {
            data.add(i + temp);
        }

        return data;
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

        private List<String> mData;

        public MyAdapter(List<String> data) {
            this.mData = data;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new ViewHolder(v);
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // 绑定数据
            holder.contentTv.setText(mData.get(position));
            holder.contentTv.setTag(position);
            holder.rooView.closeMenu();
        }

        @Override
        public int getItemCount() {
            return mData == null ? 0 : mData.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            SlipView rooView;
            TextView contentTv;
            TextView deleteMenu;
            TextView editMenu;

            public ViewHolder(final View itemView) {
                super(itemView);
                rooView = (SlipView) itemView;
                contentTv = (TextView) itemView.findViewById(R.id.content);
                deleteMenu = (TextView) itemView.findViewById(R.id.menu_delete);
                editMenu = (TextView) itemView.findViewById(R.id.menu_edit);
            }
        }
    }
}
