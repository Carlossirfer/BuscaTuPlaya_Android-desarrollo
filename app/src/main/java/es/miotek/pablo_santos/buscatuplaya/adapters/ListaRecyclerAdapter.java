package es.miotek.pablo_santos.buscatuplaya.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.List;

import es.miotek.pablo_santos.buscatuplaya.R;

/**
 * Created by Pablo Santos on 12/06/2017.
 */
public class ListaRecyclerAdapter extends RecyclerView.Adapter<ListaRecyclerAdapter.ViewHolder> {

	private List<ParseObject> lista;
	private int layout;
	private OnItemClickListener itemClickListener;

	public static class ViewHolder extends RecyclerView.ViewHolder {
		// Elementos UI a rellenar.
		public TextView tvNombre;
		public TextView tvComunidad;
		public TextView tvProvincia;
		public ImageView ivBandera;

		public ViewHolder(View itemView) {
			// Recibe la View completa. La pasa al constructor padre y enlazamos referencias UI
			// con nuestras propiedades ViewHolder declaradas justo arriba.
			super(itemView);
			tvNombre = (TextView) itemView.findViewById(R.id.tvNombre);
			tvComunidad = (TextView) itemView.findViewById(R.id.tvComunidad);
			tvProvincia = (TextView) itemView.findViewById(R.id.tvProvincia);
			ivBandera = (ImageView) itemView.findViewById(R.id.ivBandera);
		}

		public void bind(final ParseObject objeto, final OnItemClickListener listener) {
			// Procesamos los datos a renderizar.
			tvNombre.setText(objeto.getString("Nombre"));
			tvComunidad.setText(objeto.getString("Comunidad_Autonoma"));
			tvProvincia.setText(objeto.getString("Provincia"));
			// Definimos que por cada elemento de nuestro recycler view, tenemos un click listener
			// que se comporta de la siguiente manera...
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// ... pasamos nuestro objeto modelo y su posición.
					listener.onItemClick(objeto, getAdapterPosition());
				}
			});
		}
	}

	public ListaRecyclerAdapter(List<ParseObject> lista, int layout, OnItemClickListener
		  listener) {
		this.lista = lista;
		this.layout = layout;
		this.itemClickListener = listener;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		// Inflamos el layout y se lo pasamos al constructor del ViewHolder, donde manejaremos
		// toda la lógica como extraer los datos, referencias...
		View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		// Llamamos al método Bind del ViewHolder pasándole objeto y listener
		holder.bind(lista.get(position), itemClickListener);
	}

	@Override
	public int getItemCount() {
		return lista.size();
	}

	// Declaramos nuestra interfaz con el/los método/s a implementar
	public interface OnItemClickListener {
		void onItemClick(ParseObject object, int position);
	}

	// Cambia la lista que el adaptador usa y le notifica que ha cambiado.
	public void setList(List<ParseObject> lista) {
		this.lista = lista;
		notifyDataSetChanged();
	}

}
