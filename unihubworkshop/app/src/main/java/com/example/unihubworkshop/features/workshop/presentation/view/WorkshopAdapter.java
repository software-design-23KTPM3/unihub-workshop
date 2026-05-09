package com.example.unihubworkshop.features.workshop.presentation.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import java.util.ArrayList;
import java.util.List;

public class WorkshopAdapter extends RecyclerView.Adapter<WorkshopAdapter.ViewHolder> {
    private List<WorkShop> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WorkShop workshop);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public WorkshopAdapter(List<WorkShop> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    public void setWorkshops(List<WorkShop> workshops) {
        this.list = workshops;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workshop, parent, false);
       return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WorkShop item = list.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvAuthor.setText("By " + item.getAuthor());
        holder.tvDate.setText(item.getDate());
        holder.tvTime.setText(item.getTime());
        holder.tvAddress.setText(item.getAddress());
        holder.tvSlots.setText(String.format("%d/%d seats", item.getAttendanceCount(), item.getMaxAttendance()));
        
        if (item.isFree()) {
            holder.tvPrice.setText("FREE");
            holder.tvPrice.setTextColor(Color.parseColor("#00C853"));
            holder.tvPrice.setBackgroundResource(R.drawable.bg_price_free);
        } else {
            holder.tvPrice.setText(String.format("%,d VND", item.getPrice()));
            holder.tvPrice.setTextColor(Color.parseColor("#5648E3"));
            holder.tvPrice.setBackgroundResource(R.drawable.bg_price);
        }

        if (item.isRegistered()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvDate, tvTime, tvAddress, tvPrice, tvSlots, tvStatus;
        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSlots = itemView.findViewById(R.id.tvSlots);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}