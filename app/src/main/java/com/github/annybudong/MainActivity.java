package com.github.annybudong;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.annybudong.slipview.SlipView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getName();

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        //分割线
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.color.color_DDDDDD));
        recyclerView.addItemDecoration(divider);

//        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new MyAdapter(this, mockData()));
    }

    private List<String> mockData() {
        List<String> data = new ArrayList<>();
        String temp = " item";
        for(int i = 0; i < 5; i++) {
            data.add(i + temp);
        }

        return data;
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> implements SlipView.OnScrollListener {

        private Context ctx;
        private List<String> data;

        public MyAdapter(Context ctx, List<String> data) {
            this.ctx = ctx;
            this.data = data;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            SlipView v = (SlipView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new ViewHolder(v);
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            // 绑定数据
            holder.contentTv.setText(data.get(position));
            holder.contentTv.setTag(position);
            holder.rooView.closeMenu(0);
            holder.rooView.setOnScrollListener(this);
            holder.deleteMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount() - position);
                    Toast.makeText(ctx, "click delete.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public void onScrollStart() {
            Log.d(TAG, "onScrollStart");
        }

        @Override
        public void onScrollEnd() {
            Log.e(TAG, "onScrollEnd");
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private boolean scrollable = true;
            SlipView rooView;
            TextView contentTv;
            TextView deleteMenu;
            TextView editMenu;

            public ViewHolder(final View itemView) {
                super(itemView);
                rooView = (SlipView) itemView;
                rooView.setTouchSlop(4);
                contentTv = (TextView) itemView.findViewById(R.id.content);
                deleteMenu = (TextView) itemView.findViewById(R.id.menu_delete);
                editMenu = (TextView) itemView.findViewById(R.id.menu_edit);
//                contentTv.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        scrollable = !scrollable;
//                        rooView.enableScroll(scrollable);
//                        Toast.makeText(ctx, "允许侧滑:" + scrollable, Toast.LENGTH_SHORT).show();
//                    }
//                });
                editMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rooView.closeMenu(200);
                    }
                });
            }
        }
    }
}
