package rs.ruffle;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;

import java.util.ArrayList;

public class OverlayMenuAdapter implements ListAdapter {
    Context context;
    ArrayList<String> detailItems;
    private ArrayList<OverlayMenuListener> buttonClickListeners = new ArrayList<>();

    public OverlayMenuAdapter(Context _context, ArrayList<String> _detailItems) {
        context = _context;
        detailItems = _detailItems;
    }

    public void AddListener(OverlayMenuListener listener) {
        buttonClickListeners.add(listener);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        String detailItem = detailItems.get(position);
        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(R.layout.menu_row_item, null);

            Button button = view.findViewById(R.id.menu_button);
            button.setText(detailItem);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Log.d("Adapter", "Pressed " + detailItem + " in position " + position);
                    for (OverlayMenuListener ovl : buttonClickListeners)
                        ovl.onButtonPress(position);
                }
            });
        }

        return view;
    }
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    @Override
    public boolean isEnabled(int position) {
        return true;
    }
    @Override
    public void registerDataSetObserver(DataSetObserver observer) { }
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) { }
    @Override
    public int getCount() {
        return detailItems.size();
    }
    @Override
    public Object getItem(int position) {
        return detailItems.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public boolean hasStableIds() {
        return false;
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getViewTypeCount() {
        return detailItems.size();
    }
    @Override
    public boolean isEmpty() {
        return detailItems.size() <= 0;
    }
}
