package es.miotek.pablo_santos.buscatuplaya.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import es.miotek.pablo_santos.buscatuplaya.R;
import es.miotek.pablo_santos.buscatuplaya.adapters.ListaRecyclerAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class Fragment_lista extends Fragment {

	private View viewRoot;
	private Context context;
	private RecyclerView recyclerView;

	public Fragment_lista() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
		  savedInstanceState) {
		// Inflate the layout for this fragment
		viewRoot = inflater.inflate(R.layout.fragment_lista, container, false);
		return viewRoot;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Carga elementos de la UI.
		context = getContext();

		// TODO — Tratar de encapsular las peticiones en ParseOperations.
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Playas");
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> response, ParseException e) {
				if (e == null) iniciarRecycler(response);
				else Log.d("PARSE", "Error: " + e.getMessage());
			}
		});
	}

	public void iniciarRecycler(List<ParseObject> lista) {
		recyclerView = (RecyclerView) viewRoot.findViewById(R.id.rvLista);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(
			  new ListaRecyclerAdapter(lista, R.layout.recycler_lista_item,
					new ListaRecyclerAdapter.OnItemClickListener() {
						@Override
						public void onItemClick(ParseObject object, int position) {
							// ¿Se necesita hacer algo...?
						}
					}));
	}

}
