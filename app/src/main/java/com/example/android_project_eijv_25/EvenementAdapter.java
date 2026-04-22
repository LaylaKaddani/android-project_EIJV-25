package com.example.android_project_eijv_25;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class EvenementAdapter extends RecyclerView.Adapter<EvenementAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Evenement evenement);
    }

    private List<Evenement> allEvents = new ArrayList<>();
    private List<Evenement> filteredEvents = new ArrayList<>();
    private final Context context;
    private final OnEventClickListener listener;

    public EvenementAdapter(Context context, OnEventClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setEvents(List<Evenement> events) {
        this.allEvents = new ArrayList<>(events);
        this.filteredEvents = new ArrayList<>(events);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredEvents.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredEvents.addAll(allEvents);
        } else {
            String lower = query.toLowerCase().trim();
            for (Evenement e : allEvents) {
                if (e.getTitre() != null && e.getTitre().toLowerCase().contains(lower)) {
                    filteredEvents.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_evenement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Evenement ev = filteredEvents.get(position);
        holder.tvTitle.setText(ev.getTitre() != null ? ev.getTitre() : "");
        holder.tvAddress.setText(ev.getAdresse() != null ? ev.getAdresse() : "");
        holder.tvDate.setText(ev.getDate_debut() != null ? ev.getDate_debut() : "");

        if (ev.getImage_url() != null && !ev.getImage_url().isEmpty()) {
            Glide.with(context)
                    .load(ev.getImage_url())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> listener.onEventClick(ev));
    }

    @Override
    public int getItemCount() {
        return filteredEvents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvAddress, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivEventImage);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvAddress = itemView.findViewById(R.id.tvEventAddress);
            tvDate = itemView.findViewById(R.id.tvEventDate);
        }
    }
}